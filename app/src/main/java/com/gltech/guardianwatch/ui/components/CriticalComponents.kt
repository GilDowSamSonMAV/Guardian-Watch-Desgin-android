package com.gltech.guardianwatch.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gltech.guardianwatch.casualty.Triage
import com.gltech.guardianwatch.ui.theme.GwColors
import com.gltech.guardianwatch.ui.theme.GwRadii
import com.gltech.guardianwatch.ui.theme.GwSpacing
import com.gltech.guardianwatch.ui.theme.GwTypography

/** Critical alert banner — red rail, title, body, ACKNOWLEDGE button. */
@Composable
fun CriticalBanner(
    title: String,
    body: String,
    onAck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GwRadii.r2.dp))
            .background(GwColors.critRedBg)
            .border(1.dp, GwColors.critRed, RoundedCornerShape(GwRadii.r2.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(GwColors.critRed),
        )
        Column(modifier = Modifier.weight(1f).padding(GwSpacing.sp4.dp)) {
            Text(
                text = title.uppercase(),
                style = GwTypography.H2.copy(color = GwColors.fg000),
            )
            Spacer(Modifier.height(GwSpacing.sp1.dp))
            Text(
                text = body,
                style = GwTypography.Mono.copy(color = GwColors.fg100),
            )
        }
        Box(
            modifier = Modifier
                .height(GwSpacing.sp7.dp)
                .padding(end = GwSpacing.sp4.dp)
                .clip(RoundedCornerShape(GwRadii.r1.dp))
                .background(GwColors.critRed)
                .clickable { onAck() }
                .padding(horizontal = GwSpacing.sp5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "ACKNOWLEDGE",
                style = GwTypography.Label.copy(color = GwColors.fg000),
            )
        }
    }
}

/** Confirm bar — two-tap destructive action (Cancel / Confirm). */
@Composable
fun ConfirmBar(
    label: String,
    target: String,
    confirmText: String = "Confirm Handoff",
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GwRadii.r2.dp))
            .background(GwColors.bg400)
            .border(1.dp, GwColors.strokeStrong, RoundedCornerShape(GwRadii.r2.dp))
            .padding(horizontal = GwSpacing.sp4.dp, vertical = GwSpacing.sp3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = buildString {
                append(label)
                append("  ")
                append(target)
            },
            style = GwTypography.Mono.copy(color = GwColors.fg000),
            modifier = Modifier.weight(1f),
        )
        GhostButton("Cancel", onClick = onCancel)
        Spacer(Modifier.width(GwSpacing.sp2.dp))
        PrimaryButton(confirmText, onClick = onConfirm)
    }
}

@Composable
fun PrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(GwSpacing.sp7.dp)
            .clip(RoundedCornerShape(GwRadii.r1.dp))
            .background(GwColors.chromeOlive500)
            .border(1.dp, GwColors.strokeStrong, RoundedCornerShape(GwRadii.r1.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            style = GwTypography.Label.copy(color = GwColors.fg000),
        )
    }
}

@Composable
fun GhostButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(GwSpacing.sp7.dp)
            .clip(RoundedCornerShape(GwRadii.r1.dp))
            .background(Color.Transparent)
            .border(1.dp, GwColors.strokeDefault, RoundedCornerShape(GwRadii.r1.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            style = GwTypography.Label.copy(color = GwColors.fg200),
        )
    }
}

/** Casualty header — triage dot + ID + name + MGRS + chain summary. */
@Composable
fun CasualtyHeader(
    id: String,
    name: String,
    age: Int,
    mgrs: String,
    chainSummary: String,
    triage: Triage,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriageGlyph(triage, size = 40)
        Spacer(Modifier.width(GwSpacing.sp3.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = id,
                    style = GwTypography.MonoLg.copy(color = GwColors.fg000),
                )
                Spacer(Modifier.width(GwSpacing.sp3.dp))
                TriageChip(triage)
            }
            Spacer(Modifier.height(GwSpacing.sp1.dp))
            Text(
                text = "$name · $age yr",
                style = GwTypography.Body.copy(color = GwColors.fg100),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(mgrs, style = GwTypography.MonoSm.copy(color = GwColors.fg200))
            Text(chainSummary, style = GwTypography.MonoSm.copy(color = GwColors.fg300))
        }
    }
}

