package com.gltech.guardianwatch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gltech.guardianwatch.ble.BleScanner
import com.gltech.guardianwatch.ble.ScanState
import com.gltech.guardianwatch.ble.ScannedDevice
import com.gltech.guardianwatch.ui.theme.GwColors
import com.gltech.guardianwatch.ui.theme.GwRadii
import com.gltech.guardianwatch.ui.theme.GwTypography

/**
 * Modal dialog that scans for nearby Guardian-compatible BLE devices and
 * lets the user pick one to pair.
 *
 * Auto-starts a scan on first composition. Scan cancels automatically when
 * the dialog leaves composition (`LaunchedEffect` teardown) or when the
 * [BleScanner] hits its duration timeout.
 *
 * The caller is responsible for turning the selected [ScannedDevice] into a
 * [com.gltech.guardianwatch.casualty.Casualty] and feeding it to
 * [com.gltech.guardianwatch.ble.BleService.pairAndConnect].
 */
@Composable
fun PairingDialog(
    scanner: BleScanner,
    onDeviceSelected: (ScannedDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    // A tick we increment to re-trigger the LaunchedEffect for a rescan.
    var scanTick by remember { mutableStateOf(0) }
    var scanning by remember { mutableStateOf(true) }
    var results by remember { mutableStateOf<List<ScannedDevice>>(emptyList()) }
    var error by remember { mutableStateOf<ScanState.Reason?>(null) }

    LaunchedEffect(scanTick) {
        scanning = true
        error = null
        results = emptyList()
        scanner.scan().collect { state ->
            when (state) {
                is ScanState.Discoveries -> results = state.devices
                is ScanState.Error -> {
                    error = state.reason
                    scanning = false
                }
            }
        }
        scanning = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,   // kiosk policy: no accidental outside-tap dismiss
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 480.dp, max = 720.dp)
                .heightIn(min = 400.dp, max = 640.dp)
                .clip(RoundedCornerShape(GwRadii.r2.dp))
                .background(GwColors.bg100)
                .border(1.dp, GwColors.strokeStrong, RoundedCornerShape(GwRadii.r2.dp)),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Title bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GwColors.bg200)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "PAIR WATCH",
                        style = GwTypography.H2.copy(color = GwColors.fg000),
                    )
                }

                // Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    StatusLine(
                        scanning = scanning,
                        resultCount = results.size,
                        error = error,
                    )
                    Spacer(Modifier.height(12.dp))
                    ResultsList(
                        results = results,
                        scanning = scanning,
                        error = error,
                        onDeviceSelected = onDeviceSelected,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Action bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GwColors.bg200)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    GhostButton(label = "Close", onClick = onDismiss)
                    Spacer(Modifier.width(8.dp))
                    PrimaryButton(
                        label = if (scanning) "Scanning…" else "Rescan",
                        onClick = {
                            if (!scanning) scanTick += 1
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusLine(
    scanning: Boolean,
    resultCount: Int,
    error: ScanState.Reason?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (scanning && error == null) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = GwColors.stateLive,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(8.dp))
        }
        val msg = when {
            error != null -> errorMessage(error)
            scanning && resultCount == 0 -> "Scanning for devices…"
            scanning -> "Scanning — $resultCount found"
            resultCount == 0 -> "No devices found. Ensure the watch is on and within range."
            else -> "$resultCount device${if (resultCount == 1) "" else "s"} found — tap to pair."
        }
        Text(
            text = msg,
            style = GwTypography.Mono.copy(
                color = if (error != null) GwColors.critRed else GwColors.fg200,
            ),
        )
    }
}

private fun errorMessage(reason: ScanState.Reason): String = when (reason) {
    ScanState.Reason.BLUETOOTH_UNAVAILABLE -> "Bluetooth unavailable on this device."
    ScanState.Reason.BLUETOOTH_OFF -> "Bluetooth is off. Enable it and retry."
    ScanState.Reason.PERMISSION_DENIED -> "Bluetooth permission denied. Grant in settings."
    ScanState.Reason.SCAN_FAILED -> "Scan failed. Toggle Bluetooth and retry."
}

@Composable
private fun ResultsList(
    results: List<ScannedDevice>,
    scanning: Boolean,
    error: ScanState.Reason?,
    onDeviceSelected: (ScannedDevice) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (error != null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = errorMessage(error),
                style = GwTypography.Mono.copy(color = GwColors.fg300),
            )
        }
        return
    }
    if (results.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = if (scanning) "Waiting for devices…" else "No devices in range.",
                style = GwTypography.Mono.copy(color = GwColors.fg300),
            )
        }
        return
    }
    LazyColumn(modifier = modifier) {
        items(items = results, key = { it.mac }) { device ->
            DeviceRow(device = device, onClick = { onDeviceSelected(device) })
        }
    }
}

@Composable
private fun DeviceRow(device: ScannedDevice, onClick: () -> Unit) {
    val signalColor = when {
        device.rssiDbm >= -60 -> GwColors.stateLive        // strong
        device.rssiDbm >= -80 -> GwColors.warnAmber         // medium
        else -> GwColors.fg300                              // weak
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GwRadii.r1.dp))
            .background(GwColors.bg200)
            .border(1.dp, GwColors.strokeDefault, RoundedCornerShape(GwRadii.r1.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Signal bar indicator
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .background(signalColor),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = device.name ?: "Unknown device",
                    style = GwTypography.MonoLg.copy(color = GwColors.fg000),
                )
                if (device.matchedGuardianService) {
                    Spacer(Modifier.width(8.dp))
                    GuardianBadge()
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = device.mac,
                style = GwTypography.MonoSm.copy(color = GwColors.fg300),
            )
        }
        Text(
            text = "${device.rssiDbm} dBm",
            style = GwTypography.MonoSm.copy(color = GwColors.fg200),
        )
        Spacer(Modifier.width(12.dp))
        // "Tap to pair" chevron
        Box(
            modifier = Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(GwRadii.r1.dp))
                .background(GwColors.chromeOlive500)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "PAIR",
                style = GwTypography.Label.copy(color = GwColors.fg000),
            )
        }
    }
}

@Composable
private fun GuardianBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(GwRadii.r1.dp))
            .background(GwColors.stateLive.copy(alpha = 0.18f))
            .border(1.dp, GwColors.stateLive, RoundedCornerShape(GwRadii.r1.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "GUARDIAN",
            style = GwTypography.Label.copy(color = GwColors.stateLive),
        )
    }
}
