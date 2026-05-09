package com.gltech.guardianwatch.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gltech.guardianwatch.model.Company
import com.gltech.guardianwatch.model.DemoData
import com.gltech.guardianwatch.model.Platoon
import com.gltech.guardianwatch.model.Soldier
import com.gltech.guardianwatch.model.SoldierRole
import com.gltech.guardianwatch.model.SoldierStatus
import com.gltech.guardianwatch.model.Squad
import com.gltech.guardianwatch.model.SimulationEngine
import com.gltech.guardianwatch.ui.components.SoldierCard
import com.gltech.guardianwatch.ui.theme.GwColors
import com.gltech.guardianwatch.ui.theme.GwRadii
import com.gltech.guardianwatch.ui.theme.GwSpacing
import com.gltech.guardianwatch.ui.theme.GwTypography
import androidx.compose.ui.res.stringResource
import com.gltech.guardianwatch.R

// ──────────────────────────────────────────────────────────────────────────────
// Helper: apply live simulation vitals to a soldier
// ──────────────────────────────────────────────────────────────────────────────

private fun effectiveSoldier(s: Soldier, simulation: SimulationEngine?): Soldier {
    if (simulation == null || !simulation.isRunning) return s
    val lv = simulation.liveVitals[s.id] ?: return s
    return s.copy(
        hr = lv.hr,
        br = lv.br,
        spo2 = lv.spo2,
        coreTemp = lv.coreTemp,
        risk = lv.risk,
        status = lv.status,
        lastUpdateSec = lv.lastUpdateSec,
        batteryPct = lv.batteryPct,
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// COMPANY VIEW — 3 platoon cards side-by-side
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CompanyView(
    company: Company,
    onPickPlatoon: (String) -> Unit,
    simulation: SimulationEngine? = null,
    modifier: Modifier = Modifier,
) {
    val allSoldiers = company.platoons.flatMap { p -> p.squads.flatMap { s -> s.soldiers } }
        .map { effectiveSoldier(it, simulation) }
    val counts = statusCounts(allSoldiers)

    Column(modifier = modifier.fillMaxSize()) {
        ViewMeta(
            label  = stringResource(R.string.company_overview),
            sub    = stringResource(R.string.platoons_count, company.platoons.size),
            total  = allSoldiers.size,
            counts = counts,
        )
        Spacer(Modifier.height(GwSpacing.sp3.dp))
        // Platoon cards — wrap in rows of 3
        val rows = company.platoons.chunked(3)
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { platoon ->
                    PlatoonCard(
                        platoon    = platoon,
                        onClick    = { onPickPlatoon(platoon.id) },
                        simulation = simulation,
                        // ★ NO fillMaxHeight — card height = content height only
                        modifier   = Modifier.weight(1f),
                    )
                }
                // pad last row if fewer than 3
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            if (rows.size > 1) Spacer(Modifier.height(GwSpacing.sp4.dp))
        }
    }
}

@Composable
private fun PlatoonCard(
    platoon: Platoon, onClick: () -> Unit,
    simulation: SimulationEngine?,
    modifier: Modifier,
) {
    val soldiers    = platoon.squads.flatMap { it.soldiers }.map { effectiveSoldier(it, simulation) }
    val hasAlert    = soldiers.any { it.status == SoldierStatus.CRITICAL }
    val borderColor = if (hasAlert) GwColors.critRed else GwColors.strokeHairline
    val counts      = statusCounts(soldiers)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GwRadii.r2.dp))
            .background(GwColors.bg200)
            .border(if (hasAlert) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(GwRadii.r2.dp))
            .clickable { onClick() }
            .padding(GwSpacing.sp4.dp),
        // ★ wrapContentHeight — border only wraps actual content
    ) {
        // ── Header ───────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text      = platoon.callsign,
                    style     = GwTypography.Audit.copy(color = GwColors.fg300),
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                )
                Text(
                    text      = platoon.name,
                    style     = GwTypography.Label.copy(color = GwColors.fg000, fontSize = 16.sp),
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${soldiers.size}",
                style = GwTypography.MonoLg.copy(color = GwColors.fg200),
            )
        }

        Spacer(Modifier.height(GwSpacing.sp2.dp))

        // ── Soldier tile grid ─────────────────────
        platoon.squads.forEach { squad ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                squad.soldiers.forEach { s ->
                    val eff = effectiveSoldier(s, simulation)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(eff.status.color.copy(alpha = 0.85f))
                    )
                }
            }
        }

        Spacer(Modifier.height(GwSpacing.sp2.dp))

        // ── Footer: counts + open hint ───────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusCountRow(counts)
            Spacer(Modifier.weight(1f))
            Text(
                "${stringResource(R.string.open_action)} ◀",
                style = GwTypography.Audit.copy(color = GwColors.infoCyan),
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// PLATOON VIEW — 3 squad cards side-by-side
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun PlatoonView(
    platoon: Platoon,
    onPickSquad: (String) -> Unit,
    simulation: SimulationEngine? = null,
    modifier: Modifier = Modifier,
) {
    val allSoldiers = platoon.squads.flatMap { it.soldiers }.map { effectiveSoldier(it, simulation) }
    val counts      = statusCounts(allSoldiers)

    Column(modifier = modifier.fillMaxSize()) {
        ViewMeta(
            label  = platoon.name,
            sub    = platoon.sector,
            total  = allSoldiers.size,
            counts = counts,
        )
        Spacer(Modifier.height(GwSpacing.sp3.dp))

        // Platoon view: 2-column grid for up to 4 squads
        val squadRows = platoon.squads.chunked(2)
        squadRows.forEach { pair ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                pair.forEach { squad ->
                    SquadCard(
                        squad      = squad,
                        onClick    = { onPickSquad(squad.id) },
                        simulation = simulation,
                        modifier   = Modifier.weight(1f),   // ★ NO fillMaxHeight
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(GwSpacing.sp4.dp))
        }
    }
}

@Composable
private fun SquadCard(
    squad: Squad, onClick: () -> Unit,
    simulation: SimulationEngine?,
    modifier: Modifier,
) {
    val soldiers    = squad.soldiers.map { effectiveSoldier(it, simulation) }
    val hasAlert    = soldiers.any { it.status == SoldierStatus.CRITICAL }
    val borderColor = if (hasAlert) GwColors.critRed else GwColors.strokeHairline
    val counts      = statusCounts(soldiers)
    val tl          = squad.soldiers.firstOrNull { it.role == SoldierRole.TL }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GwRadii.r2.dp))
            .background(GwColors.bg200)
            .border(if (hasAlert) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(GwRadii.r2.dp))
            .clickable { onClick() }
            .padding(GwSpacing.sp3.dp),
    ) {
        // ── Header ───────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.squad_short, squad.id),
                style    = GwTypography.Audit.copy(color = GwColors.fg300),
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            if (tl != null) {
                Text(stringResource(R.string.role_tl) + " ", style = GwTypography.Audit.copy(color = GwColors.fg300))
                Text(tl.last, style = GwTypography.Audit.copy(color = GwColors.fg200),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(
            text     = squad.name,
            style    = GwTypography.Label.copy(color = GwColors.fg000, fontSize = 18.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(GwSpacing.sp2.dp))

        // ── 2 rows × 4 soldiers tile grid ────────
        val rows = soldiers.chunked(4)
        rows.forEach { rowSoldiers ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                rowSoldiers.forEach { s ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(s.status.color.copy(alpha = 0.85f))
                    )
                }
                // Pad last row if fewer than 4
                repeat(4 - rowSoldiers.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(GwSpacing.sp2.dp))

        // ── Footer ───────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusCountRow(counts)
            Spacer(Modifier.weight(1f))
            Text("${stringResource(R.string.open_action)} ◀", style = GwTypography.Audit.copy(color = GwColors.infoCyan))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// SQUAD VIEW — 2-column × 4-row soldier card grid (no LazyGrid!)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun SquadView(
    squad: Squad,
    onPickSoldier: (String) -> Unit,
    selectedId: String?,
    simulation: SimulationEngine? = null,
    removedSoldierIds: Set<String> = emptySet(),
    onRemoveSoldier: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val soldiers = squad.soldiers
        .filter { it.id !in removedSoldierIds }
        .map { effectiveSoldier(it, simulation) }
    val counts = statusCounts(soldiers)

    Column(modifier = modifier.fillMaxSize()) {
        ViewMeta(
            label  = stringResource(R.string.squad_title, squad.name),
            sub    = stringResource(R.string.squad_short, squad.id),
            total  = soldiers.size,
            counts = counts,
        )
        Spacer(Modifier.height(GwSpacing.sp3.dp))

        // Split soldiers into pairs, render each pair as a Row
        val rows = soldiers.chunked(2)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(GwSpacing.sp3.dp),
        ) {
            rows.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp3.dp),
                ) {
                    pair.forEach { soldier ->
                        SoldierCard(
                            soldier   = soldier,
                            selected  = soldier.id == selectedId,
                            onClick   = { onPickSoldier(soldier.id) },
                            onRemove  = onRemoveSoldier?.let { cb -> { cb(soldier.id) } },
                            modifier  = Modifier.weight(1f),
                        )
                    }
                    // Pad if odd number
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// SHARED HELPERS
// ──────────────────────────────────────────────────────────────────────────────

/** Tiny colored square tile representing one soldier in compact grid views. */
@Composable
fun SoldierTile(soldier: Soldier) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
            .background(soldier.status.color.copy(alpha = 0.85f)),
    )
}

data class StatusCounts(
    val ok:       Int = 0,
    val caution:  Int = 0,
    val high:     Int = 0,
    val critical: Int = 0,
    val offline:  Int = 0,
)

fun statusCounts(soldiers: List<Soldier>): StatusCounts {
    var ok = 0; var caution = 0; var high = 0; var critical = 0; var offline = 0
    soldiers.forEach {
        when (it.status) {
            SoldierStatus.OK       -> ok++
            SoldierStatus.CAUTION  -> caution++
            SoldierStatus.HIGH     -> high++
            SoldierStatus.CRITICAL -> critical++
            SoldierStatus.OFFLINE  -> offline++
        }
    }
    return StatusCounts(ok, caution, high, critical, offline)
}

@Composable
fun StatusCountRow(counts: StatusCounts) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        StatusCount(counts.ok,       SoldierStatus.OK.color,       stringResource(R.string.status_ok))
        StatusCount(counts.caution,  SoldierStatus.CAUTION.color,  stringResource(R.string.status_caut))
        StatusCount(counts.high,     SoldierStatus.HIGH.color,     stringResource(R.string.status_high))
        StatusCount(counts.critical, SoldierStatus.CRITICAL.color, stringResource(R.string.status_crit))
        if (counts.offline > 0)
            StatusCount(counts.offline, SoldierStatus.OFFLINE.color, stringResource(R.string.status_offline))
    }
}

