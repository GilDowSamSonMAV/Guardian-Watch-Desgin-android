package com.gltech.guardianwatch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gltech.guardianwatch.ble.CasualtyStream
import com.gltech.guardianwatch.casualty.HealthTone
import com.gltech.guardianwatch.ui.components.*
import com.gltech.guardianwatch.ui.theme.GwColors
import com.gltech.guardianwatch.ui.theme.GwSpacing
import com.gltech.guardianwatch.ui.theme.GwTypography

/**
 * SINGLE-PAIRED MODE
 * One watch ↔ one tablet. Offline triage. Buddy-aid scenario.
 * Stripped-down UI — no LeftNav, one vital block, oversized HR readout.
 */
@Composable
fun SinglePairedScreen(
    stream: CasualtyStream?,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GwColors.bg000)
            .padding(GwSpacing.sp5.dp),
    ) {
        androidx.compose.material3.TextButton(onClick = onBack) {
            Text("← MENU", style = GwTypography.Label.copy(color = GwColors.fg200))
        }
        androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(GwSpacing.sp3.dp))
        if (stream == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No watch paired.\nPress PAIR to scan.",
                    style = GwTypography.Mono.copy(color = GwColors.fg300),
                )
            }
            return
        }

        CasualtyHeader(
            id = stream.casualty.id,
            name = stream.casualty.name,
            age = stream.casualty.age,
            mgrs = stream.casualty.mgrs,
            chainSummary = "OFFLINE · BUDDY-AID",
            triage = stream.casualty.triage,
        )
        Spacer(Modifier.height(GwSpacing.sp5.dp))

        if (stream.assessment?.overallTone == HealthTone.CRITICAL && stream.assessment.suspected != null) {
            CriticalBanner(
                title = stream.assessment.suspected.replace('_', ' '),
                body = "HR ${stream.latest?.hrBpm ?: "--"} bpm",
                onAck = {},
            )
            Spacer(Modifier.height(GwSpacing.sp4.dp))
        }

        // Oversized HR — readable at arm's length while treating casualty.
        Row(horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp4.dp)) {
            VitalTile(
                label = "HR",
                value = (stream.latest?.hrBpm ?: "--").toString(),
                unit = "bpm",
                trend = "trend: —",
                tone = stream.assessment?.hrTone ?: HealthTone.OK,
                modifier = Modifier.weight(2f),
            )
            VitalTile(
                label = "BATT",
                value = (stream.latest?.batteryPct ?: 0).toString(),
                unit = "%",
                trend = "BLE: ${stream.latest?.rssiDbm ?: "--"} dBm",
                tone = HealthTone.OK,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(GwSpacing.sp4.dp))
        BiometricChart(
            title = "Heart Rate",
            latestValue = (stream.latest?.hrBpm ?: "--").toString(),
            unit = "bpm",
            tone = stream.assessment?.hrTone ?: HealthTone.OK,
            samples = stream.hrHistory,
            timeLabel = "last 10 min",
        )
    }
}

/**
 * RELAY MODE
 * Tablet receives BLE, forwards to upstream (cellular via Tab A9+ 5G radio).
 * Minimal UI — just bridge status. Tablet may be pocketed / in a ruck.
 */
@Composable
fun RelayScreen(
    streams: Map<String, CasualtyStream>,
    upstreamConnected: Boolean,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GwColors.bg000)
            .padding(GwSpacing.sp5.dp),
    ) {
        androidx.compose.material3.TextButton(onClick = onBack) {
            Text("← MENU", style = GwTypography.Label.copy(color = GwColors.fg200))
        }
        androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(GwSpacing.sp3.dp))
        Text("RELAY NODE", style = GwTypography.H1.copy(color = GwColors.fg000))
        Spacer(Modifier.height(GwSpacing.sp1.dp))
        Text(
            "BLE → cellular bridge. Screen may be off; service runs in foreground.",
            style = GwTypography.Body.copy(color = GwColors.fg200),
        )
        Spacer(Modifier.height(GwSpacing.sp5.dp))

        // Upstream status
        StatusRow(
            label = "UPSTREAM",
            value = if (upstreamConnected) "CONNECTED" else "DISCONNECTED",
            tone = if (upstreamConnected) HealthTone.OK else HealthTone.CRITICAL,
        )
        Spacer(Modifier.height(GwSpacing.sp2.dp))
        StatusRow(
            label = "PAIRED WATCHES",
            value = streams.size.toString(),
            tone = HealthTone.OK,
        )
        Spacer(Modifier.height(GwSpacing.sp4.dp))

        // Per-watch mini status
        streams.values.forEach { s ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = GwSpacing.sp1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TriageGlyph(s.casualty.triage, size = 20)
                Spacer(Modifier.width(GwSpacing.sp3.dp))
                Text(
                    s.casualty.id,
                    style = GwTypography.Mono.copy(color = GwColors.fg000),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "HR ${s.latest?.hrBpm ?: "--"}",
                    style = GwTypography.MonoSm.copy(color = GwColors.fg200),
                )
                Spacer(Modifier.width(GwSpacing.sp4.dp))
                Text(
                    "${s.latest?.rssiDbm ?: "--"} dBm",
                    style = GwTypography.MonoSm.copy(color = GwColors.fg300),
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, tone: HealthTone) {
    val valueColor = when (tone) {
        HealthTone.CRITICAL -> GwColors.critRed
        HealthTone.WARN -> GwColors.warnAmber
        HealthTone.OK -> GwColors.stateLive
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = GwTypography.Label.copy(color = GwColors.fg200))
        Spacer(Modifier.width(GwSpacing.sp3.dp))
        Text(value, style = GwTypography.MonoLg.copy(color = valueColor))
    }
}
