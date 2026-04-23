package com.gltech.guardianwatch.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Scan result surfaced to the UI layer. Decoupled from the Android SDK's
 * [ScanResult] so Composables don't take a platform dependency.
 */
data class ScannedDevice(
    val name: String?,
    val mac: String,
    val rssiDbm: Int,
    /** True when the device advertises the Guardian custom service UUID
     *  (i.e. the Connect IQ watchapp is loaded). False means only the
     *  standard HR service was seen — we can still connect, but the
     *  movementClass channel will be absent. */
    val matchedGuardianService: Boolean,
)

/** Result wrapper for a scan stream — either a running list of discoveries
 *  or an error that prevents scanning (permissions denied, BT off, etc.). */
sealed interface ScanState {
    data class Discoveries(val devices: List<ScannedDevice>) : ScanState
    data class Error(val reason: Reason) : ScanState

    enum class Reason {
        BLUETOOTH_UNAVAILABLE,
        BLUETOOTH_OFF,
        PERMISSION_DENIED,
        SCAN_FAILED,
    }
}

/**
 * BLE scanner for Guardian Watch devices.
 *
 * Filters on the Guardian custom service UUID (`f1ac0000-...`) and the
 * standard Heart Rate service UUID (`0x180D`) — the two paths [WatchBleManager]
 * knows how to talk to. Also opportunistically matches devices whose name
 * starts with `Instinct` (Garmin's naming) so a watch that's still booting
 * its Connect IQ app and hasn't started advertising the custom service yet
 * still shows up.
 *
 * Consumers collect [scan] as a cold [Flow]. Cancelling the coroutine stops
 * the scan. A `durationMs` timeout auto-cancels after that long to save
 * battery.
 */
class BleScanner(private val context: Context) {

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    /**
     * Returns a cold Flow that emits [ScanState.Discoveries] with the current
     * running list of discovered devices on every new result, then completes
     * after [durationMs] milliseconds (or when the consumer cancels).
     *
     * Emits a single [ScanState.Error] and completes if preconditions fail.
     */
    @SuppressLint("MissingPermission")   // checked explicitly below
    fun scan(durationMs: Long = DEFAULT_SCAN_DURATION_MS): Flow<ScanState> {
        val bt = adapter ?: return flow { emit(ScanState.Error(ScanState.Reason.BLUETOOTH_UNAVAILABLE)) }
        if (!bt.isEnabled) return flow { emit(ScanState.Error(ScanState.Reason.BLUETOOTH_OFF)) }
        if (!hasScanPermission()) return flow { emit(ScanState.Error(ScanState.Reason.PERMISSION_DENIED)) }

        val scanner = bt.bluetoothLeScanner
            ?: return flow { emit(ScanState.Error(ScanState.Reason.BLUETOOTH_UNAVAILABLE)) }

        return callbackFlow {
            val found = LinkedHashMap<String, ScannedDevice>()   // MAC -> ScannedDevice, preserves order

            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val mac = result.device.address ?: return
                    val record = result.scanRecord
                    val name = record?.deviceName ?: try {
                        result.device.name
                    } catch (_: SecurityException) {
                        null
                    }
                    val advertisedUuids = record?.serviceUuids?.map { it.uuid } ?: emptyList()
                    val matchedGuardian = advertisedUuids.contains(GuardianWatchProfile.GW_SERVICE)
                    val matchedHr = advertisedUuids.contains(GuardianWatchProfile.HEART_RATE_SERVICE)
                    val matchedName = name?.startsWith("Instinct", ignoreCase = true) == true

                    if (!matchedGuardian && !matchedHr && !matchedName) return

                    val scanned = ScannedDevice(
                        name = name,
                        mac = mac,
                        rssiDbm = result.rssi,
                        matchedGuardianService = matchedGuardian,
                    )
                    val prior = found[mac]
                    // Upgrade the entry if we now see the Guardian UUID where we didn't before,
                    // or refresh RSSI on each sighting.
                    if (prior == null ||
                        (!prior.matchedGuardianService && scanned.matchedGuardianService) ||
                        prior.rssiDbm != scanned.rssiDbm ||
                        prior.name != scanned.name
                    ) {
                        found[mac] = scanned.copy(
                            matchedGuardianService = prior?.matchedGuardianService == true || scanned.matchedGuardianService,
                        )
                        trySend(ScanState.Discoveries(found.values.toList()))
                    }
                }

                override fun onBatchScanResults(results: MutableList<ScanResult>) {
                    results.forEach { onScanResult(ScanCallback.SCAN_FAILED_ALREADY_STARTED, it) }
                }

                override fun onScanFailed(errorCode: Int) {
                    trySend(ScanState.Error(ScanState.Reason.SCAN_FAILED))
                    close()
                }
            }

            // An empty ScanFilter matches all devices — we apply the actual
            // allow-list (Guardian UUID / HR UUID / name starts with "Instinct")
            // inside [ScanCallback.onScanResult] above. Doing it there rather
            // than in native filters lets us also catch devices that aren't
            // yet advertising the Guardian UUID (e.g. Connect IQ app still
            // booting) by matching on device name.
            val filters = listOf(ScanFilter.Builder().build())
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            try {
                scanner.startScan(filters, settings, callback)
            } catch (e: SecurityException) {
                trySend(ScanState.Error(ScanState.Reason.PERMISSION_DENIED))
                close(e)
                return@callbackFlow
            }

            // Emit an empty discoveries tick immediately so the UI can show "scanning…"
            trySend(ScanState.Discoveries(emptyList()))

            // Auto-stop after timeout. Using a launched coroutine via the producer scope.
            val timeoutJob = launch {
                delay(durationMs)
                if (isActive) close()
            }

            awaitClose {
                timeoutJob.cancel()
                try {
                    scanner.stopScan(callback)
                } catch (_: SecurityException) {
                    // perms revoked mid-scan — nothing we can do
                } catch (_: IllegalStateException) {
                    // BT turned off mid-scan — already-stopped is fine
                }
            }
        }.flowOn(Dispatchers.Default)
    }

    private fun hasScanPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val DEFAULT_SCAN_DURATION_MS: Long = 10_000L
    }
}
