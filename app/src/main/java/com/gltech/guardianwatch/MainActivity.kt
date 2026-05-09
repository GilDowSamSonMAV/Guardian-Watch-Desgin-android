package com.gltech.guardianwatch

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.*
import com.gltech.guardianwatch.ble.BleService
import com.gltech.guardianwatch.ble.ScannedDevice
import com.gltech.guardianwatch.ble.VitalsRepository
import com.gltech.guardianwatch.casualty.Casualty
import com.gltech.guardianwatch.casualty.Triage
import com.gltech.guardianwatch.kiosk.KioskController
import com.gltech.guardianwatch.mode.AppMode
import com.gltech.guardianwatch.mode.ModeController
import com.gltech.guardianwatch.ui.components.PairingDialog
import com.gltech.guardianwatch.ui.screens.*
import com.gltech.guardianwatch.ui.screens.TacticalDashboard
import com.gltech.guardianwatch.ui.theme.GuardianWatchTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Identity of the medic running this tablet. Stamped onto every audit entry.
 *  TODO: source from a login screen / device profile once auth is wired. */
private const val SELF_ID = "M.ROSEN"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var vitalsRepository: VitalsRepository

    private lateinit var modeController: ModeController
    private lateinit var kioskController: KioskController

    private val bleServiceState = mutableStateOf<BleService?>(null)
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            bleServiceState.value = (binder as? BleService.LocalBinder)?.service()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            bleServiceState.value = null
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handled via state below — we just re-evaluate */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immersive, edge-to-edge — tablet is fullscreen kiosk.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        modeController = ModeController(this)
        kioskController = KioskController(this)

        // Force Mode Selector screen every launch during testing
        lifecycleScope.launch {
            modeController.clear()
        }

        // Request permissions on first boot — required before BLE service starts.
        requestRuntimePermissions()

        // Bind foreground service (starts it too if not running).
        val svcIntent = Intent(this, BleService::class.java)
        ContextCompat.startForegroundService(this, svcIntent)
        bindService(svcIntent, serviceConnection, BIND_AUTO_CREATE)

        // Try to enter kiosk mode (safe no-op if not device owner).
        kioskController.startLockTask(this)

        setContent {
            GuardianWatchTheme {
                App(
                    modeController = modeController,
                    vitalsRepository = vitalsRepository,
                    bleServiceState = bleServiceState,
                )
            }
        }
    }

    override fun onDestroy() {
        try { unbindService(serviceConnection) } catch (_: Throwable) {}
        super.onDestroy()
    }

    private fun requestRuntimePermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms += Manifest.permission.BLUETOOTH_SCAN
            perms += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            perms += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }


}

@Composable
private fun App(
    modeController: ModeController,
    vitalsRepository: VitalsRepository,
    bleServiceState: State<BleService?>,
) {
    val currentMode by modeController.currentMode.collectAsState(initial = null)
    val streams by vitalsRepository.streams.collectAsState()
    val bleService by bleServiceState
    val scope = rememberCoroutineScope()
    var showPairingDialog by remember { mutableStateOf(false) }

    when (currentMode) {
        null -> ModeSelectorScreen(
            onModeSelected = { picked -> scope.launch { modeController.setMode(picked) } },
            onPairWatchRequested = bleService?.let { { showPairingDialog = true } },
        )
        AppMode.MEDIC_DASHBOARD -> TacticalDashboard(
            streams = streams,
            onBack = { scope.launch { modeController.clear() } },
            onPairWatch = bleService?.let { { showPairingDialog = true } },
        )
        AppMode.SINGLE_PAIRED -> SinglePairedScreen(
            stream = streams.values.firstOrNull(),
            onBack = { scope.launch { modeController.clear() } },
            onPairWatch = bleService?.let { { showPairingDialog = true } },
        )
        AppMode.RELAY -> RelayScreen(
            streams = streams,
            upstreamConnected = false,
            onBack = { scope.launch { modeController.clear() } },
        )
    }

    val svc = bleService
    if (showPairingDialog && svc != null) {
        PairingDialog(
            scanner = svc.scanner,
            onDeviceSelected = { scanned ->
                val casualty = buildCasualtyFromScan(
                    scanned = scanned,
                    existingIds = streams.keys,
                )
                svc.pairAndConnect(casualty)
                showPairingDialog = false
            },
            onDismiss = { showPairingDialog = false },
        )
    }
}

/** Build a placeholder Casualty from a scanned device. The medic can edit
 *  name / age / MGRS / triage later (future feature). Auto-picks the next
 *  unused CAS-NNNN ID. */
private fun buildCasualtyFromScan(
    scanned: ScannedDevice,
    existingIds: Collection<String>,
): Casualty {
    val nextIndex = existingIds
        .mapNotNull { it.removePrefix("CAS-").toIntOrNull() }
        .maxOrNull()?.plus(1) ?: 1
    return Casualty(
        id = "CAS-" + nextIndex.toString().padStart(4, '0'),
        name = scanned.name ?: "Unknown",
        age = 0,
        mgrs = "--",
        triage = Triage.IMMEDIATE,
        watchDeviceAddress = scanned.mac,
        openedAtMs = System.currentTimeMillis(),
    )
}
