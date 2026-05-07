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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gltech.guardianwatch.model.Company
import com.gltech.guardianwatch.model.Platoon
import com.gltech.guardianwatch.model.Soldier
import com.gltech.guardianwatch.model.SoldierStatus
import com.gltech.guardianwatch.model.Squad
import com.gltech.guardianwatch.ui.theme.GwColors
import com.gltech.guardianwatch.ui.theme.GwRadii
import com.gltech.guardianwatch.ui.theme.GwSpacing
import com.gltech.guardianwatch.ui.theme.GwTypography
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tactical map panel — right half of the dashboard.
 * Renders a dark map canvas with:
 *   - Dot-grid background
 *   - Colored circle per soldier, grouped by platoon sector
 *   - HOTZONE polygon (dashed red)
 *   - LZ-ALPHA marker (square H)
 *   - Map controls (TERRAIN / SAT / IR toggle chips)
 */
@Composable
fun TacMap(
    company: Company,
    activeSquadId: String?,
    alertSoldierId: String?,
    onPinClicked: (Soldier) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Fixed pseudorandom positions for demo (seeded so they're stable)
    val rng = Random(1337)

    // Build position map: soldierId → (normalizedX, normalizedY) 0..1
    val positions = buildSoldierPositions(company, rng)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090E09)),
    ) {
        // Map controls top-right
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(GwSpacing.sp3.dp),
            horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp2.dp),
        ) {
            MapToggle("TERRAIN", false)
            MapToggle("SAT",     true)
            MapToggle("IR",      false)
        }

        // Map label top-left
        Column(modifier = Modifier.align(Alignment.TopStart).padding(GwSpacing.sp3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TAC-MAP", style = GwTypography.Audit.copy(color = GwColors.fg300))
                Spacer(Modifier.width(6.dp))
                Text("18S TJ 4486 8741", style = GwTypography.Audit.copy(color = GwColors.fg200))
            }
        }

        // Canvas map
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Grid
            val gridColor = Color(0xFF161E16)
            val gridStep = 40.dp.toPx()
            var gx = 0f
            while (gx < w) { drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), 0.5f); gx += gridStep }
            var gy = 0f
            while (gy < h) { drawLine(gridColor, Offset(0f, gy), Offset(w, gy), 0.5f); gy += gridStep }

            // HOTZONE polygon (top-right area)
            val hzPoints = listOf(
                Offset(w * 0.72f, h * 0.08f),
                Offset(w * 0.88f, h * 0.12f),
                Offset(w * 0.92f, h * 0.30f),
                Offset(w * 0.82f, h * 0.38f),
                Offset(w * 0.70f, h * 0.28f),
                Offset(w * 0.68f, h * 0.15f),
            )
            val hzPath = androidx.compose.ui.graphics.Path().apply {
                hzPoints.forEachIndexed { i, pt ->
                    if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
                close()
            }
            drawPath(hzPath, Color(0xFFFF2D38).copy(alpha = 0.08f))
            drawPath(hzPath, Color(0xFFFF2D38), style = Stroke(width = 1.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(8f, 6f))))

            // Curved terrain lines (suggest topography)
            val tColor = Color(0xFF1C291C)
            for (i in 0..4) {
                val yFrac = 0.2f + i * 0.15f
                val path2 = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, h * yFrac)
                    cubicTo(w * 0.25f, h * (yFrac - 0.06f),
                        w * 0.6f, h * (yFrac + 0.04f),
                        w, h * (yFrac - 0.02f))
                }
                drawPath(path2, tColor, style = Stroke(width = 0.8.dp.toPx()))
            }

            // Soldier dots
            positions.forEach { (soldierId, pos) ->
                val x = pos.first  * w
                val y = pos.second * h
                val soldier = findSoldier(company, soldierId) ?: return@forEach
                val dotColor = soldier.status.color
                val isAlert  = soldierId == alertSoldierId
                val radius   = if (isAlert) 7.dp.toPx() else 5.dp.toPx()

                // Outer ring for alert soldier
                if (isAlert) {
                    drawCircle(dotColor.copy(alpha = 0.3f), radius * 2, Offset(x, y))
                    drawCircle(dotColor, radius, Offset(x, y), style = Stroke(1.5.dp.toPx()))
                }
                drawCircle(dotColor, radius, Offset(x, y))
            }
        }

        // LZ-ALPHA marker — bottom-centre-left
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp, start = 0.dp)
                .offset(x = (-60).dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, GwColors.fg200)
                        .background(GwColors.bg200),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("H", style = GwTypography.H2.copy(color = GwColors.fg000))
                }
                Text("LZ-ALPHA", style = GwTypography.Audit.copy(color = GwColors.fg300))
            }
        }

        // HOTZONE label
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 60.dp),
        ) {
            Text("HOTZONE-1",
                style = GwTypography.Audit.copy(color = GwColors.critRed.copy(alpha = 0.7f)))
        }

        // Scale bar — bottom right
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(GwSpacing.sp3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.width(60.dp).height(8.dp)) {
                drawLine(Color(0xFF9B9893), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1f)
                drawLine(Color(0xFF9B9893), Offset(0f, 0f), Offset(0f, size.height), 1f)
                drawLine(Color(0xFF9B9893), Offset(size.width, 0f), Offset(size.width, size.height), 1f)
            }
            Spacer(Modifier.width(3.dp))
            Text("500m", style = GwTypography.Audit.copy(color = GwColors.fg300))
        }

        // Legend — top left below TAC-MAP label
        LegendRow(modifier = Modifier.align(Alignment.TopStart).padding(top = 36.dp, start = GwSpacing.sp3.dp))
    }
}

