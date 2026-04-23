package com.gltech.guardianwatch.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gltech.guardianwatch.casualty.HealthTone
import com.gltech.guardianwatch.ui.theme.GwColors
import com.gltech.guardianwatch.ui.theme.GwRadii
import com.gltech.guardianwatch.ui.theme.GwSpacing
import com.gltech.guardianwatch.ui.theme.GwTypography

/**
 * Streaming biometric chart.
 *
 * Input: a list of (timestampMs, value) samples, already downsampled by the
 * VitalsRepository (~10 min window at up to 1 Hz = ~600 points — well within Canvas).
 *
 * Draws hairline grid, then a single polyline in the tone color.
 * No anti-alias on strokes — per design system: "square caps, miter joins".
 */
@Composable
fun BiometricChart(
    title: String,
    latestValue: String,
    unit: String,
    tone: HealthTone,
    samples: List<Pair<Long, Int>>,
    yMin: Int = 40,
    yMax: Int = 200,
    timeLabel: String = "",
    modifier: Modifier = Modifier,
) {
    val traceColor = when (tone) {
        HealthTone.CRITICAL -> GwColors.critRed
        HealthTone.WARN -> GwColors.warnAmber
        HealthTone.OK -> GwColors.stateLive
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GwRadii.r2.dp))
            .background(GwColors.bg100)
            .border(1.dp, GwColors.strokeHairline, RoundedCornerShape(GwRadii.r2.dp))
            .padding(GwSpacing.sp3.dp),
    ) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title.uppercase(),
                style = GwTypography.Label.copy(color = GwColors.fg200),
            )
            Spacer(Modifier.width(GwSpacing.sp3.dp))
            Text(
                text = latestValue,
                style = GwTypography.MonoLg.copy(color = traceColor),
            )
            Spacer(Modifier.width(GwSpacing.sp1.dp))
            Text(
                text = unit,
                style = GwTypography.MonoSm.copy(color = GwColors.fg300),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = timeLabel,
                style = GwTypography.MonoSm.copy(color = GwColors.fg300),
            )
        }
        Spacer(Modifier.height(GwSpacing.sp2.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
        ) {
            val w = size.width
            val h = size.height
            // Grid (3 horizontal hairlines)
            val grid = GwColors.strokeHairline
            for (i in 1..3) {
                val y = h * i / 4f
                drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 0.5f)
            }
            if (samples.size < 2) return@Canvas

            val tStart = samples.first().first
            val tEnd = samples.last().first
            val tRange = (tEnd - tStart).coerceAtLeast(1)
            val vRange = (yMax - yMin).coerceAtLeast(1)

            val path = Path()
            samples.forEachIndexed { i, (t, v) ->
                val x = w * (t - tStart) / tRange.toFloat()
                val clampedV = v.coerceIn(yMin, yMax)
                val y = h - (h * (clampedV - yMin) / vRange.toFloat())
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = traceColor,
                style = Stroke(
                    width = 2f,
                    cap = StrokeCap.Square,
                    join = StrokeJoin.Miter,
                ),
            )
        }
    }
}
