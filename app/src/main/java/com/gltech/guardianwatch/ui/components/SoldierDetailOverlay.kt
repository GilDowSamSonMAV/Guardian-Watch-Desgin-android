package com.gltech.guardianwatch.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gltech.guardianwatch.model.MeshSignal
import com.gltech.guardianwatch.model.Soldier
import com.gltech.guardianwatch.model.SoldierStatus
import com.gltech.guardianwatch.ui.theme.GwColors
import com.gltech.guardianwatch.ui.theme.GwRadii
import com.gltech.guardianwatch.ui.theme.GwSpacing
import com.gltech.guardianwatch.ui.theme.GwTypography
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Soldier detail panel — full-screen dialog overlay.
 * Matches reference screenshot 4 (soldier-detail.jsx).
 *
 * Layout:
 *   [col 1] VITALS · LIVE — HR waveform, BR waveform, SpO₂/CoreTemp/HSI
 *   [col 2] ASSESSMENT — composite risk bar + factor chips | POSITION
 *   [col 3] GEAR · SENSORS | ACTIONS
 */
@Composable
fun SoldierDetailOverlay(
    soldier: Soldier,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            val accentColor = soldier.status.color

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .fillMaxHeight(0.88f)
                    .clip(RoundedCornerShape(GwRadii.r2.dp))
                    .background(GwColors.bg100)
                    .border(1.5.dp, accentColor, RoundedCornerShape(GwRadii.r2.dp))
                    .clickable { /* consume — don't close */ },
            ) {
                Column {
                    // Header
                    DrawerHeader(soldier, accentColor, onClose)

                    // Body — 3 columns
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(GwSpacing.sp4.dp),
                        horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp4.dp),
                    ) {
                        // COL 1 — Vitals
                        VitalsColumn(soldier, accentColor, Modifier.weight(1f))

                        // COL 2 — Assessment + Position
                        AssessmentColumn(soldier, accentColor, Modifier.weight(1f))

                        // COL 3 — Gear + Actions
                        GearColumn(soldier, Modifier.weight(1f))
                    }

                    // Footer
                    DrawerFooter(soldier)
                }
            }
        }
    }
}

// ─── HEADER ──────────────────────────────────────────────────────────────────

@Composable
private fun DrawerHeader(soldier: Soldier, accentColor: Color, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GwColors.bg200)
            .border(bottom = true, color = GwColors.strokeHairline)
            .padding(horizontal = GwSpacing.sp5.dp, vertical = GwSpacing.sp3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column { Text("עמדה", style = GwTypography.Audit.copy(color = GwColors.fg300, fontSize = 9.sp)) }
        Spacer(Modifier.width(4.dp))
        Text(
            text = soldier.pos.toString().padStart(2, '0'),
            style = GwTypography.MonoXl.copy(color = GwColors.fg200),
        )
        Spacer(Modifier.width(GwSpacing.sp4.dp))

        Column {
            Text(soldier.last, style = GwTypography.H2.copy(color = GwColors.fg000))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(soldier.role.display, style = GwTypography.Mono.copy(color = accentColor))
                Text(" · ", style = GwTypography.Mono.copy(color = GwColors.fg300))
                Text(soldier.id, style = GwTypography.Mono.copy(color = GwColors.fg200))
                Text(" · ", style = GwTypography.Mono.copy(color = GwColors.fg300))
                Text(
                    text = soldier.status.label,
                    style = GwTypography.Mono.copy(color = accentColor),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Close
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
}

// ─── COL 1: VITALS ───────────────────────────────────────────────────────────

@Composable
private fun VitalsColumn(soldier: Soldier, accentColor: Color, modifier: Modifier) {
    val hrWave = remember(soldier.hr) { makeWave(120, soldier.hr / 60f) }
    val brWave = remember(soldier.br) { makeWave(120, soldier.br / 60f, ampScale = 0.4f) }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        SectionLabel("מדדים · חי")

        WaveCard(
            label  = "דופק",
            value  = soldier.hr.toString(),
            unit   = "bpm",
            range  = "60–100",
            wave   = hrWave,
            accent = if (soldier.hr > 140) GwColors.critRed else accentColor,
            hi     = soldier.hr > 140,
        )
        Spacer(Modifier.height(GwSpacing.sp3.dp))

        WaveCard(
            label  = "נשימה",
            value  = soldier.br.toString(),
            unit   = "rpm",
            range  = "12–20",
            wave   = brWave,
            accent = if (soldier.br > 22) GwColors.warnAmber else accentColor,
            hi     = soldier.br > 22,
        )
        Spacer(Modifier.height(GwSpacing.sp3.dp))

        // SpO₂ / CoreTemp / HSI row
        Row(horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp3.dp)) {
            StatBox("SpO₂",
                if (soldier.spo2 > 0) "${soldier.spo2}%" else "--",
                if (soldier.spo2 in 1..91) GwColors.critRed else GwColors.fg000,
                Modifier.weight(1f))
            StatBox("חום גוף",
                if (soldier.coreTemp > 0f) "${String.format("%.1f", soldier.coreTemp)}°C" else "--",
                if (soldier.coreTemp > 38f) GwColors.warnAmber else GwColors.fg000,
                Modifier.weight(1f))
            StatBox("CRI (קריטיות)",
                String.format("%.1f", soldier.risk),
                accentColor,
                Modifier.weight(1f))
        }
    }
}

