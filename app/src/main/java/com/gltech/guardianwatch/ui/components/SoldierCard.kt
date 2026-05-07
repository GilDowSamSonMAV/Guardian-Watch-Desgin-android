package com.gltech.guardianwatch.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gltech.guardianwatch.model.Soldier
import com.gltech.guardianwatch.model.SoldierStatus
import com.gltech.guardianwatch.ui.theme.GwColors
import com.gltech.guardianwatch.ui.theme.GwRadii
import com.gltech.guardianwatch.ui.theme.GwSpacing
import com.gltech.guardianwatch.ui.theme.GwTypography

/**
 * Per-soldier card used in the squad vitals grid.
 * 2-column × 4-row layout = 8 soldiers per squad.
 * Shows: position, name, role badge, HR/BR/SpO₂/TEMP, composite risk bar,
 * mesh signal bar, battery %, staleness timer.
 */
@Composable
fun SoldierCard(
    soldier: Soldier,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColor = soldier.status.color
    val isCritical  = soldier.status == SoldierStatus.CRITICAL
    val isOffline   = soldier.status == SoldierStatus.OFFLINE

    val borderColor = when {
        selected  -> GwColors.infoCyan
        isCritical -> statusColor
        else      -> GwColors.strokeHairline
    }
    val bgColor = when {
        isCritical -> Color(0xFF1A0305)
        selected   -> Color(0xFF091316)
        else       -> GwColors.bg200
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GwRadii.r2.dp))
            .background(bgColor)
            .border(
                width = if (isCritical || selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(GwRadii.r2.dp),
            )
            .clickable { onClick() }
            .padding(GwSpacing.sp3.dp),
    ) {
        // Header row: pos + name + role + status
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Position number
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "POS",
                    style = GwTypography.Audit.copy(color = GwColors.fg300, fontSize = 9.sp),
                )
                Text(
                    soldier.pos.toString().padStart(2, '0'),
                    style = GwTypography.MonoLg.copy(color = GwColors.fg200),
                )
            }
            Spacer(Modifier.width(GwSpacing.sp3.dp))

            // Name + role
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = soldier.last,
                    style = GwTypography.Label.copy(color = GwColors.fg000),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoleBadge(soldier.role.display)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = soldier.id,
                        style = GwTypography.Audit.copy(color = GwColors.fg300),
                    )
                }
            }

            // Status label
            StatusLabel(soldier.status)
        }

        Spacer(Modifier.height(GwSpacing.sp3.dp))

        // Vitals row: HR | BR | SpO₂ | TEMP
        if (!isOffline) {
            Row(horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp3.dp)) {
                VitalMini("HR", soldier.hr.toString(), "bpm",
                    if (soldier.hr > 140) GwColors.critRed
                    else if (soldier.hr > 100) GwColors.warnAmber
                    else GwColors.fg000)
                VitalMini("BR", soldier.br.toString(), "rpm",
                    if (soldier.br > 22) GwColors.warnAmber else GwColors.fg000)
                VitalMini("SpO₂",
                    if (soldier.spo2 > 0) soldier.spo2.toString() else "--", "%",
                    if (soldier.spo2 in 1..91) GwColors.critRed else GwColors.fg000)
                VitalMini("TEMP",
                    if (soldier.coreTemp > 0f) String.format("%.1f", soldier.coreTemp) else "--",
                    "°C",
                    if (soldier.coreTemp > 38f) GwColors.warnAmber else GwColors.fg000)
            }
            Spacer(Modifier.height(GwSpacing.sp2.dp))

            // Risk bar
            RiskBar(risk = soldier.risk, statusColor = statusColor)

            Spacer(Modifier.height(GwSpacing.sp2.dp))
        } else {
            // Offline — show stale indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .border(1.dp, GwColors.strokeHairline, RoundedCornerShape(GwRadii.r1.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("OFFLINE · ${formatStaleness(soldier.lastUpdateSec)}",
                    style = GwTypography.Audit.copy(color = GwColors.fg300))
            }
            Spacer(Modifier.height(GwSpacing.sp2.dp))
        }

        // Footer: MESH signal + battery + staleness
        Row(verticalAlignment = Alignment.CenterVertically) {
            MeshBar(soldier.meshSignal)
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${soldier.batteryPct}%",
                style = GwTypography.Audit.copy(color = GwColors.fg300),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "T+${formatStaleness(soldier.lastUpdateSec)}",
                style = GwTypography.Audit.copy(
                    color = if (soldier.lastUpdateSec > 30) GwColors.warnAmber else GwColors.fg300,
                ),
            )
        }
    }
}

