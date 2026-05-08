package com.gltech.guardianwatch.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gltech.guardianwatch.model.*
import com.gltech.guardianwatch.ui.components.*
import com.gltech.guardianwatch.ui.theme.GwColors
import com.gltech.guardianwatch.ui.theme.GwRadii
import com.gltech.guardianwatch.ui.theme.GwSpacing
import com.gltech.guardianwatch.ui.theme.GwTypography
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import com.gltech.guardianwatch.R
import kotlin.random.Random
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
    object Company    : TacNavLevel()
    object Casualties : TacNavLevel()   // ★ Triage Board
    data class Platoon(val platoonId: String) : TacNavLevel()
    data class Squad(val platoonId: String, val squadId: String) : TacNavLevel()
}

@Composable
fun TacticalDashboard(
    company: Company = DemoData.COMPANY,
    alert: CriticalAlert? = DemoData.CRITICAL_ALERT,
) {
    // ── Simulation engine (single instance per dashboard lifecycle) ──
    val simulation = remember { SimulationEngine() }

    // Navigation state
    var navLevel by remember { mutableStateOf<TacNavLevel>(
        TacNavLevel.Squad("PLT-2", "2A")   // open on the critical squad by default
    ) }
    var selectedSoldier by remember { mutableStateOf<String?>(null) }
    var showNineLine    by remember { mutableStateOf(false) }
    var alertActive     by remember { mutableStateOf(alert != null) }
    var time            by remember { mutableStateOf(nowUtc()) }

    // Simulation-generated injury alert
    var simAlertActive  by remember { mutableStateOf(false) }
    var simAlert        by remember { mutableStateOf<CriticalAlert?>(null) }

    // ── Build initial static positions once (for TacMap and sim seeding) ──
    val staticPositions = remember {
        buildSoldierPositionsStatic(company, Random(1337))
    }

    // Tick clock + simulation every second
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            time = nowUtc()
            if (simulation.isRunning) {
                simulation.step()
                // Check for injury notification
                if (simulation.injuryNotificationPending) {
                    val injId = simulation.injuredSoldierId
                    if (injId != null) {
                        simAlert = CriticalAlert(
                            soldierId = injId,
                            type = "VITALS",
                            message = "TACHYCARDIA · POSSIBLE HEMORRHAGE · AUTO-DETECT",
                            triggeredSec = 0,
                            acknowledged = false,
                        )
                        simAlertActive = true
                    }
                    simulation.consumeInjuryNotification()
                }
            }
        }
    }

    // ★ Auto re-alert every 10s if there are CRITICAL soldiers — makes it impossible to ignore
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10_000)
            val hasCritical = DemoData.ACTIVE_ALERTS.isNotEmpty() ||
                (simulation.isRunning && simulation.liveVitals.values.any {
                    it.status == SoldierStatus.CRITICAL
                })
            if (hasCritical) {
                alertActive = true   // re-raise the static alert
            }
        }
    }

    // ── Helper: get effective soldier with live vitals if sim running ──
    fun effectiveSoldier(s: Soldier): Soldier {
        if (!simulation.isRunning) return s
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

    // Choose which alert is active — sim alert takes priority
    val activeAlert = if (simAlertActive && simAlert != null) simAlert else if (alertActive) alert else null
    val alertSoldier: Soldier? = activeAlert?.let { DemoData.findSoldier(it.soldierId) }
        ?.let { effectiveSoldier(it) }

    // Breadcrumb items
    val breadcrumbs: List<BreadcrumbItem> = buildList {
        add(BreadcrumbItem("COY", company.name))
        if (navLevel == TacNavLevel.Casualties) {
            add(BreadcrumbItem("WIA", "Triage Board"))
        }
        if (navLevel is TacNavLevel.Platoon || navLevel is TacNavLevel.Squad) {
            currentPlatoon?.let { add(BreadcrumbItem("PLT", it.name)) }
        }
        if (navLevel is TacNavLevel.Squad) {
            currentSquad?.let { add(BreadcrumbItem("SQD", it.name)) }
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
        activeAlert?.let { a ->
            val sqId = DemoData.soldierSquadId(a.soldierId)
            if (sqId != null) onPickSquad(sqId)
            selectedSoldier = a.soldierId
        }
    }

    // All soldiers for mesh count
    val allSoldiers = DemoData.allSoldiers()
    val meshOnline  = allSoldiers.count { it.status != SoldierStatus.OFFLINE }

    // Live positions for map
    val mapPositions = if (simulation.isRunning && simulation.livePositions.isNotEmpty()) {
        simulation.livePositions.toMap()
    } else {
        staticPositions
    }

    Column(modifier = Modifier.fillMaxSize().background(GwColors.bg000)) {

        // TOP BAR
        TacticalTopBar(
            missionLabel = "${stringResource(R.string.medcom)} · ${company.callsign}",
            opLabel      = "${stringResource(R.string.op_name)} · ${stringResource(R.string.day_plus)}",
            timeStr      = time,
            status       = SystemStatus(
                meshConnected = meshOnline,
                meshTotal     = allSoldiers.size,
            ),
        )

        // GLOBAL ALERT BANNER — handles both static and sim alerts
        if (activeAlert != null) {
            GlobalAlertBanner(
                alert     = activeAlert,
                soldier   = alertSoldier,
                onView    = onAlertView,
                onAck     = {
                    alertActive = false
                    simAlertActive = false
                },
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
                // ★ GUARDIAN WATCH brand header — cleaner, less dense
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    GwColors.bg100,
                                    GwColors.chromeOlive900,
                                    GwColors.bg100,
                                )
                            )
                        )
                        .padding(horizontal = GwSpacing.sp5.dp, vertical = GwSpacing.sp2.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text  = "GUARDIAN WATCH",
                                style = GwTypography.MonoLg.copy(
                                    color    = GwColors.fg000,
                                    fontSize = 22.sp,
                                    letterSpacing = 3.sp,
                                ),
                            )
                            Text(
                                text  = "${company.callsign} · TACTICAL MEDICAL OPS",
                                style = GwTypography.Audit.copy(color = GwColors.infoCyan),
                            )
                        }

                        // ★ SIMULATION BUTTON
                        SimulationButton(
                            isRunning = simulation.isRunning,
                            onToggle = {
                                if (simulation.isRunning) {
                                    simulation.stop()
                                    simAlertActive = false
                                    simAlert = null
                                } else {
                                    simulation.start(allSoldiers, staticPositions)
                                }
                            },
                        )
                    }
                }

                // Breadcrumb toolbar + WIA button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(GwColors.bg100)
                        .padding(horizontal = GwSpacing.sp4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Breadcrumb(items = breadcrumbs, onNavigate = onBcNavigate)
                    Spacer(Modifier.weight(1f))
                    // ★ WIA Triage Board button with live count
                    val woundedCount = DemoData.allWounded().size
                    val critCount    = DemoData.allWounded().count { it.third.status == SoldierStatus.CRITICAL }
                    val wiaActive    = navLevel == TacNavLevel.Casualties
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(GwRadii.r1.dp))
                            .background(if (wiaActive) GwColors.critRed else
                                if (critCount > 0) GwColors.critRedBg else GwColors.bg300)
                            .border(1.dp,
                                if (critCount > 0) GwColors.critRed else GwColors.strokeDefault,
                                RoundedCornerShape(GwRadii.r1.dp))
                            .clickable {
                                navLevel = if (wiaActive) TacNavLevel.Company
                                           else TacNavLevel.Casualties
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = if (critCount > 0) "⚠ ${stringResource(R.string.wia)} ×$woundedCount  ${stringResource(R.string.status_crit)} ×$critCount"
                                   else "${stringResource(R.string.wia)} ×$woundedCount",
                            style = GwTypography.Audit.copy(
                                color = if (critCount > 0 || wiaActive) GwColors.fg000 else GwColors.fg200,
                                fontSize = 10.sp,
                            ),
                        )
                    }
                }

                // Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(GwSpacing.sp3.dp),
                ) {
                    when (val level = navLevel) {
                        is TacNavLevel.Company ->
                            CompanyView(
                                company       = company,
                                onPickPlatoon = onPickPlatoon,
                                simulation    = simulation,
                            )

                        // ★ TRIAGE BOARD
                        TacNavLevel.Casualties ->
                            CasualtiesView(
                                simulation    = simulation,
                                onPickSoldier = { id ->
                                    val sqId = DemoData.soldierSquadId(id)
                                    if (sqId != null) onPickSquad(sqId)
                                    selectedSoldier = id
                                },
                                onCasevac = { id ->
                                    selectedSoldier = id
                                    showNineLine = true
                                },
                            )

                        is TacNavLevel.Platoon -> {
                            val plt = DemoData.findPlatoon(level.platoonId)
                            if (plt != null) {
                                PlatoonView(
                                    platoon     = plt,
                                    onPickSquad = onPickSquad,
                                    simulation  = simulation,
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
                                    simulation    = simulation,
                                )
                            }
                        }
                    }
                }

                // FOOT STATUS BAR
                FootStatusBar(
                    meshOnline = meshOnline,
                    meshTotal  = allSoldiers.size,
                    lastMsg    = if (simAlertActive && simAlert != null) {
                        "[${time}] ${simAlert!!.soldierId} vitals threshold breach · auto-flag CRITICAL"
                    } else {
                        "[12:42:18] ALEPH-2A-02 vitals threshold breach · auto-flag CRITICAL"
                    },
                )
            }
            // Right-side divider
            Box(modifier = Modifier.align(Alignment.CenterEnd).width(1.dp).fillMaxHeight()
                .background(GwColors.strokeHairline))
            } // end Box wrapper

            // RIGHT PANE — OSM satellite map
            OsmTacMap(
                company        = company,
                activeSquadId  = (navLevel as? TacNavLevel.Squad)?.squadId,
                alertSoldierId = activeAlert?.soldierId,
                onPinClicked   = { soldier ->
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

    // ── INJURY NOTIFICATION OVERLAY ──
    if (simAlertActive && simAlert != null) {
        InjuryNotificationOverlay(
            soldierName = DemoData.findSoldier(simAlert!!.soldierId)?.last ?: "UNKNOWN",
            soldierId   = simAlert!!.soldierId,
            onDismiss   = { /* stays until ACK on banner */ },
        )
    }

    // ── OVERLAYS ──

    // Soldier detail
    val soldierId = selectedSoldier
    if (soldierId != null) {
        val soldier = DemoData.findSoldier(soldierId)?.let { effectiveSoldier(it) }
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

// ─── SIMULATION BUTTON ───────────────────────────────────────────────────────

@Composable
private fun SimulationButton(isRunning: Boolean, onToggle: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (isRunning) GwColors.critRed.copy(alpha = 0.2f) else GwColors.stateLive.copy(alpha = 0.15f),
        animationSpec = tween(400),
        label = "simBtnBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isRunning) GwColors.critRed else GwColors.stateLive,
        animationSpec = tween(400),
        label = "simBtnBorder",
    )
    val labelColor by animateColorAsState(
        targetValue = if (isRunning) GwColors.critRed else GwColors.stateLive,
        animationSpec = tween(400),
        label = "simBtnLabel",
    )

    // Pulsing glow when running
    val infiniteTransition = rememberInfiniteTransition(label = "simPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "simPulseAlpha",
    )

    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(GwRadii.r1.dp))
            .background(bgColor)
            .border(
                1.dp,
                if (isRunning) borderColor.copy(alpha = pulseAlpha) else borderColor,
                RoundedCornerShape(GwRadii.r1.dp),
            )
            .clickable { onToggle() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Animated dot
            val dotSize by animateDpAsState(
                targetValue = if (isRunning) 8.dp else 6.dp,
                animationSpec = tween(300),
                label = "simDot",
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (isRunning) GwColors.critRed.copy(alpha = pulseAlpha) else GwColors.stateLive),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (isRunning) "■  עצור הדמיה" else "▶  ${stringResource(R.string.simulate)}",
                style = GwTypography.Label.copy(
                    color = labelColor,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                ),
            )
        }
    }
}