@Composable
private fun WaveCard(
    label: String, value: String, unit: String,
    range: String, wave: List<Offset>,
    accent: Color, hi: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GwRadii.r2.dp))
            .background(if (hi) GwColors.critRedBg else GwColors.bg200)
            .border(1.dp, if (hi) GwColors.critRed else GwColors.strokeHairline,
                RoundedCornerShape(GwRadii.r2.dp))
            .padding(GwSpacing.sp3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(label, style = GwTypography.Label.copy(color = GwColors.fg200))
                Text("תקין $range", style = GwTypography.Audit.copy(color = GwColors.fg400))
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = GwTypography.MonoLg.copy(color = accent))
                Spacer(Modifier.width(3.dp))
                Text(unit, style = GwTypography.Audit.copy(color = GwColors.fg300))
            }
        }
        Spacer(Modifier.height(GwSpacing.sp2.dp))
        // Waveform
        Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
            if (wave.size < 2) return@Canvas
            val w = size.width
            val h = size.height
            val xStep = w / (wave.size - 1)
            val yRange = wave.maxOf { it.y } - wave.minOf { it.y }
            val yMin = wave.minOf { it.y }
            val path = Path()
            wave.forEachIndexed { i, pt ->
                val x = i * xStep
                val y = if (yRange > 0f) h - (pt.y - yMin) / yRange * h * 0.85f else h / 2f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, accent, style = Stroke(width = 1.2.dp.toPx()))
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GwRadii.r2.dp))
            .background(GwColors.bg200)
            .border(1.dp, GwColors.strokeHairline, RoundedCornerShape(GwRadii.r2.dp))
            .padding(GwSpacing.sp3.dp),
    ) {
        Text(label, style = GwTypography.Audit.copy(color = GwColors.fg300))
        Spacer(Modifier.height(3.dp))
        Text(value, style = GwTypography.MonoLg.copy(color = valueColor))
    }
}

// ─── COL 2: ASSESSMENT + POSITION ────────────────────────────────────────────

