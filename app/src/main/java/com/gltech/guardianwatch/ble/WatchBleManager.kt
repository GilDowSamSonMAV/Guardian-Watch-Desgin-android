package com.gltech.guardianwatch.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.util.Log
import com.gltech.guardianwatch.casualty.HealthTone
import com.gltech.guardianwatch.casualty.VitalsFrame
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.ktx.stateAsFlow
import no.nordicsemi.android.ble.observer.ConnectionObserver

/**
 * Manages a single Guardian Watch BLE connection.
 *
 * One instance per watch. Lifetime is tied to the foreground service.
 * Never call from Main thread if possible — BleManager posts back to it.
 *
 * Key design choices:
 * - Retries 133 errors 3×. Android BT stack is flaky; this is standard.
 * - auto-reconnect: true. In combat, connections drop from movement + shielding;
 *   we want the stack to rebuild as soon as range is re-established.
 * - CCCDs are enabled in onServicesInvalidated()'s companion [initialize()] — this is
 *   the Nordic pattern for subscribing to notifications reliably across disconnects.
 */
class WatchBleManager(context: Context) : BleManager(context) {

    companion object {
        private const val TAG = "WatchBleManager"
    }

    private var telemetryChar: android.bluetooth.BluetoothGattCharacteristic? = null
    private var hrChar: android.bluetooth.BluetoothGattCharacteristic? = null
    private var batteryChar: android.bluetooth.BluetoothGattCharacteristic? = null

    private val _vitals = MutableSharedFlow<VitalsFrame>(
        replay = 1,
        extraBufferCapacity = 64,
    )
    val vitals: SharedFlow<VitalsFrame> = _vitals.asSharedFlow()

    private var currentRssi: Int = -127
    private var currentBattery: Int = 100

    init {
        setConnectionObserver(object : ConnectionObserver {
            override fun onDeviceConnecting(device: BluetoothDevice) {
                Log.i(TAG, "Connecting to ${device.address}")
            }
            override fun onDeviceConnected(device: BluetoothDevice) {
                Log.i(TAG, "Connected to ${device.address}")
            }
            override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
                Log.w(TAG, "Failed to connect to ${device.address}: reason=$reason")
            }
            override fun onDeviceReady(device: BluetoothDevice) {
                Log.i(TAG, "Device ready: ${device.address}")
                // Poll RSSI every 2s for link-quality indicator.
                readRssi().with { _, rssi -> currentRssi = rssi }.enqueue()
            }
            override fun onDeviceDisconnecting(device: BluetoothDevice) {
                Log.i(TAG, "Disconnecting: ${device.address}")
            }
            override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
                Log.w(TAG, "Disconnected from ${device.address}: reason=$reason")
            }
        })
    }

    override fun getMinLogPriority(): Int = Log.INFO

    override fun log(priority: Int, message: String) {
        Log.println(priority, TAG, message)
    }

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        // Guardian service is required; HR + battery are optional fallbacks.
        val gwService = gatt.getService(GuardianWatchProfile.GW_SERVICE)
        if (gwService != null) {
            telemetryChar = gwService.getCharacteristic(GuardianWatchProfile.GW_TELEMETRY)
        }
        val hrService = gatt.getService(GuardianWatchProfile.HEART_RATE_SERVICE)
        hrChar = hrService?.getCharacteristic(GuardianWatchProfile.HEART_RATE_MEASUREMENT)
        val battService = gatt.getService(GuardianWatchProfile.BATTERY_SERVICE)
        batteryChar = battService?.getCharacteristic(GuardianWatchProfile.BATTERY_LEVEL)

        // Require at least one telemetry channel.
        return telemetryChar != null || hrChar != null
    }

    override fun initialize() {
        // Request high-throughput MTU (ATT_MTU=517 is the spec max).
        requestMtu(517)
            .fail { _, status -> Log.w(TAG, "MTU request failed: $status") }
            .enqueue()

        // Request high connection priority for low-latency vitals streaming.
        requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH).enqueue()

        // Subscribe to Guardian telemetry (primary channel).
        telemetryChar?.let { ch ->
            setNotificationCallback(ch).with { device, data ->
                onTelemetryReceived(data)
            }
            enableNotifications(ch)
                .fail { _, status -> Log.w(TAG, "GW telemetry notify enable failed: $status") }
                .enqueue()
        }

        // Fallback: standard HR service for Instinct 2 with no Guardian app loaded.
        hrChar?.let { ch ->
            setNotificationCallback(ch).with { _, data ->
                onHeartRateReceived(data)
            }
            enableNotifications(ch).enqueue()
        }

        // Battery poll every 30s.
        batteryChar?.let { ch ->
            setNotificationCallback(ch).with { _, data ->
                data.getIntValue(Data.FORMAT_UINT8, 0)?.let { currentBattery = it }
            }
            enableNotifications(ch).enqueue()
        }
    }

    override fun onServicesInvalidated() {
        telemetryChar = null
        hrChar = null
        batteryChar = null
    }

    // ---------- Data parsers ----------

    private fun onTelemetryReceived(data: Data) {
        val bytes = data.value ?: return
        val tm = GuardianWatchProfile.parseTelemetry(bytes) ?: return
        currentBattery = tm.batteryPct
        val frame = VitalsFrame(
            timestampMs = tm.tsSec * 1000L,
            hrBpm = tm.hrBpm,
            spo2Pct = null,           // Instinct 2 has no SpO2 sensor — per hardware constraint
            skinTempC = null,         // likewise
            accelMagG = tm.accelMagG,
            batteryPct = currentBattery,
            rssiDbm = currentRssi,
        )
        _vitals.tryEmit(frame)
    }

    /** Standard HR Measurement parser (spec 3.106 of Bluetooth SIG). */
    private fun onHeartRateReceived(data: Data) {
        val bytes = data.value ?: return
        if (bytes.isEmpty()) return
        val flags = bytes[0].toInt() and 0xFF
        val hrUint16 = (flags and 0x01) != 0
        val hr: Int = if (hrUint16 && bytes.size >= 3) {
            (bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)
        } else if (bytes.size >= 2) {
            bytes[1].toInt() and 0xFF
        } else return

        val frame = VitalsFrame(
            timestampMs = System.currentTimeMillis(),
            hrBpm = hr,
            spo2Pct = null,
            skinTempC = null,
            accelMagG = null,
            batteryPct = currentBattery,
            rssiDbm = currentRssi,
        )
        _vitals.tryEmit(frame)
    }

    /** Public connection entry point with standard retry+timeout profile. */
    fun connectWithProfile(device: BluetoothDevice) {
        connect(device)
            .useAutoConnect(true)
            .retry(3, 100)
            .timeout(15_000)
            .enqueue()
    }

    /** Expose connection state as a Flow (from ble-ktx). */
    val stateFlow get() = stateAsFlow()
}