// ─── INJURY NOTIFICATION OVERLAY ─────────────────────────────────────────────

@Composable
private fun InjuryNotificationOverlay(
    soldierName: String,
    soldierId: String,
    onDismiss: () -> Unit,
) {
    // Auto-dismiss after 6 seconds
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(6000)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(400)) { -it },
        exit  = fadeOut(tween(500)) + slideOutVertically(tween(400)) { -it },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(480.dp)
                    .clip(RoundedCornerShape(GwRadii.r2.dp))
                    .background(Color(0xFF2A0508))
                    .border(2.dp, GwColors.critRed, RoundedCornerShape(GwRadii.r2.dp))
                    .padding(GwSpacing.sp6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Pulsing warning icon
                val pulse = rememberInfiniteTransition(label = "injPulse")
                val pAlpha by pulse.animateFloat(
                    initialValue = 0.5f, targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                    label = "injAlpha",
                )

                Text(
                    "⚠",
                    style = GwTypography.H1.copy(
                        color = GwColors.critRed.copy(alpha = pAlpha),
                        fontSize = 48.sp,
                    ),
                )
                Spacer(Modifier.height(GwSpacing.sp3.dp))

                Text(
                    "CASUALTY DETECTED",
                    style = GwTypography.H2.copy(
                        color = GwColors.critRed,
                        letterSpacing = 3.sp,
                    ),
                )
                Spacer(Modifier.height(GwSpacing.sp2.dp))

                Text(
                    "$soldierName · $soldierId",
                    style = GwTypography.MonoLg.copy(
                        color = GwColors.fg000,
                        fontSize = 22.sp,
                    ),
                )
                Spacer(Modifier.height(GwSpacing.sp2.dp))

                Text(
                    "TACHYCARDIA · POSSIBLE HEMORRHAGE",
                    style = GwTypography.Mono.copy(color = GwColors.warnAmber),
                )
                Spacer(Modifier.height(GwSpacing.sp4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(GwSpacing.sp4.dp),
                ) {
                    // Vitals preview boxes
                    NotifVitalBox("HR", "185+", GwColors.critRed)
                    NotifVitalBox("BR", "30+", GwColors.warnAmber)
                    NotifVitalBox("SpO₂", "<90%", GwColors.critRed)
                    NotifVitalBox("RISK", "8.0+", GwColors.critRed)
                }

                Spacer(Modifier.height(GwSpacing.sp4.dp))

                Text(
                    "AUTO-FLAGGED · ACKNOWLEDGE ON BANNER",
                    style = GwTypography.Audit.copy(color = GwColors.fg300),
                )
            }
        }
    }
}

@Composable
private fun NotifVitalBox(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(GwRadii.r1.dp))
            .background(GwColors.bg200)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(GwRadii.r1.dp))
            .padding(horizontal = GwSpacing.sp3.dp, vertical = GwSpacing.sp2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = GwTypography.Audit.copy(color = GwColors.fg300))
        Spacer(Modifier.height(2.dp))
        Text(value, style = GwTypography.MonoLg.copy(color = color))
    }
}


// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun nowUtc(): String =
    SimpleDateFormat("HH:mm:ss", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date())

/** Build soldier positions for the map — also used to seed simulation. */
fun buildSoldierPositionsStatic(company: Company, rng: Random): Map<String, Pair<Float, Float>> {
    val result = mutableMapOf<String, Pair<Float, Float>>()
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
                val x = (cx + kotlin.math.cos(angle) * radius).coerceIn(0.05f, 0.95f)
                val y = (cy + kotlin.math.sin(angle) * radius).coerceIn(0.1f,  0.9f)
                result[soldier.id] = x to y
            }
        }
    }
    return result
}
