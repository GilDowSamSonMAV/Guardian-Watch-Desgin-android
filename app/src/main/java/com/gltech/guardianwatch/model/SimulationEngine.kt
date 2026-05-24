package com.gltech.guardianwatch.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random
import org.osmdroid.util.GeoPoint

/**
 * Drives the "live" simulation for demo purposes.
 *
 * - On [start]: every soldier is forced to healthy status so the demo begins clean.
 * - After 15-30 s the first injury fires; subsequent injuries follow every 20-40 s.
 * - Every injured soldier's vitals worsen continuously each tick.
 * - [pendingNotifications] accumulates injury IDs until the UI calls [dismissNotification].
 */
class SimulationEngine {

    var isRunning by mutableStateOf(false)
        private set

    var tick by mutableStateOf(0L)
        private set

    val liveVitals       = mutableStateMapOf<String, LiveVitals>()
    val livePositions    = mutableStateMapOf<String, Pair<Float, Float>>()
    val liveGeoPositions = mutableStateMapOf<String, GeoPoint>()

    /** All soldiers who have been injured (vitals keep worsening each tick). */
    val injuredSoldierIds = mutableStateListOf<String>()

    /** Injuries the user hasn't closed yet — drives the notification panel. */
    val pendingNotifications = mutableStateListOf<String>()

    // Legacy single-value kept for backward compat
    var injuredSoldierId by mutableStateOf<String?>(null)
        private set
    var injuryNotificationPending by mutableStateOf(false)
        private set

    private var injuryCountdown = -1
    private val rng = Random(System.currentTimeMillis())

    fun start(
        allSoldiers: List<Soldier>,
        positions: Map<String, Pair<Float, Float>>,
        geoPositions: Map<String, GeoPoint> = emptyMap(),
    ) {
        if (isRunning) return
        isRunning = true
        tick = 0
        injuredSoldierId = null
        injuryNotificationPending = false
        injuredSoldierIds.clear()
        pendingNotifications.clear()

        // Force EVERYONE to healthy status — the demo starts clean
        allSoldiers.forEach { s ->
            liveVitals[s.id] = LiveVitals(
                hr            = rng.nextInt(62, 86),
                br            = rng.nextInt(13, 19),
                spo2          = rng.nextInt(97, 100),
                coreTemp      = 36.2f + rng.nextFloat() * 0.8f,
                risk          = rng.nextFloat() * 1.5f,
                status        = SoldierStatus.OK,
                lastUpdateSec = rng.nextInt(2, 16),
                batteryPct    = s.batteryPct,
            )
        }

        livePositions.clear()
        livePositions.putAll(positions)

        liveGeoPositions.clear()
        liveGeoPositions.putAll(geoPositions)

        injuryCountdown = rng.nextInt(15, 31)   // first injury in 15–30 s
    }

    fun stop() {
        isRunning = false
        tick = 0
        liveVitals.clear()
        livePositions.clear()
        liveGeoPositions.clear()
        injuredSoldierId = null
        injuryNotificationPending = false
        injuryCountdown = -1
        injuredSoldierIds.clear()
        pendingNotifications.clear()
    }

    fun consumeInjuryNotification() {
        injuryNotificationPending = false
    }

    /** Called when the user closes one notification card. */
    fun dismissNotification(soldierId: String) {
        pendingNotifications.remove(soldierId)
    }

    fun step() {
        if (!isRunning) return
        tick++

        // Drift healthy soldiers slightly
        val vitalUpdates = mutableMapOf<String, LiveVitals>()
        liveVitals.forEach { (id, v) ->
            if (id in injuredSoldierIds) return@forEach
            vitalUpdates[id] = v.copy(
                hr            = (v.hr + rng.nextInt(-3, 4)).coerceIn(55, 120),
                br            = (v.br + if (rng.nextFloat() > 0.6f) rng.nextInt(-1, 2) else 0).coerceIn(10, 22),
                lastUpdateSec = maxOf(0, v.lastUpdateSec + rng.nextInt(-2, 2)),
            )
        }
        liveVitals.putAll(vitalUpdates)

        // Worsen ALL injured soldiers each tick
        injuredSoldierIds.forEach { id ->
            val v = liveVitals[id] ?: return@forEach
            liveVitals[id] = v.copy(
                hr       = (v.hr + rng.nextInt(0, 3)).coerceIn(140, 200),
                br       = (v.br + if (rng.nextFloat() > 0.5f) 1 else 0).coerceIn(28, 44),
                spo2     = (v.spo2 - if (rng.nextFloat() > 0.7f) 1 else 0).coerceIn(78, 95),
                coreTemp = (v.coreTemp + rng.nextFloat() * 0.05f).coerceAtMost(40.0f),
                risk     = (v.risk + rng.nextFloat() * 0.1f).coerceAtMost(10.0f),
                status   = SoldierStatus.CRITICAL,
                lastUpdateSec = 0,
            )
        }

        // Drift canvas positions (used by TacMap overlay)
        val posUpdates = mutableMapOf<String, Pair<Float, Float>>()
        livePositions.forEach { (id, pos) ->
            val dx = (rng.nextFloat() - 0.5f) * 0.006f
            val dy = (rng.nextFloat() - 0.5f) * 0.006f
            posUpdates[id] = (pos.first + dx).coerceIn(0.05f, 0.95f) to
                             (pos.second + dy).coerceIn(0.10f, 0.90f)
        }
        livePositions.putAll(posUpdates)

        // Drift GeoPoint positions — healthy soldiers move, injured stay still
        val geoUpdates = mutableMapOf<String, GeoPoint>()
        liveGeoPositions.forEach { (id, pos) ->
            if (id in injuredSoldierIds) return@forEach
            val dLat = (rng.nextFloat() - 0.5f) * 0.0004
            val dLon = (rng.nextFloat() - 0.5f) * 0.0004
            geoUpdates[id] = GeoPoint(pos.latitude + dLat, pos.longitude + dLon)
        }
        liveGeoPositions.putAll(geoUpdates)

        // Injury countdown
        if (injuryCountdown > 0) {
            injuryCountdown--
            if (injuryCountdown == 0) triggerInjury()
        }
    }

    private fun triggerInjury() {
        val candidates = liveVitals.filter { (id, v) ->
            v.status == SoldierStatus.OK && id !in injuredSoldierIds
        }
        if (candidates.isEmpty()) return

        val targetId = candidates.keys.random(rng)
        injuredSoldierIds.add(targetId)
        pendingNotifications.add(targetId)
        injuredSoldierId = targetId
        injuryNotificationPending = true

        val v = liveVitals[targetId] ?: return
        liveVitals[targetId] = v.copy(
            hr       = 165 + rng.nextInt(0, 25),
            br       = 30 + rng.nextInt(0, 6),
            spo2     = 85 + rng.nextInt(0, 5),
            coreTemp = 38.2f + rng.nextFloat() * 0.6f,
            risk     = 7.5f + rng.nextFloat() * 1.5f,
            status   = SoldierStatus.CRITICAL,
        )

        // Schedule the next injury if healthy soldiers still remain
        val moreHealthy = liveVitals.any { (id, v2) ->
            v2.status == SoldierStatus.OK && id !in injuredSoldierIds
        }
        injuryCountdown = if (moreHealthy) rng.nextInt(20, 41) else -1
    }
}

data class LiveVitals(
    val hr: Int,
    val br: Int,
    val spo2: Int,
    val coreTemp: Float,
    val risk: Float,
    val status: SoldierStatus,
    val lastUpdateSec: Int = 0,
    val batteryPct: Int = 80,
)
