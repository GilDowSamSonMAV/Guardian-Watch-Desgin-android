package com.gltech.guardianwatch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gltech.guardianwatch.model.Company
import com.gltech.guardianwatch.model.CriticalAlert
import com.gltech.guardianwatch.model.DemoData
import com.gltech.guardianwatch.model.Soldier
import com.gltech.guardianwatch.ui.components.*
import com.gltech.guardianwatch.ui.theme.GwColors
import com.gltech.guardianwatch.ui.theme.GwSpacing
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

/**
 * Master tactical medical dashboard.
 *
 * Layout:
 *   [TopBar — full width]
 *   [AlertBanner — full width, shown when alert active]
 *   [Left pane (55%)] | [Right pane — TacMap (45%)]
 *   [FootStatusBar — full width]
 *
 * Left pane shows Company / Platoon / Squad views based on nav state.
 * Right pane always shows the tactical map.
 * Overlays: SoldierDetailOverlay, NineLineOverlay.
 */

/** Navigation level within the hierarchy. */
sealed class TacNavLevel {
    object Company : TacNavLevel()
    data class Platoon(val platoonId: String) : TacNavLevel()
    data class Squad(val platoonId: String, val squadId: String) : TacNavLevel()
}

@Composable
fun TacticalDashboard(
    company: Company = DemoData.COMPANY,
    alert: CriticalAlert? = DemoData.CRITICAL_ALERT,
) {
    // Navigation state
    var navLevel by remember { mutableStateOf<TacNavLevel>(
        TacNavLevel.Squad("PLT-2", "2A")   // open on the critical squad by default
    ) }
    var selectedSoldier by remember { mutableStateOf<String?>(null) }
    var showNineLine    by remember { mutableStateOf(false) }
    var alertActive     by remember { mutableStateOf(alert != null) }
    var time            by remember { mutableStateOf(nowUtc()) }

    // Tick clock every second
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            time = nowUtc()
        }
    }

    // Derived nav data
    val currentPlatoon = when (val n = navLevel) {
        is TacNavLevel.Platoon -> DemoData.findPlatoon(n.platoonId)
        is TacNavLevel.Squad   -> DemoData.findPlatoon(n.platoonId)
        else -> null
    }
    val currentSquad = when (val n = navLevel) {
        is TacNavLevel.Squad -> DemoData.findSquad(n.squadId)
        else -> null
    }
    val alertSoldier: Soldier? = alert?.let { DemoData.findSoldier(it.soldierId) }

    // Breadcrumb items
    val breadcrumbs: List<BreadcrumbItem> = buildList {
        add(BreadcrumbItem("COY", company.name))
        if (navLevel is TacNavLevel.Platoon || navLevel is TacNavLevel.Squad) {
            currentPlatoon?.let { add(BreadcrumbItem("PLT", it.name)) }
        }
        if (navLevel is TacNavLevel.Squad) {
            currentSquad?.let { add(BreadcrumbItem("SQD", "Squad ${it.name}")) }
        }
    }

    // Navigation handlers
    val onBcNavigate: (Int) -> Unit = { i ->
        when (i) {
            0 -> navLevel = TacNavLevel.Company
            1 -> {
                val pId = when (val n = navLevel) {
                    is TacNavLevel.Platoon -> n.platoonId
                    is TacNavLevel.Squad   -> n.platoonId
                    else -> null
                }
                if (pId != null) navLevel = TacNavLevel.Platoon(pId)
            }
        }
    }

    val onPickPlatoon: (String) -> Unit = { id ->
        navLevel = TacNavLevel.Platoon(id)
        selectedSoldier = null
    }

    val onPickSquad: (String) -> Unit = { sqId ->
        val plt = company.platoons.firstOrNull { p -> p.squads.any { it.id == sqId } }
        if (plt != null) navLevel = TacNavLevel.Squad(plt.id, sqId)
        selectedSoldier = null
    }

    val onPickSoldier: (String) -> Unit = { id -> selectedSoldier = id }

    val onAlertView: () -> Unit = {
        // Navigate to the alert soldier's squad
        alert?.let { a ->
            val sqId = DemoData.soldierSquadId(a.soldierId)
            if (sqId != null) onPickSquad(sqId)
            selectedSoldier = a.soldierId
        }
    }

    // All soldiers for mesh count
    val allSoldiers = DemoData.allSoldiers()
    val meshOnline  = allSoldiers.count { it.status != com.gltech.guardianwatch.model.SoldierStatus.OFFLINE }

    Column(modifier = Modifier.fillMaxSize().background(GwColors.bg000)) {

        // TOP BAR
        TacticalTopBar(
            missionLabel = "MEDCOM · ${company.callsign}",
            opLabel      = "OP NORTH WIND · D+3",
            timeStr      = time,
            status       = SystemStatus(
                meshConnected = meshOnline,
                meshTotal     = allSoldiers.size,
            ),
        )

        // GLOBAL ALERT BANNER
        if (alertActive && alert != null) {
            GlobalAlertBanner(
                alert     = alert,
                soldier   = alertSoldier,
                onView    = onAlertView,
                onAck     = { alertActive = false },
                onCasevac = { showNineLine = true; onAlertView() },
            )
        }

        // MAIN BODY
        Row(modifier = Modifier.weight(1f)) {

            // LEFT PANE — hierarchy views
            Box(modifier = Modifier.fillMaxHeight().weight(0.55f)) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Toolbar: breadcrumb + meta
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(GwColors.bg100)
                        .padding(horizontal = GwSpacing.sp4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Breadcrumb(items = breadcrumbs, onNavigate = onBcNavigate)
                }

                // Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(GwSpacing.sp4.dp),
                ) {
                    when (val level = navLevel) {
                        is TacNavLevel.Company ->
                            CompanyView(
                                company       = company,
                                onPickPlatoon = onPickPlatoon,
                            )

                        is TacNavLevel.Platoon -> {
                            val plt = DemoData.findPlatoon(level.platoonId)
                            if (plt != null) {
                                PlatoonView(
                                    platoon     = plt,
                                    onPickSquad = onPickSquad,
                                )
                            }
                        }

                        is TacNavLevel.Squad -> {
                            val sq = DemoData.findSquad(level.squadId)
                            if (sq != null) {
                                SquadView(
                                    squad         = sq,
                                    onPickSoldier = onPickSoldier,
                                    selectedId    = selectedSoldier,
                                )
                            }
                        }
                    }
                }

                // FOOT STATUS BAR
                FootStatusBar(
                    meshOnline = meshOnline,
                    meshTotal  = allSoldiers.size,
                    lastMsg    = "[12:42:18] ALEPH-2A-02 vitals threshold breach · auto-flag CRITICAL",
                )
            }
            // Right-side divider
            Box(modifier = Modifier.align(Alignment.CenterEnd).width(1.dp).fillMaxHeight()
                .background(GwColors.strokeHairline))
            } // end Box wrapper

            // RIGHT PANE — tactical map
            TacMap(
                company         = company,
                activeSquadId   = (navLevel as? TacNavLevel.Squad)?.squadId,
                alertSoldierId  = alert?.soldierId,
                onPinClicked    = { soldier ->
                    onPickSquad(soldier.squadId)
                    selectedSoldier = soldier.id
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.45f),
            )
        }

        // CASEVAC FAB — bottom right
        // (Rendered as part of the parent Box if this is in a Box; here we handle as overlay below)
    }

    // ── OVERLAYS ──

    // Soldier detail
    val soldierId = selectedSoldier
    if (soldierId != null) {
        val soldier = DemoData.findSoldier(soldierId)
        if (soldier != null) {
            SoldierDetailOverlay(
                soldier   = soldier,
                onClose   = { selectedSoldier = null },
                onCasevac = { showNineLine = true },
            )
        }
    }

    // 9-Line CASEVAC
    if (showNineLine) {
        val casevacSoldier = selectedSoldier?.let { DemoData.findSoldier(it) }
            ?: alertSoldier
        NineLineOverlay(
            soldier = casevacSoldier,
            onClose = { showNineLine = false },
        )
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun nowUtc(): String =
    SimpleDateFormat("HH:mm:ss", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date())
