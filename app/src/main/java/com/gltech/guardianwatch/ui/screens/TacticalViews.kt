package com.gltech.guardianwatch.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gltech.guardianwatch.model.Company
import com.gltech.guardianwatch.model.Platoon
import com.gltech.guardianwatch.model.Soldier
import com.gltech.guardianwatch.model.SoldierRole
import com.gltech.guardianwatch.model.SoldierStatus
import com.gltech.guardianwatch.model.Squad
import com.gltech.guardianwatch.ui.components.SoldierCard
import com.gltech.guardianwatch.ui.theme.GwColors
import com.gltech.guardianwatch.ui.theme.GwRadii
import com.gltech.guardianwatch.ui.theme.GwSpacing
import com.gltech.guardianwatch.ui.theme.GwTypography

// ──────────────────────────────────────────────────────────────────────────────
// COMPANY VIEW — 3 platoon cards side-by-side
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CompanyView(
    company: Company,
    onPickPlatoon: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allSoldiers = company.platoons.flatMap { p -> p.squads.flatMap { s -> s.soldiers } }
    val counts = statusCounts(allSoldiers)

    Column(modifier = modifier.fillMaxSize()) {
        ViewMeta(
            label  = "COMPANY OVERVIEW",
            sub    = "${company.platoons.size} PLATOONS · ${allSoldiers.size} PERSONNEL",
            total  = allSoldiers.size,
            counts = counts,
        )
        Spacer(Modifier.height(GwSpacing.sp4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp4.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            company.platoons.forEach { platoon ->
                PlatoonCard(
                    platoon  = platoon,
                    onClick  = { onPickPlatoon(platoon.id) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun PlatoonCard(platoon: Platoon, onClick: () -> Unit, modifier: Modifier) {
    val soldiers    = platoon.squads.flatMap { it.soldiers }
    val hasAlert    = soldiers.any { it.status == SoldierStatus.CRITICAL }
    val borderColor = if (hasAlert) GwColors.critRed else GwColors.strokeHairline
    val counts      = statusCounts(soldiers)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GwRadii.r2.dp))
            .background(GwColors.bg200)
            .border(if (hasAlert) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(GwRadii.r2.dp))
            // ★ entire card is now clickable
            .clickable { onClick() }
            .padding(GwSpacing.sp4.dp),
    ) {
        // ── Header ───────────────────────────────────────────────
        Text(
            text      = platoon.callsign,
            style     = GwTypography.Audit.copy(color = GwColors.fg300),
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
        )
        Text(
            text      = platoon.name,
            style     = GwTypography.Label.copy(color = GwColors.fg000, fontSize = 18.sp),
            maxLines  = 1,                        // ★ no more vertical text
            overflow  = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text      = platoon.sector,
            style     = GwTypography.Audit.copy(color = GwColors.fg300),
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(GwSpacing.sp3.dp))

        // ── Soldier tile grid (3 rows of 8) ─────────────────────
        // Using plain Column+Row so NO LazyVerticalGrid steals touch events
        platoon.squads.forEach { squad ->
            val squadHasAlert = squad.soldiers.any { it.status == SoldierStatus.CRITICAL }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                squad.soldiers.forEach { s ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(s.status.color.copy(alpha = 0.85f))
                    )
                }
            }
        }

        Spacer(Modifier.height(GwSpacing.sp3.dp))

        // ── Footer: counts + open hint ───────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusCountRow(counts)
            Spacer(Modifier.weight(1f))
            Text(
                "OPEN ▶",
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
    modifier: Modifier = Modifier,
) {
    val allSoldiers = platoon.squads.flatMap { it.soldiers }
    val counts      = statusCounts(allSoldiers)

    Column(modifier = modifier.fillMaxSize()) {
        ViewMeta(
            label  = platoon.name,
            sub    = platoon.sector,
            total  = allSoldiers.size,
            counts = counts,
        )
        Spacer(Modifier.height(GwSpacing.sp4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp4.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            platoon.squads.forEach { squad ->
                SquadCard(
                    squad    = squad,
                    onClick  = { onPickSquad(squad.id) },   // ★ passes squad.id up
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun SquadCard(squad: Squad, onClick: () -> Unit, modifier: Modifier) {
    val hasAlert    = squad.soldiers.any { it.status == SoldierStatus.CRITICAL }
    val borderColor = if (hasAlert) GwColors.critRed else GwColors.strokeHairline
    val counts      = statusCounts(squad.soldiers)
    val tl          = squad.soldiers.firstOrNull { it.role == SoldierRole.TL }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GwRadii.r2.dp))
            .background(GwColors.bg200)
            .border(if (hasAlert) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(GwRadii.r2.dp))
            // ★ entire card is clickable — no LazyGrid in the way
            .clickable { onClick() }
            .padding(GwSpacing.sp4.dp),
    ) {
        // ── Header ───────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "SQD · ${squad.id}",
                style    = GwTypography.Audit.copy(color = GwColors.fg300),
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            if (tl != null) {
                Text("TL ", style = GwTypography.Audit.copy(color = GwColors.fg300))
                Text(tl.last, style = GwTypography.Audit.copy(color = GwColors.fg200),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(
            text     = squad.name,
            style    = GwTypography.Label.copy(color = GwColors.fg000, fontSize = 20.sp),
            maxLines = 1,           // ★ no more vertical text
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(GwSpacing.sp3.dp))

        // ── 2 rows × 4 soldiers tile grid (no LazyGrid!) ────────
        val rows = squad.soldiers.chunked(4)
        rows.forEach { rowSoldiers ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                rowSoldiers.forEach { s ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
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

        Spacer(Modifier.height(GwSpacing.sp3.dp))

        // ── Footer ───────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusCountRow(counts)
            Spacer(Modifier.weight(1f))
            Text("OPEN ▶", style = GwTypography.Audit.copy(color = GwColors.infoCyan))
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
    modifier: Modifier = Modifier,
) {
    val counts = statusCounts(squad.soldiers)

    Column(modifier = modifier.fillMaxSize()) {
        ViewMeta(
            label  = "Squad ${squad.name}",
            sub    = "SQD ${squad.id} · ${squad.soldiers.size} SOLDIERS",
            total  = squad.soldiers.size,
            counts = counts,
        )
        Spacer(Modifier.height(GwSpacing.sp4.dp))

        // Split 8 soldiers into 4 pairs, render each pair as a Row
        // This avoids LazyVerticalGrid touch-stealing completely
        val rows = squad.soldiers.chunked(2)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(GwSpacing.sp4.dp),
        ) {
            rows.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp4.dp),
                ) {
                    pair.forEach { soldier ->
                        SoldierCard(
                            soldier  = soldier,
                            selected = soldier.id == selectedId,
                            onClick  = { onPickSoldier(soldier.id) },
                            modifier = Modifier.weight(1f),
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
        StatusCount(counts.ok,       SoldierStatus.OK.color,       "OK")
        StatusCount(counts.caution,  SoldierStatus.CAUTION.color,  "CAUT")
        StatusCount(counts.high,     SoldierStatus.HIGH.color,     "HIGH")
        StatusCount(counts.critical, SoldierStatus.CRITICAL.color, "CRIT")
        if (counts.offline > 0)
            StatusCount(counts.offline, SoldierStatus.OFFLINE.color, "OFF")
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
        Spacer(Modifier.width(GwSpacing.sp4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                total.toString(),
                style = GwTypography.MonoXl.copy(color = GwColors.fg000),
            )
            Spacer(Modifier.width(GwSpacing.sp3.dp))
            Column {
                Text("PERSONNEL", style = GwTypography.Audit.copy(color = GwColors.fg300))
                StatusCountRow(counts)
            }
        }
    }
}