@Composable
private fun StatusCount(count: Int, color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(2.dp))
        Text("$count", style = GwTypography.Audit.copy(color = color))
        Spacer(Modifier.width(1.dp))
        Text(label, style = GwTypography.Audit.copy(color = GwColors.fg400, fontSize = 9.sp))
    }
}

@Composable
fun ViewMeta(label: String, sub: String, total: Int, counts: StatusCounts) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = label,
                style    = GwTypography.H2.copy(color = GwColors.fg000),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text     = sub,
                style    = GwTypography.Audit.copy(color = GwColors.fg300),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(GwSpacing.sp3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                total.toString(),
                style = GwTypography.MonoXl.copy(color = GwColors.fg000),
            )
            Spacer(Modifier.width(GwSpacing.sp2.dp))
            Column {
                Text(stringResource(R.string.personnel), style = GwTypography.Audit.copy(color = GwColors.fg300))
                StatusCountRow(counts)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CASUALTIES / TRIAGE BOARD — all WIA across all units in one place
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CasualtiesView(
    simulation: SimulationEngine? = null,
    onPickSoldier: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val wounded = DemoData.allWounded()
    val critCount = wounded.count { it.third.status == SoldierStatus.CRITICAL }
    val highCount = wounded.count { it.third.status == SoldierStatus.HIGH }
    val cautCount = wounded.count { it.third.status == SoldierStatus.CAUTION }

    val infiniteTransition = rememberInfiniteTransition(label = "triagePulse")
    val triagePulse by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "triagePulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(GwRadii.r2.dp))
            .background(if (critCount > 0) GwColors.critRed.copy(alpha = triagePulse) else GwColors.bg100)
            .border(2.dp, if (critCount > 0) GwColors.critRed else Color.Transparent, RoundedCornerShape(GwRadii.r2.dp))
            .padding(if (critCount > 0) GwSpacing.sp3.dp else 0.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (critCount > 0) {
                    Text("⚠ מצב חירום", style = GwTypography.H1.copy(color = GwColors.critRed, fontSize = 24.sp))
                } else {
                    Text(stringResource(R.string.triage_board), style = GwTypography.H2.copy(color = GwColors.fg000))
                }
                Text(stringResource(R.string.triage_subtitle),
                    style = GwTypography.Audit.copy(color = GwColors.fg300))
            }
            // Count badges
            SeverityBadge(stringResource(R.string.status_crit), critCount, GwColors.critRed)
            Spacer(Modifier.width(GwSpacing.sp2.dp))
            SeverityBadge(stringResource(R.string.status_high), highCount, SoldierStatus.HIGH.color)
            Spacer(Modifier.width(GwSpacing.sp2.dp))
            SeverityBadge(stringResource(R.string.status_caut), cautCount, SoldierStatus.CAUTION.color)
            Spacer(Modifier.width(GwSpacing.sp2.dp))
            SeverityBadge(stringResource(R.string.total), wounded.size, GwColors.fg200)
        }

        Spacer(Modifier.height(GwSpacing.sp3.dp))

        if (wounded.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✓", style = GwTypography.H1.copy(color = GwColors.stateLive, fontSize = 48.sp))
                    Text(stringResource(R.string.all_nominal),
                        style = GwTypography.Label.copy(color = GwColors.stateLive))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GwSpacing.sp2.dp),
            ) {
                wounded.forEach { (platoon, squad, rawSoldier) ->
                    val s = effectiveSoldier(rawSoldier, simulation)
                    CasualtyRow(
                        soldier     = s,
                        platoonName = platoon.name,
                        squadName   = squad.name,
                        onView      = { onPickSoldier(s.id) },
                    )
                }
                Spacer(Modifier.height(GwSpacing.sp4.dp))
            }
        }
    }
}