@Composable
fun TriageGlyph(triage: Triage, size: Int = 24) {
    val (color, shape) = when (triage) {
        Triage.IMMEDIATE -> GwColors.triageImmediate to RoundedCornerShape(0.dp)  // square
        Triage.DELAYED -> GwColors.triageDelayed to RoundedCornerShape(0.dp)      // triangle (approx)
        Triage.MINOR -> GwColors.triageMinor to RoundedCornerShape(GwSpacing.sp1.dp)          // rounded
        Triage.EXPECTANT -> GwColors.triageExpectant to RoundedCornerShape(size.dp / 2)  // circle
    }
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(shape)
            .background(color)
            .border(1.dp, GwColors.strokeStrong, shape),
    )
}

@Composable
fun TriageChip(triage: Triage) {
    val (bg, fg, label) = when (triage) {
        Triage.IMMEDIATE -> Triple(GwColors.triageImmediateDim, GwColors.fg000, "IMMEDIATE")
        Triage.DELAYED -> Triple(GwColors.triageDelayedDim, GwColors.fg000, "DELAYED")
        Triage.MINOR -> Triple(GwColors.triageMinorDim, GwColors.fg000, "MINOR")
        Triage.EXPECTANT -> Triple(GwColors.triageExpectant, GwColors.triageExpectantFg, "EXPECTANT")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(GwRadii.r1.dp))
            .background(bg)
            .padding(horizontal = GwSpacing.sp2.dp, vertical = GwSpacing.sp1.dp),
    ) {
        Text(label, style = GwTypography.Label.copy(color = fg))
    }
}

/** Evac timeline — POI → Medic → CASEVAC → Role-2 → Role-3. */
@Composable
fun EvacTimeline(modifier: Modifier = Modifier) {
    val nodes = listOf(
        TimelineNode("POI", "Point of Injury", "14:22:06Z", NodeState.DONE),
        TimelineNode("MED", "Medic", "+02:14", NodeState.DONE),
        TimelineNode("EVC", "CASEVAC", "+06:32 · now", NodeState.NOW),
        TimelineNode("R2", "Role-2", "ETA +18:00", NodeState.PENDING),
        TimelineNode("R3", "Role-3", "—", NodeState.PENDING),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(GwColors.bg100)
            .border(1.dp, GwColors.strokeHairline)
            .padding(GwSpacing.sp4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        nodes.forEach { TimelineDot(it) }
    }
}

private data class TimelineNode(val k: String, val label: String, val time: String, val state: NodeState)
private enum class NodeState { DONE, NOW, PENDING }

@Composable
private fun TimelineDot(node: TimelineNode) {
    val (dotBg, dotFg, labelColor) = when (node.state) {
        NodeState.DONE -> Triple(GwColors.chromeOlive500, GwColors.fg000, GwColors.fg100)
        NodeState.NOW -> Triple(GwColors.stateLive, GwColors.bg000, GwColors.fg000)
        NodeState.PENDING -> Triple(GwColors.bg300, GwColors.fg300, GwColors.fg300)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(GwRadii.r1.dp))
                .background(dotBg)
                .border(1.dp, GwColors.strokeStrong, RoundedCornerShape(GwRadii.r1.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(node.k, style = GwTypography.Label.copy(color = dotFg))
        }
        Spacer(Modifier.height(6.dp))
        Text(node.label, style = GwTypography.MonoSm.copy(color = labelColor))
        Text(node.time, style = GwTypography.Audit.copy(color = GwColors.fg300))
    }
}
