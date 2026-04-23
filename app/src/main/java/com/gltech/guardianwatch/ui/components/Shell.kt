package com.gltech.guardianwatch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gltech.guardianwatch.ui.theme.GwColors
import com.gltech.guardianwatch.ui.theme.GwRadii
import com.gltech.guardianwatch.ui.theme.GwSpacing
import com.gltech.guardianwatch.ui.theme.GwTypography

/** Top status bar with mission label, self-ID, BLE strength, battery, clock. */
@Composable
fun TopBar(
    missionLabel: String,
    selfId: String,
    bleRssi: Int,
    batteryPct: Int,
    timeStr: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(GwColors.bg100)
            .border(1.dp, GwColors.strokeHairline),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(GwSpacing.sp3.dp))
        // Brand monogram
        BrandMonogram(size = 20)
        Spacer(Modifier.width(GwSpacing.sp4.dp))
        LabeledValue("Mission", missionLabel)
        Spacer(Modifier.width(GwSpacing.sp4.dp))
        LabeledValue("Self", selfId)
        Spacer(Modifier.weight(1f))
        LabeledValue("BLE", "$bleRssi dBm", labelColor = GwColors.stateLive)
        Spacer(Modifier.width(GwSpacing.sp3.dp))
        LabeledValue("Batt", "$batteryPct%")
        Spacer(Modifier.width(GwSpacing.sp4.dp))
        Text(
            text = timeStr,
            style = GwTypography.Mono.copy(color = GwColors.fg000),
        )
        Spacer(Modifier.width(GwSpacing.sp3.dp))
    }
}

@Composable
private fun LabeledValue(label: String, value: String, labelColor: Color = GwColors.fg200) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label.uppercase(),
            style = GwTypography.Label.copy(color = labelColor),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = value,
            style = GwTypography.Mono.copy(color = GwColors.fg000),
        )
    }
}

/** Left navigation rail — icon-only tap targets (48dp enforced). */
@Composable
fun LeftNav(
    selected: NavItem,
    onSelect: (NavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(56.dp)
            .fillMaxHeight()
            .background(GwColors.bg100)
            .border(1.dp, GwColors.strokeHairline),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(GwSpacing.sp3.dp))
        NavItem.entries.forEach { item ->
            NavButton(
                label = item.label,
                selected = item == selected,
                onClick = { onSelect(item) },
            )
            Spacer(Modifier.height(GwSpacing.sp2.dp))
        }
    }
}

enum class NavItem(val label: String) {
    CASUALTIES("CAS"),
    SQUAD("SQD"),
    MAP("MAP"),
    AUDIT("AUD"),
}

@Composable
private fun NavButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) GwColors.chromeOlive500 else Color.Transparent
    val fg = if (selected) GwColors.fg000 else GwColors.fg200
    Box(
        modifier = Modifier
            .size(GwSpacing.sp7.dp)
            .clip(RoundedCornerShape(GwRadii.r1.dp))
            .background(bg)
            .border(1.dp, GwColors.strokeHairline, RoundedCornerShape(GwRadii.r1.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = GwTypography.MonoSm.copy(color = fg))
    }
}

/** Brand monogram — nested triangles in fg/bg. */
@Composable
fun BrandMonogram(size: Int = 24) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size.dp)) {
        val s = this.size.minDimension
        fun triangle(scale: Float, color: Color) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(s / 2f, s * (0.125f + (1 - scale) * 0.3f))
                lineTo(s * (0.875f - (1 - scale) * 0.3f), s * (0.8125f - (1 - scale) * 0.1f))
                lineTo(s * (0.125f + (1 - scale) * 0.3f), s * (0.8125f - (1 - scale) * 0.1f))
                close()
            }
            drawPath(path, color)
        }
        triangle(1.0f, GwColors.fg000)
        triangle(0.65f, GwColors.bg100)
        triangle(0.30f, GwColors.fg000)
    }
}
