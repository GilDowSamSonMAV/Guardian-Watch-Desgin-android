package com.gltech.guardianwatch.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gltech.guardianwatch.ui.components.BrandMonogram
import com.gltech.guardianwatch.model.CriticalAlert
import com.gltech.guardianwatch.model.Soldier
import com.gltech.guardianwatch.model.SoldierStatus
import com.gltech.guardianwatch.ui.theme.GwColors
import com.gltech.guardianwatch.ui.theme.GwRadii
import com.gltech.guardianwatch.ui.theme.GwSpacing
import com.gltech.guardianwatch.ui.theme.GwTypography

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR — MEDCOM · ALEPH-6 | SATCOM | MESH | TELEMED | GPS | time | batt
// ─────────────────────────────────────────────────────────────────────────────

/** Connection/system status for the top status chip row. */
data class SystemStatus(
    val satcomLocked: Boolean  = true,
    val meshConnected: Int     = 14,   // connected watches
    val meshTotal: Int         = 14,
    val telemedOnline: Boolean = true,
    val gpsMode: String        = "HI-PREC",
    val batteryPct: Int        = 78,
)

@Composable
fun TacticalTopBar(
    missionLabel: String,    // "MEDCOM · ALEPH-6"
    opLabel: String,         // "OP NORTH WIND · D+3"
    timeStr: String,         // "01:47:20"
    status: SystemStatus = SystemStatus(),
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFF0A0C0A))
            .border(width = 1.dp, color = GwColors.strokeHairline),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(GwSpacing.sp3.dp))

        // Brand monogram
        BrandMonogram(size = 18)
        Spacer(Modifier.width(GwSpacing.sp3.dp))

        // Mission + op
        Column {
            Text(
                text = missionLabel,
                style = GwTypography.Label.copy(color = GwColors.fg000, fontSize = androidx.compose.ui.unit.sp(13f)),
            )
            Text(
                text = opLabel,
                style = GwTypography.Audit.copy(color = GwColors.fg300),
            )
        }

        Spacer(Modifier.weight(1f))

        // Status chips
        StatusChip("SATCOM", if (status.satcomLocked) "LOCK" else "NO LOCK",
            if (status.satcomLocked) GwColors.stateLive else GwColors.critRed)
        Spacer(Modifier.width(GwSpacing.sp3.dp))
        StatusChip("MESH", "${status.meshConnected}/${status.meshTotal}",
            GwColors.stateLive)
        Spacer(Modifier.width(GwSpacing.sp3.dp))
        StatusChip("TELEMED", if (status.telemedOnline) "ONLINE" else "OFFLINE",
            if (status.telemedOnline) GwColors.stateLive else GwColors.stateStale)
        Spacer(Modifier.width(GwSpacing.sp3.dp))
        StatusChip("GPS", status.gpsMode, GwColors.stateLive)

        Spacer(Modifier.width(GwSpacing.sp5.dp))

        // Clock
        Text(
            text = timeStr,
            style = GwTypography.MonoLg.copy(color = GwColors.fg000),
        )
        Spacer(Modifier.width(GwSpacing.sp3.dp))

        // Battery
        BatteryIndicator(pct = status.batteryPct)
        Spacer(Modifier.width(GwSpacing.sp4.dp))
    }
}

@Composable
fun StatusChip(label: String, value: String, valueColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Live indicator dot
        val dotColor = valueColor
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = GwTypography.Audit.copy(color = GwColors.fg300))
        Spacer(Modifier.width(3.dp))
        Text(value, style = GwTypography.Audit.copy(color = valueColor))
    }
}