@Composable
private fun LegendRow(modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        val items = listOf(
            "OK"   to SoldierStatus.OK.color,
            "CAUT" to SoldierStatus.CAUTION.color,
            "HIGH" to SoldierStatus.HIGH.color,
            "CRIT" to SoldierStatus.CRITICAL.color,
            "OFF"  to SoldierStatus.OFFLINE.color,
        )
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .background(color))
                Spacer(Modifier.width(2.dp))
                Text(label, style = GwTypography.Audit.copy(color = GwColors.fg300, fontSize = 9.sp))
            }
        }
    }
}

@Composable
private fun MapToggle(label: String, active: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(GwRadii.r1.dp))
            .background(if (active) GwColors.infoCyan.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, if (active) GwColors.infoCyan else GwColors.strokeHairline,
                RoundedCornerShape(GwRadii.r1.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, style = GwTypography.Audit.copy(
            color = if (active) GwColors.infoCyan else GwColors.fg300))
    }
}

private fun buildSoldierPositions(company: Company, rng: Random): Map<String, Pair<Float, Float>> {
    val result = mutableMapOf<String, Pair<Float, Float>>()
    // Each platoon has a cluster centre
    val centres = listOf(
        0.30f to 0.60f,
        0.50f to 0.55f,
        0.40f to 0.75f,
    )
    company.platoons.forEachIndexed { pi, platoon ->
        val (cx, cy) = centres[pi]
        platoon.squads.forEach { squad ->
            squad.soldiers.forEach { soldier ->
                val angle  = rng.nextFloat() * 2 * Math.PI.toFloat()
                val radius = rng.nextFloat() * 0.12f + 0.03f
                val x = (cx + cos(angle) * radius).coerceIn(0.05f, 0.95f)
                val y = (cy + sin(angle) * radius).coerceIn(0.1f,  0.9f)
                result[soldier.id] = x to y
            }
        }
    }
    return result
}

private fun findSoldier(company: Company, id: String): Soldier? =
    company.platoons.flatMap { it.squads }.flatMap { it.soldiers }.firstOrNull { it.id == id }
