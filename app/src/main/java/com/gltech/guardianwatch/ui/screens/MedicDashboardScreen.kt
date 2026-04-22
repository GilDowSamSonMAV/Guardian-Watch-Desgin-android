package com.gltech.guardianwatch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gltech.guardianwatch.ble.CasualtyStream
import com.gltech.guardianwatch.ble.VitalsRepository
import com.gltech.guardianwatch.casualty.Casualty
import com.gltech.guardianwatch.casualty.HealthTone
import com.gltech.guardianwatch.casualty.Triage
import com.gltech.guardianwatch.ui.components.*
import com.gltech.guardianwatch.ui.theme.GwColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Medic Dashboard — the primary field screen.
 *
 * For MVP: a single casualty view (the most-critical). Multi-casualty list is
 * the next iteration — see LeftNav's CASUALTIES tab.
 */
@Composable
fun MedicDashboardScreen(
    streams: Map<String, CasualtyStream>,
    selfId: String,
    onHandoffConfirmed: (casualtyId: String, event: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var selectedNav by remember { mutableStateOf(NavItem.CASUALTIES) }
    var showConfirm by remember { mutableStateOf(false) }

    // Pick the most-critical casualty to feature.
    val featured = streams.values
        .sortedByDescending { (it.assessment?.overallTone ?: HealthTone.OK).ordinal }
        .firstOrNull()

    val time = remember { mutableStateOf(nowIso()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            time.value = nowIso()
        }
    }

    Column(modifier = modifier.fillMaxSize().background(GwColors.bg000)) {
        TopBar(
            missionLabel = "TF-IRON · DAY 14",
            selfId = selfId,
            bleRssi = featured?.latest?.rssiDbm ?: -127,
            batteryPct = featured?.latest?.batteryPct ?: 0,
            timeStr = time.value,
        )
        Row(modifier = Modifier.weight(1f)) {
            LeftNav(selected = selectedNav, onSelect = { selectedNav = it })
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                if (featured != null) {
                    val latest = featured.latest
                    val assess = featured.assessment
                    CasualtyHeader(
                        id = featured.casualty.id,
                        name = featured.casualty.name,
                        age = featured.casualty.age,
                        mgrs = featured.casualty.mgrs,
                        chainSummary = "POI → MED → CASEVAC",
                        triage = featured.casualty.triage,
                    )
                    Spacer(Modifier.height(16.dp))

                    // Critical banner when anomaly engine flags suspect.
                    if (assess?.overallTone == HealthTone.CRITICAL && assess.suspected != null) {
                        val bodyText = buildString {
                            append(featured.casualty.id)
                            append(" · HR ")
                            append(latest?.hrBpm ?: "--")
                            append(" · movement STILL")
                        }
                        CriticalBanner(
                            title = assess.suspected.replace('_', ' '),
                            body = bodyText,
                            onAck = { showConfirm = true },
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    // Vital tiles row — HR only on Instinct 2 (no SpO2/temp sensor).
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        VitalTile(
                            label = "HR",
                            value = (latest?.hrBpm ?: "--").toString(),
                            unit = "bpm",
                            trend = "trend: ${assess?.let { "—" } ?: "—"}",
                            tone = assess?.hrTone ?: HealthTone.OK,
                            modifier = Modifier.weight(1f),
                        )
                        VitalTile(
                            label = "SpO₂",
                            value = "--",
                            unit = "%",
                            trend = "sensor: unavailable",
                            tone = HealthTone.OK,
                            modifier = Modifier.weight(1f),
                        )
                        VitalTile(
                            label = "SKIN T",
                            value = "--",
                            unit = "°C",
                            trend = "sensor: unavailable",
                            tone = HealthTone.OK,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    BiometricChart(
                        title = "Heart Rate",
                        latestValue = (latest?.hrBpm ?: "--").toString(),
                        unit = "bpm",
                        tone = assess?.hrTone ?: HealthTone.OK,
                        samples = featured.hrHistory,
                        timeLabel = "last 10 min",
                    )
                    Spacer(Modifier.height(16.dp))

                    EvacTimeline()
                    Spacer(Modifier.height(16.dp))

                    if (showConfirm) {
                        val handoffEvent = "HANDOFF → ROLE-2"
                        ConfirmBar(
                            label = "Handoff to ROLE-2 for",
                            target = featured.casualty.id,
                            onConfirm = {
                                onHandoffConfirmed(featured.casualty.id, handoffEvent)
                                showConfirm = false
                            },
                            onCancel = { showConfirm = false },
                        )
                    }
                } else {
                    EmptyState(message = "No casualties paired.\nUse the CAS tab to pair a watch.")
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = message,
            style = com.gltech.guardianwatch.ui.theme.GwTypography.Mono.copy(color = GwColors.fg300),
        )
    }
}

private fun nowIso(): String =
    SimpleDateFormat("HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date())
