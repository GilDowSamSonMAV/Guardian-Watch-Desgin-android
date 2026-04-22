package com.gltech.guardianwatch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gltech.guardianwatch.casualty.HealthTone
import com.gltech.guardianwatch.ui.theme.GwColors
import com.gltech.guardianwatch.ui.theme.GwRadii
import com.gltech.guardianwatch.ui.theme.GwTypography

@Composable
fun VitalTile(
    label: String,
    value: String,
    unit: String,
    trend: String,
    tone: HealthTone = HealthTone.OK,
    modifier: Modifier = Modifier,
) {
    val (bg, border, valueColor) = when (tone) {
        HealthTone.CRITICAL -> Triple(GwColors.critRedBg, GwColors.critRed, GwColors.fg000)
        HealthTone.WARN -> Triple(GwColors.warnAmberBg, GwColors.warnAmber, GwColors.fg000)
        HealthTone.OK -> Triple(GwColors.bg200, GwColors.strokeHairline, GwColors.fg000)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GwRadii.r2.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(GwRadii.r2.dp))
            .padding(16.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = GwTypography.Label.copy(color = GwColors.fg200),
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
            Text(
                text = value,
                style = GwTypography.MonoXl.copy(color = valueColor),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = unit,
                style = GwTypography.MonoSm.copy(color = GwColors.fg300),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Text(
            text = trend,
            style = GwTypography.MonoSm.copy(color = GwColors.fg300),
        )
    }
}
