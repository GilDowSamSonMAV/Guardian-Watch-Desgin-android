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
import androidx.compose.ui.unit.sp
import com.gltech.guardianwatch.ui.components.BrandMonogram
import com.gltech.guardianwatch.model.CriticalAlert
import com.gltech.guardianwatch.model.Soldier
import com.gltech.guardianwatch.model.SoldierStatus
import com.gltech.guardianwatch.ui.theme.GwColors
import com.gltech.guardianwatch.ui.theme.GwRadii
import com.gltech.guardianwatch.ui.theme.GwSpacing
import com.gltech.guardianwatch.ui.theme.GwTypography

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR — Cleaner, less dense. Single horizontal strip.
// Shows: mission label | status pills | clock | battery
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
            .height(34.dp)
            .background(Color(0xFF0A0C0A))
            .border(width = 1.dp, color = GwColors.strokeHairline),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(GwSpacing.sp3.dp))

        // Brand monogram
        BrandMonogram(size = 16)
        Spacer(Modifier.width(GwSpacing.sp2.dp))

        // Mission label — compact
        Text(
            text = missionLabel,
            style = GwTypography.Label.copy(color = GwColors.fg000, fontSize = 12.sp),
        )
        Spacer(Modifier.width(GwSpacing.sp2.dp))
        Text(
            text = opLabel,
            style = GwTypography.Audit.copy(color = GwColors.fg300, fontSize = 9.sp),
        )

        Spacer(Modifier.weight(1f))

        // Status pills — compact inline
        StatusPill("SATCOM", if (status.satcomLocked) "LOCK" else "—",
            if (status.satcomLocked) GwColors.stateLive else GwColors.critRed)
        Spacer(Modifier.width(GwSpacing.sp2.dp))
        StatusPill("MESH", "${status.meshConnected}/${status.meshTotal}",
            GwColors.stateLive)
        Spacer(Modifier.width(GwSpacing.sp2.dp))
        StatusPill("TELEMED", if (status.telemedOnline) "ON" else "OFF",
            if (status.telemedOnline) GwColors.stateLive else GwColors.stateStale)
        Spacer(Modifier.width(GwSpacing.sp2.dp))
        StatusPill("GPS", status.gpsMode, GwColors.stateLive)

        Spacer(Modifier.width(GwSpacing.sp4.dp))

        // Clock — bold mono
        Text(
            text = timeStr,
            style = GwTypography.MonoLg.copy(color = GwColors.infoCyan, fontSize = 16.sp),
        )
        Spacer(Modifier.width(GwSpacing.sp2.dp))

        // Date
        val cal = java.util.Calendar.getInstance()
        val dayOfWeek = java.text.SimpleDateFormat("EEE", java.util.Locale.US).format(cal.time).uppercase()
        val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US).format(cal.time).uppercase()
        Text(
            text = "$dayOfWeek $dateStr",
            style = GwTypography.Audit.copy(color = GwColors.fg300, fontSize = 9.sp),
        )

        Spacer(Modifier.width(GwSpacing.sp3.dp))

        // Battery
        BatteryIndicator(pct = status.batteryPct)
        Spacer(Modifier.width(GwSpacing.sp3.dp))
    }
}

/** Compact inline status pill: dot + label + value */
@Composable
private fun StatusPill(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(GwColors.bg200.copy(alpha = 0.6f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Live dot
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(valueColor)
        )
        Spacer(Modifier.width(3.dp))
        Text(label, style = GwTypography.Audit.copy(color = GwColors.fg300, fontSize = 9.sp))
        Spacer(Modifier.width(3.dp))
        Text(value, style = GwTypography.Audit.copy(color = valueColor, fontSize = 10.sp))
    }
}

