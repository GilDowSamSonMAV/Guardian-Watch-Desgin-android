package com.gltech.guardianwatch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gltech.guardianwatch.model.Soldier
import com.gltech.guardianwatch.ui.theme.GwColors
import com.gltech.guardianwatch.ui.theme.GwRadii
import com.gltech.guardianwatch.ui.theme.GwSpacing
import com.gltech.guardianwatch.ui.theme.GwTypography

/**
 * 9-Line MEDEVAC request overlay.
 * Matches reference screenshots 5 and 6.
 *
 * States: DRAFT → button shows "TRANSMIT 9-LINE" (red)
 *         TRANSMITTED → button shows "✓ TRANSMITTED" (green), air asset status = AIRBORNE
 */
@Composable
fun NineLineOverlay(
    soldier: Soldier?,
    onClose: () -> Unit,
) {
    var transmitted by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(700.dp)
                    .clip(RoundedCornerShape(GwRadii.r2.dp))
                    .background(GwColors.bg100)
                    .border(2.dp, GwColors.critRed, RoundedCornerShape(GwRadii.r2.dp))
                    .clickable { /* consume */ },
            ) {
                Column {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GwColors.bg200)
                            .padding(horizontal = GwSpacing.sp5.dp, vertical = GwSpacing.sp3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                "9-LINE MEDEVAC REQUEST",
                                style = GwTypography.H2.copy(color = GwColors.critRed),
                            )
                            Text(
                                "DRAFT · VERIFY · TRANSMIT",
                                style = GwTypography.Audit.copy(color = GwColors.fg300),
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(GwSpacing.sp7.dp)
                                .clip(RoundedCornerShape(GwRadii.r1.dp))
                                .border(1.dp, GwColors.strokeDefault, RoundedCornerShape(GwRadii.r1.dp))
                                .clickable { onClose() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✕", style = GwTypography.Mono.copy(color = GwColors.fg200))
                        }
                    }

                    // 9-Line body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(GwSpacing.sp4.dp),
                    ) {
                        NineLineRow(1, "PICKUP LOCATION",  "18S TJ 4486 8741", warn = false)
                        NineLineRow(2, "FREQ / CALLSIGN",  "FH-7 · MEDCOM-6",  warn = false)
                        NineLineRow(3, "PRECEDENCE",       "A · URGENT",       warn = true)
                        NineLineRow(4, "SPECIAL EQUIP",    "VENT · BLOOD",     warn = false)
                        NineLineRow(5, "PATIENTS / TYPE",  "1 · LITTER",       warn = false)
                        NineLineRow(6, "SECURITY",         "E · ENEMY · ESCORT", warn = true)
                        NineLineRow(7, "MARKING METHOD",   "VS-17 / IR STROBE",warn = false)
                        NineLineRow(8, "PATIENT NAT.",     "IDF · ISRAELI",    warn = false)
                        NineLineRow(9, "NBC CONTAM",       "NEGATIVE",         warn = false)
                    }

                    // Patient summary strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GwColors.bg300)
                            .padding(horizontal = GwSpacing.sp5.dp, vertical = GwSpacing.sp2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("PATIENT", style = GwTypography.Label.copy(color = GwColors.fg300))
                        Spacer(Modifier.width(GwSpacing.sp3.dp))
                        val patText = if (soldier != null) {
                            "POS-${soldier.pos.toString().padStart(2,'0')} · ${soldier.last} · " +
                            "${soldier.role.display} · HR ${soldier.hr} · BR ${soldier.br} · " +
                            "RISK ${String.format("%.1f", soldier.risk)}"
                        } else "—"
                        Text(patText, style = GwTypography.Mono.copy(color = GwColors.fg000))
                    }

                    // Footer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GwColors.bg200)
                            .padding(horizontal = GwSpacing.sp5.dp, vertical = GwSpacing.sp3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Cancel
                        Box(
                            modifier = Modifier
                                .height(GwSpacing.sp7.dp)
                                .clip(RoundedCornerShape(GwRadii.r1.dp))
                                .background(Color.Transparent)
                                .border(1.dp, GwColors.strokeDefault, RoundedCornerShape(GwRadii.r1.dp))
                                .clickable { onClose() }
                                .padding(horizontal = GwSpacing.sp5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("CANCEL", style = GwTypography.Label.copy(color = GwColors.fg200))
                        }

                        Spacer(Modifier.weight(1f))

                        // ETA info
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            EtaCell("NEAREST LZ", "LZ-ALPHA · 1.2 KM")
                            EtaCell("AIR ASSET",
                                "YAS-08 · ${if (transmitted) "AIRBORNE" else "ALERTED"}")
                            EtaCell("ETA", "08:12", GwColors.warnAmber)
                        }

                        Spacer(Modifier.weight(1f))

                        // Transmit button
                        val txBg    = if (transmitted) Color(0xFF0D3B1E) else GwColors.critRed
                        val txBorder= if (transmitted) GwColors.stateLive else GwColors.critRed
                        val txText  = if (transmitted) "✓ TRANSMITTED" else "TRANSMIT 9-LINE"
                        val txColor = if (transmitted) GwColors.stateLive else GwColors.fg000

                        Box(
                            modifier = Modifier
                                .height(GwSpacing.sp7.dp)
                                .clip(RoundedCornerShape(GwRadii.r1.dp))
                                .background(txBg)
                                .border(1.dp, txBorder, RoundedCornerShape(GwRadii.r1.dp))
                                .clickable(enabled = !transmitted) { transmitted = true }
                                .padding(horizontal = GwSpacing.sp6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(txText, style = GwTypography.Label.copy(color = txColor))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NineLineRow(n: Int, label: String, value: String, warn: Boolean) {
    val valueColor = if (warn) GwColors.warnAmber else GwColors.fg000
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(
                if (warn) GwColors.warnAmberBg.copy(alpha = 0.3f) else Color.Transparent,
                RoundedCornerShape(GwRadii.r1.dp),
            )
            .padding(horizontal = GwSpacing.sp2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            n.toString().padStart(2, '0'),
            style = GwTypography.MonoLg.copy(color = if (warn) GwColors.warnAmber else GwColors.fg300),
            modifier = Modifier.width(36.dp),
        )
        Text(
            label,
            style = GwTypography.Label.copy(color = GwColors.fg300),
            modifier = Modifier.width(160.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(value, style = GwTypography.Mono.copy(color = valueColor))
    }
}

@Composable
private fun EtaCell(label: String, value: String, valueColor: Color = GwColors.fg000) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = GwTypography.Audit.copy(color = GwColors.fg300))
        Text(value, style = GwTypography.MonoSm.copy(color = valueColor))
    }
}
