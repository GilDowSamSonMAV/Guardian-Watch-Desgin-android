package com.gltech.guardianwatch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

// ──────────────────────────────────────────────────────────────────────────────
// COMPANY VIEW — grid of platoon cards (left panel)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CompanyView(
    company: Company,
    onPickPlatoon: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allSoldiers = company.platoons.flatMap { p -> p.squads.flatMap { s -> s.soldiers } }
    val counts = statusCounts(allSoldiers)

    Column(modifier = modifier) {
        // Meta
        ViewMeta(
            label = "COMPANY OVERVIEW",
            sub   = "${company.platoons.size} PLATOONS",
            total = allSoldiers.size,
            counts = counts,
        )
        Spacer(Modifier.height(GwSpacing.sp4.dp))

        // Platoon cards
        Row(
            horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            company.platoons.forEach { platoon ->
                PlatoonCard(
                    platoon    = platoon,
                    onClick    = { onPickPlatoon(platoon.id) },
                    modifier   = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PlatoonCard(platoon: Platoon, onClick: () -> Unit, modifier: Modifier) {
    val soldiers = platoon.squads.flatMap { it.soldiers }
    val hasAlert = soldiers.any { it.status == SoldierStatus.CRITICAL }
    val borderColor = if (hasAlert) GwColors.critRed else GwColors.strokeHairline

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GwRadii.r2.dp))
            .background(GwColors.bg200)
            .border(1.dp, borderColor, RoundedCornerShape(GwRadii.r2.dp))
            .padding(GwSpacing.sp4.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(platoon.callsign,
                    style = GwTypography.Audit.copy(color = GwColors.fg300))
                Text(platoon.name,
                    style = GwTypography.H2.copy(color = GwColors.fg000))
            }
            Text(platoon.sector,
                style = GwTypography.Audit.copy(color = GwColors.fg300))
        }
        Spacer(Modifier.height(GwSpacing.sp3.dp))

        // Soldier tile grid (all soldiers in this platoon)
        val gridCols = platoon.squads.firstOrNull()?.soldiers?.size ?: 8
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridCols),
            modifier = Modifier.height(84.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement   = Arrangement.spacedBy(3.dp),
        ) {
            items(soldiers) { s ->
                SoldierTile(s)
            }
        }

        Spacer(Modifier.height(GwSpacing.sp3.dp))

        // Counts + OPEN button
        val counts = statusCounts(soldiers)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusCountRow(counts)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(GwRadii.r1.dp))
                    .border(1.dp, GwColors.strokeDefault, RoundedCornerShape(GwRadii.r1.dp))
                    .clickable { onClick() }
                    .padding(horizontal = GwSpacing.sp3.dp, vertical = GwSpacing.sp1.dp),
            ) {
                Text("OPEN ▶", style = GwTypography.Audit.copy(color = GwColors.fg200))
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// PLATOON VIEW — grid of squad cards
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun PlatoonView(
    platoon: Platoon,
    onPickSquad: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allSoldiers = platoon.squads.flatMap { it.soldiers }
    val counts = statusCounts(allSoldiers)

    Column(modifier = modifier) {
        ViewMeta(
            label  = platoon.name,
            sub    = platoon.sector,
            total  = allSoldiers.size,
            counts = counts,
        )
        Spacer(Modifier.height(GwSpacing.sp4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            platoon.squads.forEach { squad ->
                SquadCard(
                    squad   = squad,
                    onClick = { onPickSquad(squad.id) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SquadCard(squad: Squad, onClick: () -> Unit, modifier: Modifier) {
    val hasAlert = squad.soldiers.any { it.status == SoldierStatus.CRITICAL }
    val borderColor = if (hasAlert) GwColors.critRed else GwColors.strokeHairline

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GwRadii.r2.dp))
            .background(GwColors.bg200)
            .border(1.dp, borderColor, RoundedCornerShape(GwRadii.r2.dp))
            .padding(GwSpacing.sp4.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SQUAD · ${squad.id}", style = GwTypography.Audit.copy(color = GwColors.fg300))
            Spacer(Modifier.weight(1f))
            val tl = squad.soldiers.firstOrNull { it.role == com.gltech.guardianwatch.model.SoldierRole.TL }
            if (tl != null) {
                Text("TL", style = GwTypography.Audit.copy(color = GwColors.fg300))
                Spacer(Modifier.width(3.dp))
                Text(tl.last, style = GwTypography.Audit.copy(color = GwColors.fg200))
            }
        }
        Text(squad.name, style = GwTypography.H2.copy(color = GwColors.fg000))
        Spacer(Modifier.height(GwSpacing.sp3.dp))

        // 4×2 tile grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.height(60.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement   = Arrangement.spacedBy(3.dp),
        ) {
            items(squad.soldiers) { s ->
                SoldierTile(s)
            }
        }

        Spacer(Modifier.height(GwSpacing.sp3.dp))

        val counts = statusCounts(squad.soldiers)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusCountRow(counts)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(GwRadii.r1.dp))
                    .border(1.dp, GwColors.strokeDefault, RoundedCornerShape(GwRadii.r1.dp))
                    .clickable { onClick() }
                    .padding(horizontal = GwSpacing.sp3.dp, vertical = GwSpacing.sp1.dp),
            ) {
                Text("OPEN ▶", style = GwTypography.Audit.copy(color = GwColors.fg200))
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// SQUAD VIEW — 2-column × 4-row soldier card grid
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun SquadView(
    squad: Squad,
    onPickSoldier: (String) -> Unit,
    selectedId: String?,
    modifier: Modifier = Modifier,
) {
    val counts = statusCounts(squad.soldiers)

    Column(modifier = modifier) {
        ViewMeta(
            label  = "Squad ${squad.name}",
            sub    = "SQD ${squad.id}",
            total  = squad.soldiers.size,
            counts = counts,
        )
        Spacer(Modifier.height(GwSpacing.sp4.dp))

        // 2-column card grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp4.dp),
            verticalArrangement   = Arrangement.spacedBy(GwSpacing.sp4.dp),
        ) {
            items(squad.soldiers) { soldier ->
                com.gltech.guardianwatch.ui.components.SoldierCard(
                    soldier  = soldier,
                    selected = soldier.id == selectedId,
                    onClick  = { onPickSoldier(soldier.id) },
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// SHARED HELPERS
// ──────────────────────────────────────────────────────────────────────────────

/** Tiny colored square tile representing one soldier in grid views. */
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
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        StatusCount(counts.ok,       SoldierStatus.OK.color,       "OK")
        StatusCount(counts.caution,  SoldierStatus.CAUTION.color,  "CAUT")
        StatusCount(counts.high,     SoldierStatus.HIGH.color,     "HIGH")
        StatusCount(counts.critical, SoldierStatus.CRITICAL.color, "CRIT")
        StatusCount(counts.offline,  SoldierStatus.OFFLINE.color,  "OFF")
    }
}

@Composable
private fun StatusCount(count: Int, color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(androidx.compose.foundation.shape.CircleShape)
            .background(color))
        Spacer(Modifier.width(2.dp))
        Text("$count", style = GwTypography.Audit.copy(color = color))
        Spacer(Modifier.width(1.dp))
        // label only for OK to save space
        if (label == "OK") Text(label, style = GwTypography.Audit.copy(color = GwColors.fg400, fontSize = 9.sp))
    }
}

@Composable
fun ViewMeta(label: String, sub: String, total: Int, counts: StatusCounts) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = GwTypography.H2.copy(color = GwColors.fg000))
            Text(sub,   style = GwTypography.Audit.copy(color = GwColors.fg300))
        }
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