@Composable
private fun VitalMini(label: String, value: String, unit: String, valueColor: Color) {
    Column {
        Text(label, style = GwTypography.Audit.copy(color = GwColors.fg300, fontSize = 9.sp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = GwTypography.MonoSm.copy(color = valueColor))
            Spacer(Modifier.width(1.dp))
            Text(unit, style = GwTypography.Audit.copy(color = GwColors.fg400, fontSize = 9.sp))
        }
    }
}

@Composable
fun RiskBar(risk: Float, statusColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("COMPOSITE RISK", style = GwTypography.Audit.copy(
                color = GwColors.fg300, fontSize = 9.sp))
            Spacer(Modifier.weight(1f))
            Text(
                text = String.format("%.1f", risk) + " /10",
                style = GwTypography.MonoSm.copy(color = statusColor),
            )
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(GwColors.bg400),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((risk / 10f).coerceIn(0f, 1f))
                    .background(statusColor, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun RoleBadge(role: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(GwRadii.r1.dp))
            .background(GwColors.bg400)
            .border(1.dp, GwColors.strokeDefault, RoundedCornerShape(GwRadii.r1.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(role, style = GwTypography.Audit.copy(color = GwColors.fg200))
    }
}

@Composable
private fun StatusLabel(status: SoldierStatus) {
    if (status == SoldierStatus.OK) {
        // Subtle dot for OK
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .background(status.color))
            Spacer(Modifier.width(3.dp))
            Text("OK", style = GwTypography.Audit.copy(color = status.color))
        }
        return
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(GwRadii.r1.dp))
            .background(
                when (status) {
                    SoldierStatus.CRITICAL -> GwColors.critRedBg
                    SoldierStatus.HIGH     -> Color(0xFF3D2000)
                    SoldierStatus.CAUTION  -> Color(0xFF2A1800)
                    else                   -> GwColors.bg300
                }
            )
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        Text(
            text = status.label,
            style = GwTypography.Audit.copy(color = status.color),
        )
    }
}

@Composable
private fun MeshBar(signal: com.gltech.guardianwatch.model.MeshSignal) {
    val color = when (signal) {
        com.gltech.guardianwatch.model.MeshSignal.STRONG -> GwColors.stateLive
        com.gltech.guardianwatch.model.MeshSignal.WEAK   -> GwColors.stateStale
        com.gltech.guardianwatch.model.MeshSignal.NONE   -> GwColors.fg400
    }
    // 3-bar signal icon drawn with Canvas
    Canvas(modifier = Modifier.size(width = 12.dp, height = 10.dp)) {
        val barW  = size.width / 5f
        val count = when (signal) {
            com.gltech.guardianwatch.model.MeshSignal.STRONG -> 3
            com.gltech.guardianwatch.model.MeshSignal.WEAK   -> 2
            com.gltech.guardianwatch.model.MeshSignal.NONE   -> 0
        }
        for (i in 0 until 3) {
            val h    = size.height * (i + 1) / 3f
            val x    = i * (barW + 2.dp.toPx())
            val barC = if (i < count) color else color.copy(alpha = 0.2f)
            drawRect(barC,
                topLeft = Offset(x, size.height - h),
                size    = androidx.compose.ui.geometry.Size(barW, h))
        }
    }
}

private fun formatStaleness(sec: Int): String = when {
    sec < 60   -> "${sec}s"
    sec < 3600 -> "${sec / 60}m${sec % 60}s"
    else       -> "${sec / 3600}h"
}