@Composable
private fun BatteryIndicator(pct: Int) {
    val color = when {
        pct > 50 -> GwColors.stateLive
        pct > 20 -> GwColors.warnAmber
        else     -> GwColors.critRed
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Simple bar
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(10.dp)
                .border(1.dp, GwColors.fg300, RoundedCornerShape(2.dp))
                .padding(1.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(pct / 100f)
                    .background(color, RoundedCornerShape(1.dp))
            )
        }
        Spacer(Modifier.width(3.dp))
        Text("$pct%", style = GwTypography.Audit.copy(color = GwColors.fg200))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GLOBAL CRITICAL ALERT BANNER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlobalAlertBanner(
    alert: CriticalAlert,
    soldier: Soldier?,
    onView: () -> Unit,
    onAck: () -> Unit,
    onCasevac: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Pulsing background animation for CRITICAL
    val infiniteTransition = rememberInfiniteTransition(label = "alertPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    val bgColor = Color(0xFF3D0507).copy(alpha = pulse)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(bgColor)
            .border(width = 1.dp, color = GwColors.critRed),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left accent rail
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(GwColors.critRed)
        )
        Spacer(Modifier.width(GwSpacing.sp3.dp))

        // Warning icon
        Text("⚠", style = GwTypography.H2.copy(color = GwColors.critRed))
        Spacer(Modifier.width(GwSpacing.sp2.dp))

        // CRITICAL badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(GwRadii.r1.dp))
                .background(GwColors.critRed)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text("CRITICAL", style = GwTypography.Label.copy(color = GwColors.fg000))
        }
        Spacer(Modifier.width(GwSpacing.sp3.dp))

        // Soldier info
        if (soldier != null) {
            Text(
                text = "POS-${soldier.pos.toString().padStart(2,'0')} ${soldier.last}",
                style = GwTypography.Mono.copy(color = GwColors.fg000),
            )
            Spacer(Modifier.width(GwSpacing.sp2.dp))
            Text("·", style = GwTypography.Mono.copy(color = GwColors.fg300))
            Spacer(Modifier.width(GwSpacing.sp2.dp))
            Text(
                text = soldier.role.display,
                style = GwTypography.Mono.copy(color = GwColors.fg200),
            )
            Spacer(Modifier.width(GwSpacing.sp2.dp))
            Text("·", style = GwTypography.Mono.copy(color = GwColors.fg300))
            Spacer(Modifier.width(GwSpacing.sp2.dp))
            Text(
                text = soldier.id,
                style = GwTypography.Mono.copy(color = GwColors.fg200),
            )
            Spacer(Modifier.width(GwSpacing.sp3.dp))
        }

        // Alert message
        Text(
            text = alert.message,
            style = GwTypography.Mono.copy(color = GwColors.fg000),
            modifier = Modifier.weight(1f),
        )

        // Vitals quick view (from soldier)
        if (soldier != null) {
            QuickVital("HR", soldier.hr.toString())
            Spacer(Modifier.width(GwSpacing.sp2.dp))
            QuickVital("SpO₂", if (soldier.spo2 > 0) soldier.spo2.toString() else "--")
            Spacer(Modifier.width(GwSpacing.sp2.dp))
            QuickVital("RISK", String.format("%.1f", soldier.risk))
            Spacer(Modifier.width(GwSpacing.sp3.dp))

            // T+Xs timer
            val tColor = GwColors.stateLive
            Text(
                text = "T+${alert.triggeredSec}s",
                style = GwTypography.MonoSm.copy(color = tColor),
            )
            Spacer(Modifier.width(GwSpacing.sp4.dp))
        }

        // Action buttons
        AlertActionButton("VIEW", Color(0xFF1A3040), onClick = onView)
        Spacer(Modifier.width(GwSpacing.sp2.dp))
        AlertActionButton("ACK",  Color(0xFF2A1E0A), onClick = onAck)
        Spacer(Modifier.width(GwSpacing.sp2.dp))
        AlertActionButton("CASEVAC", GwColors.critRed, onClick = onCasevac)
        Spacer(Modifier.width(GwSpacing.sp3.dp))
    }
}

@Composable
private fun QuickVital(label: String, value: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(label, style = GwTypography.Audit.copy(color = GwColors.fg300))
        Spacer(Modifier.width(2.dp))
        Text(value, style = GwTypography.MonoSm.copy(color = GwColors.fg000))
    }
}

@Composable
private fun AlertActionButton(label: String, bg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(GwRadii.r1.dp))
            .background(bg)
            .border(1.dp, GwColors.strokeStrong, RoundedCornerShape(GwRadii.r1.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = GwTypography.Label.copy(color = GwColors.fg000,
            fontSize = androidx.compose.ui.unit.sp(12f)))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BREADCRUMB NAV
// ─────────────────────────────────────────────────────────────────────────────

data class BreadcrumbItem(val tag: String, val label: String)

@Composable
fun Breadcrumb(
    items: List<BreadcrumbItem>,
    onNavigate: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { i, item ->
            if (i > 0) {
                Spacer(Modifier.width(6.dp))
                Text("›", style = GwTypography.MonoSm.copy(color = GwColors.fg300))
                Spacer(Modifier.width(6.dp))
            }
            val isLast = i == items.size - 1
            val tagBg = if (isLast) GwColors.infoCyan else Color.Transparent
            val tagFg = if (isLast) GwColors.bg000 else GwColors.fg300
            Row(
                modifier = Modifier.clickable { if (!isLast) onNavigate(i) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(tagBg)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(item.tag, style = GwTypography.Audit.copy(color = tagFg))
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = item.label,
                    style = GwTypography.Mono.copy(
                        color = if (isLast) GwColors.fg000 else GwColors.fg300
                    ),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FOOTER STATUS BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FootStatusBar(
    meshOnline: Int,
    meshTotal: Int,
    lastMsg: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(Color(0xFF080A08))
            .border(topStart = 1.dp, topEnd = 1.dp, color = GwColors.strokeHairline,
                shape = RoundedCornerShape(0.dp))
            .padding(horizontal = GwSpacing.sp3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FootCell("MESH", "$meshOnline/$meshTotal")
        FootCell("AVG LAT", "142ms")
        FootCell("DATA", "2.4 KB/s")
        Spacer(Modifier.weight(1f))
        Text(
            text = lastMsg,
            style = GwTypography.Audit.copy(color = GwColors.fg300),
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        FootCell("OPS CHAN", "FH-7 / 2400")
    }
}

@Composable
private fun FootCell(label: String, value: String) {
    Row(
        modifier = Modifier.padding(end = GwSpacing.sp5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = GwTypography.Audit.copy(color = GwColors.fg300))
        Spacer(Modifier.width(4.dp))
        Text(value, style = GwTypography.Audit.copy(color = GwColors.fg100))
    }
}

// Extension for border only on specific sides
@Composable
private fun Modifier.border(
    topStart: androidx.compose.ui.unit.Dp = 0.dp,
    topEnd: androidx.compose.ui.unit.Dp = 0.dp,
    color: Color,
    shape: RoundedCornerShape,
): Modifier = this