@Composable
private fun CasualtyRow(
    soldier: Soldier,
    platoonName: String,
    squadName: String,
    onView: () -> Unit,
) {
    val statusColor = soldier.status.color
    val isCritical  = soldier.status == SoldierStatus.CRITICAL

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GwRadii.r2.dp))
            .background(if (isCritical) GwColors.critRedBg else GwColors.bg200)
            .border(
                if (isCritical) 1.5.dp else 1.dp,
                if (isCritical) GwColors.critRed else GwColors.strokeHairline,
                RoundedCornerShape(GwRadii.r2.dp),
            )
            .clickable { onView() }
            .padding(horizontal = GwSpacing.sp4.dp, vertical = GwSpacing.sp3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status indicator strip
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(statusColor),
        )
        Spacer(Modifier.width(GwSpacing.sp3.dp))

        // Status badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(GwRadii.r1.dp))
                .background(statusColor.copy(alpha = 0.15f))
                .border(1.dp, statusColor, RoundedCornerShape(GwRadii.r1.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(soldier.status.label,
                style = GwTypography.Audit.copy(color = statusColor, fontSize = 10.sp))
        }
        Spacer(Modifier.width(GwSpacing.sp3.dp))

        // Identity
        Column(modifier = Modifier.width(130.dp)) {
            Text(
                "POS-${soldier.pos.toString().padStart(2,'0')} ${soldier.last}",
                style = GwTypography.Label.copy(color = GwColors.fg000),
                maxLines = 1,
            )
            Text(
                "${soldier.role.display} · ${soldier.id}",
                style = GwTypography.Audit.copy(color = GwColors.fg300),
            )
        }
        Spacer(Modifier.width(GwSpacing.sp3.dp))

        // Unit path
        Column(modifier = Modifier.width(140.dp)) {
            Text(platoonName, style = GwTypography.Audit.copy(color = GwColors.fg300), maxLines = 1)
            Text(squadName,   style = GwTypography.MonoSm.copy(color = GwColors.fg200), maxLines = 1)
        }
        Spacer(Modifier.width(GwSpacing.sp3.dp))

        // Vitals
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp4.dp),
        ) {
            TriageVital("HR",   soldier.hr.toString(), "bpm",
                if (soldier.hr > 140) GwColors.critRed else GwColors.fg000)
            TriageVital("BR",   soldier.br.toString(), "rpm",
                if (soldier.br > 22) GwColors.warnAmber else GwColors.fg000)
            TriageVital("SpO₂", if (soldier.spo2 > 0) "${soldier.spo2}%" else "--", "",
                if (soldier.spo2 in 1..91) GwColors.critRed else GwColors.fg000)
            TriageVital("RISK", String.format("%.1f", soldier.risk), "/10", statusColor)
        }

        Spacer(Modifier.width(GwSpacing.sp3.dp))

        // Actions
        ActionChip(stringResource(R.string.view), GwColors.bg300, GwColors.strokeDefault, GwColors.fg000, onView)
    }
}

@Composable
private fun TriageVital(label: String, value: String, unit: String, color: Color) {
    Column {
        Text(label, style = GwTypography.Audit.copy(color = GwColors.fg400, fontSize = 9.sp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = GwTypography.MonoSm.copy(color = color))
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(1.dp))
                Text(unit, style = GwTypography.Audit.copy(color = GwColors.fg400, fontSize = 9.sp))
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, bg: Color, border: Color, fg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(GwRadii.r1.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(GwRadii.r1.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = GwTypography.Audit.copy(color = fg, fontSize = 10.sp))
    }
}

@Composable
private fun SeverityBadge(label: String, count: Int, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(GwRadii.r1.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(GwRadii.r1.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$count", style = GwTypography.MonoSm.copy(color = color))
            Spacer(Modifier.width(3.dp))
            Text(label, style = GwTypography.Audit.copy(color = color, fontSize = 9.sp))
        }
    }
}