@Composable
private fun AssessmentColumn(soldier: Soldier, accentColor: Color, modifier: Modifier) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        SectionLabel("הערכת מצב")

        // Risk card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GwRadii.r2.dp))
                .background(GwColors.bg200)
                .border(1.dp, GwColors.strokeHairline, RoundedCornerShape(GwRadii.r2.dp))
                .padding(GwSpacing.sp4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("מדד קריטיות (CRI)", style = GwTypography.Label.copy(color = GwColors.fg300))
                Spacer(Modifier.weight(1f))
                Text(
                    String.format("%.1f", soldier.risk),
                    style = GwTypography.H1.copy(color = accentColor),
                )
                Text("/10", style = GwTypography.Mono.copy(color = GwColors.fg300))
            }
            Spacer(Modifier.height(GwSpacing.sp2.dp))
            // Detailed risk bar with tick marks
            Box(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(GwColors.bg400),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((soldier.risk / 10f).coerceIn(0f, 1f))
                            .background(accentColor, RoundedCornerShape(4.dp)),
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            // Scale labels
            Row {
                Text("0", style = GwTypography.Audit.copy(color = GwColors.fg300))
                Spacer(Modifier.weight(1f))
                Text("תקין", style = GwTypography.Audit.copy(color = SoldierStatus.OK.color))
                Spacer(Modifier.weight(1f))
                Text("3", style = GwTypography.Audit.copy(color = SoldierStatus.CAUTION.color))
                Spacer(Modifier.weight(1f))
                Text("6", style = GwTypography.Audit.copy(color = SoldierStatus.HIGH.color))
                Spacer(Modifier.weight(1f))
                Text("8", style = GwTypography.Audit.copy(color = SoldierStatus.CRITICAL.color))
                Spacer(Modifier.weight(1f))
                Text("10", style = GwTypography.Audit.copy(color = GwColors.fg300))
            }
            Spacer(Modifier.height(GwSpacing.sp3.dp))
            // Factor chips
            if (soldier.hr > 140) FactorChip("↑ טכיקרדיה (${soldier.hr})", accentColor)
            if (soldier.br > 22)  FactorChip("↑ טכיפנאה (${soldier.br})", accentColor)
            if (soldier.spo2 in 1..91) FactorChip("↓ היפוקסיה (${soldier.spo2}%)", accentColor)
            if (soldier.coreTemp > 38f) FactorChip(
                "↑ חום גבוה (${String.format("%.1f", soldier.coreTemp)}°)", accentColor)
            if (soldier.risk < 3f) FactorChip("● בטווח התקין", SoldierStatus.OK.color)
        }

        Spacer(Modifier.height(GwSpacing.sp4.dp))
        SectionLabel("מיקום")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GwRadii.r2.dp))
                .background(GwColors.bg200)
                .border(1.dp, GwColors.strokeHairline, RoundedCornerShape(GwRadii.r2.dp))
                .padding(GwSpacing.sp4.dp),
        ) {
            Text("18S TJ 4486 8741",
                style = GwTypography.MonoLg.copy(color = GwColors.fg000))
            Spacer(Modifier.height(GwSpacing.sp3.dp))
            PosRow("רוחב", "33.2871° N")
            PosRow("אורך", "35.5612° E")
            PosRow("גובה", "482 מטר")
            PosRow("מרחק חפ״ק", "2.4 ק״מ 047°")
        }
    }
}

@Composable
private fun FactorChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = GwSpacing.sp1.dp)
            .clip(RoundedCornerShape(GwRadii.r1.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(GwRadii.r1.dp))
            .padding(horizontal = GwSpacing.sp2.dp, vertical = GwSpacing.sp1.dp),
    ) {
        Text(text, style = GwTypography.MonoSm.copy(color = color))
    }
}

@Composable
private fun PosRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = GwSpacing.sp1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = GwTypography.Label.copy(color = GwColors.fg300),
            modifier = Modifier.width(80.dp))
        Text(value, style = GwTypography.Mono.copy(color = GwColors.fg000))
    }
}

// ─── COL 3: GEAR + ACTIONS ───────────────────────────────────────────────────