// Keep old StatusChip for backward compat if used elsewhere
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
                .width(20.dp)
                .height(9.dp)
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
        Text("$pct%", style = GwTypography.Audit.copy(color = GwColors.fg200, fontSize = 9.sp))
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
    // ★ AGGRESSIVE: fast pulse 350ms, strong alpha swing 0.45→1.0
    val infiniteTransition = rememberInfiniteTransition(label = "alertPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    // Second counter-phase animation for border flicker
    val borderPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue  = 0.3f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 250),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "borderPulse",
    )

    val bgColor = Color(0xFF3D0507).copy(alpha = pulse)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(bgColor)
            .border(width = (1.5 * borderPulse).dp, color = GwColors.critRed),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left accent rail
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(GwColors.critRed)
        )
        Spacer(Modifier.width(GwSpacing.sp2.dp))

        // Warning icon
        Text("⚠", style = GwTypography.H2.copy(color = GwColors.critRed, fontSize = 16.sp))
        Spacer(Modifier.width(GwSpacing.sp2.dp))

        // CRITICAL badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(GwRadii.r1.dp))
                .background(GwColors.critRed)
                .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
            Text("CRITICAL", style = GwTypography.Label.copy(color = GwColors.fg000, fontSize = 10.sp))
        }
        Spacer(Modifier.width(GwSpacing.sp2.dp))

        // Soldier info — compact
        if (soldier != null) {
            Text(
                text = "POS-${soldier.pos.toString().padStart(2,'0')} ${soldier.last}",
                style = GwTypography.Mono.copy(color = GwColors.fg000, fontSize = 12.sp),
            )
            Spacer(Modifier.width(GwSpacing.sp2.dp))
            Text("·", style = GwTypography.Mono.copy(color = GwColors.fg300))
            Spacer(Modifier.width(GwSpacing.sp2.dp))
            Text(
                text = soldier.id,
                style = GwTypography.Mono.copy(color = GwColors.fg200, fontSize = 12.sp),
            )
            Spacer(Modifier.width(GwSpacing.sp2.dp))
        }

        // Alert message
        Text(
            text = alert.message,
            style = GwTypography.Mono.copy(color = GwColors.fg000, fontSize = 11.sp),
            modifier = Modifier.weight(1f),
        )

        // Vitals quick view (from soldier)
        if (soldier != null) {
            QuickVital("HR", soldier.hr.toString())
            Spacer(Modifier.width(GwSpacing.sp2.dp))
            QuickVital("RISK", String.format("%.1f", soldier.risk))
            Spacer(Modifier.width(GwSpacing.sp2.dp))

            // T+Xs timer
            val tColor = GwColors.stateLive
            Text(
                text = "T+${alert.triggeredSec}s",
                style = GwTypography.MonoSm.copy(color = tColor),
            )
            Spacer(Modifier.width(GwSpacing.sp3.dp))
        }

        // Action buttons
        AlertActionButton("VIEW", Color(0xFF1A3040), onClick = onView)
        Spacer(Modifier.width(GwSpacing.sp1.dp))
        AlertActionButton("ACK",  Color(0xFF2A1E0A), onClick = onAck)
        Spacer(Modifier.width(GwSpacing.sp1.dp))
        AlertActionButton("CASEVAC", GwColors.critRed, onClick = onCasevac)
        Spacer(Modifier.width(GwSpacing.sp2.dp))
    }
}

@Composable
private fun QuickVital(label: String, value: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(label, style = GwTypography.Audit.copy(color = GwColors.fg300, fontSize = 9.sp))
        Spacer(Modifier.width(2.dp))
        Text(value, style = GwTypography.MonoSm.copy(color = GwColors.fg000))
    }
}

@Composable
private fun AlertActionButton(label: String, bg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(24.dp)
            .clip(RoundedCornerShape(GwRadii.r1.dp))
            .background(bg)
            .border(1.dp, GwColors.strokeStrong, RoundedCornerShape(GwRadii.r1.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = GwTypography.Label.copy(color = GwColors.fg000,
            fontSize = 10.sp))
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
                        color = if (isLast) GwColors.fg000 else GwColors.fg300,
                        fontSize = 12.sp,
                    ),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FOOTER STATUS BAR — less dense
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
            .height(24.dp)
            .background(Color(0xFF080A08))
            .border(width = 1.dp, color = GwColors.strokeHairline)
            .padding(horizontal = GwSpacing.sp3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FootCell("MESH", "$meshOnline/$meshTotal")
        FootCell("LAT", "142ms")
        FootCell("DATA", "2.4 KB/s")
        Spacer(Modifier.weight(1f))
        Text(
            text = lastMsg,
            style = GwTypography.Audit.copy(color = GwColors.fg300, fontSize = 9.sp),
            maxLines = 1,
        )
    }
}

@Composable
private fun FootCell(label: String, value: String) {
    Row(
        modifier = Modifier.padding(end = GwSpacing.sp4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = GwTypography.Audit.copy(color = GwColors.fg300, fontSize = 9.sp))
        Spacer(Modifier.width(3.dp))
        Text(value, style = GwTypography.Audit.copy(color = GwColors.fg100, fontSize = 9.sp))
    }
}

