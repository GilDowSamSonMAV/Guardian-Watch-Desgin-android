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
import com.gltech.guardianwatch.ble.VitalsRepository
import com.gltech.guardianwatch.casualty.Casualty
import com.gltech.guardianwatch.casualty.Triage
import com.gltech.guardianwatch.kiosk.KioskController
import com.gltech.guardianwatch.mode.AppMode
import com.gltech.guardianwatch.mode.ModeController
import com.gltech.guardianwatch.ui.screens.*
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

    private var bleService: BleService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            bleService = (binder as? BleService.LocalBinder)?.service()
            maybeSeedDemoCasualty()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            bleService = null
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

    /** Demo seed for MVP: register a placeholder casualty so the UI is non-empty at launch. */
    private fun maybeSeedDemoCasualty() {
        lifecycleScope.launch {
            if (vitalsRepository.streams.value.isEmpty()) {
                val demo = Casualty(
                    id = "CAS-0147",
                    name = "Weiss, J.",
                    age = 26,
                    mgrs = "33R WN 72314 28005",
                    triage = Triage.IMMEDIATE,
                    watchDeviceAddress = "00:00:00:00:00:00",  // TODO replace with actual Instinct 2 MAC
                    openedAtMs = System.currentTimeMillis(),
                )
                vitalsRepository.registerCasualty(demo)
            }
        }
    }
}

@Composable
private fun App(
    modeController: ModeController,
    vitalsRepository: VitalsRepository,
) {
    val currentMode by modeController.currentMode.collectAsState(initial = null)
    val streams by vitalsRepository.streams.collectAsState()
    val scope = rememberCoroutineScope()

    when (currentMode) {
        null -> ModeSelectorScreen(onModeSelected = { picked ->
            scope.launch { modeController.setMode(picked) }
        })
        AppMode.MEDIC_DASHBOARD -> MedicDashboardScreen(
            streams = streams,
            selfId = SELF_ID,
            onHandoffConfirmed = { casualtyId, event ->
                vitalsRepository.appendAudit(casualtyId, event, SELF_ID)
            },
        )
        AppMode.SINGLE_PAIRED -> SinglePairedScreen(stream = streams.values.firstOrNull())
        AppMode.RELAY -> RelayScreen(streams = streams, upstreamConnected = false)
    }
}