@Composable
private fun GearColumn(soldier: Soldier, modifier: Modifier) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        SectionLabel("ציוד וחיישנים")

        GearRow("שעון חכם",    "GARMIN INSTINCT",
            SoldierStatus.OK.color)
        GearRow("סוללה",     "${soldier.batteryPct}%",
            if (soldier.batteryPct > 30) SoldierStatus.OK.color else SoldierStatus.CAUTION.color)
        GearRow("רשת קשר",  when (soldier.meshSignal) {
            MeshSignal.STRONG -> "חזק"
            MeshSignal.WEAK   -> "חלש"
            MeshSignal.NONE   -> "אין אות"
        }, when (soldier.meshSignal) {
            MeshSignal.STRONG -> SoldierStatus.OK.color
            else              -> SoldierStatus.CAUTION.color
        })
        GearRow("GPS",         "HI-PRECISION", SoldierStatus.OK.color)
        GearRow("IFAK",
            if (soldier.status == SoldierStatus.CRITICAL) "בשימוש · ח.ע 1" else "מוכן",
            if (soldier.status == SoldierStatus.CRITICAL) SoldierStatus.CAUTION.color
            else SoldierStatus.OK.color)

        Spacer(Modifier.height(GwSpacing.sp4.dp))
        SectionLabel("פעולות")

        Row(horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp3.dp)) {
            ActionButton("קשר",      GwColors.bg300, GwColors.strokeDefault, GwColors.fg000,
                Modifier.weight(1f)) {}
            ActionButton("פינג",     GwColors.bg300, GwColors.strokeDefault, GwColors.fg000,
                Modifier.weight(1f)) {}
        }
        Spacer(Modifier.height(GwSpacing.sp3.dp))
        ActionButton("היסטוריה", GwColors.bg300, GwColors.strokeDefault, GwColors.fg000,
            Modifier.fillMaxWidth()) {}
    }
}

@Composable
private fun GearRow(label: String, value: String, dotColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = GwSpacing.sp2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(androidx.compose.foundation.shape.CircleShape)
            .background(dotColor))
        Spacer(Modifier.width(GwSpacing.sp3.dp))
        Text(label, style = GwTypography.Label.copy(color = GwColors.fg300),
            modifier = Modifier.width(100.dp))
        Spacer(Modifier.weight(1f))
        Text(value, style = GwTypography.Mono.copy(color = GwColors.fg000))
    }
}

@Composable
private fun ActionButton(
    label: String, bg: Color, border: Color, fg: Color,
    modifier: Modifier, onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(GwSpacing.sp7.dp)
            .clip(RoundedCornerShape(GwRadii.r1.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(GwRadii.r1.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = GwTypography.Label.copy(color = fg))
    }
}

// ─── FOOTER ──────────────────────────────────────────────────────────────────

@Composable
private fun DrawerFooter(soldier: Soldier) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GwColors.bg200)
            .border(top = true, color = GwColors.strokeHairline)
            .padding(horizontal = GwSpacing.sp5.dp, vertical = GwSpacing.sp2.dp),
        horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp5.dp),
    ) {
        Text("● עדכון אחרון T+${soldier.lastUpdateSec}s",
            style = GwTypography.Audit.copy(color = GwColors.fg300))
        Text("● טיפול אוטומטי זמין",
            style = GwTypography.Audit.copy(color = GwColors.fg300))
        Text("● עדיפות פינוי: P1",
            style = GwTypography.Audit.copy(color = GwColors.fg300))
    }
}

// ─── HELPERS ─────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text  = text,
        style = GwTypography.Label.copy(color = GwColors.fg300),
        modifier = Modifier.padding(bottom = GwSpacing.sp2.dp),
    )
}

private fun makeWave(samples: Int, freq: Float, ampScale: Float = 1.0f): List<Offset> {
    val rng = Random(freq.toBits())
    val pts = mutableListOf<Offset>()
    for (i in 0 until samples) {
        val sine  = sin(i * freq * 0.3f) * 10f * ampScale
        val spike = if (i % maxOf(6, (60f / freq).toInt()) == 0) {
            -(8f * ampScale * (0.7f + rng.nextFloat() * 0.3f))
        } else 0f
        val noise = (rng.nextFloat() - 0.5f) * 3f * ampScale
        pts.add(Offset(i.toFloat(), 14f + sine + spike + noise))
    }
    return pts
}

// Extension helpers for border-on-side simulation
@Composable
private fun Modifier.border(top: Boolean = false, bottom: Boolean = false, color: Color): Modifier {
    return if (top || bottom) {
        this.border(
            width = 1.dp,
            color = color,
            shape = RoundedCornerShape(0.dp),
        )
    } else this
}
