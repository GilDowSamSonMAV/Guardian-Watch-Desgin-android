package com.gltech.guardianwatch.model

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.math.*
import kotlin.random.Random

/**
 * Drives the "live" simulation for demo purposes.
 * When running, soldiers' vitals fluctuate, map dots drift, and
 * after a random interval one soldier gets injured (CRITICAL event).
 */
class SimulationEngine {

    var isRunning by mutableStateOf(false)
        private set

    /** Tick counter — incremented every simulation step (~1 s). */
    var tick by mutableStateOf(0L)
        private set

    /** Soldiers whose data has been overridden by simulation. keyed by soldier id. */
    val liveVitals = mutableStateMapOf<String, LiveVitals>()

    /** Map positions — solderId → (x, y) in 0..1 normalised coords. */
    val livePositions = mutableStateMapOf<String, Pair<Float, Float>>()

    /** ID of soldier who got injured during simulation, null if none yet. */
    var injuredSoldierId by mutableStateOf<String?>(null)
        private set

    /** True for one read cycle after injury fires — UI consumes and resets. */
    var injuryNotificationPending by mutableStateOf(false)
        private set

    /** How many ticks until injury event fires (-1 = already fired). */
    private var injuryCountdown = -1

    private val rng = Random(System.currentTimeMillis())

    fun start(allSoldiers: List<Soldier>, positions: Map<String, Pair<Float, Float>>) {
        if (isRunning) return
        isRunning = true
        tick = 0
        injuredSoldierId = null
        injuryNotificationPending = false

        // Seed live vitals from current static data
        allSoldiers.forEach { s ->
            liveVitals[s.id] = LiveVitals(
                hr = s.hr,
                br = s.br,
                spo2 = s.spo2,
                coreTemp = s.coreTemp,
                risk = s.risk,
                status = s.status,
                lastUpdateSec = s.lastUpdateSec,
                batteryPct = s.batteryPct,
            )
        }

        // Seed positions
        livePositions.clear()
        livePositions.putAll(positions)

        // Schedule injury event between 15 and 40 seconds from now
        injuryCountdown = rng.nextInt(15, 41)
    }

    fun stop() {
        isRunning = false
        tick = 0
        liveVitals.clear()
        livePositions.clear()
        injuredSoldierId = null
        injuryNotificationPending = false
        injuryCountdown = -1
    }

    fun consumeInjuryNotification() {
        injuryNotificationPending = false
    }

    /**
     * Called every ~1 second while simulation is running.
     * Drifts vitals slightly, moves positions, and handles injury countdown.
     */
    fun step() {
        if (!isRunning) return
        tick++

        // ── Drift all vitals slightly ───────────────────────────────────
        val vitalUpdates = mutableMapOf<String, LiveVitals>()
        liveVitals.forEach { (id, v) ->
            if (id == injuredSoldierId) return@forEach // injured soldier handled separately

            val hrDelta = rng.nextInt(-3, 4)
            val brDelta = if (rng.nextFloat() > 0.6f) rng.nextInt(-1, 2) else 0
            val newHr = (v.hr + hrDelta).coerceIn(55, 120)
            val newBr = (v.br + brDelta).coerceIn(10, 22)
            val newLastUpdate = maxOf(0, v.lastUpdateSec + rng.nextInt(-2, 2))

            vitalUpdates[id] = v.copy(
                hr = newHr,
                br = newBr,
                lastUpdateSec = newLastUpdate,
            )
        }
        liveVitals.putAll(vitalUpdates)

        // ── Drift positions ─────────────────────────────────────────────
        val posUpdates = mutableMapOf<String, Pair<Float, Float>>()
        livePositions.forEach { (id, pos) ->
            val dx = (rng.nextFloat() - 0.5f) * 0.006f
            val dy = (rng.nextFloat() - 0.5f) * 0.006f
            val nx = (pos.first + dx).coerceIn(0.05f, 0.95f)
            val ny = (pos.second + dy).coerceIn(0.10f, 0.90f)
            posUpdates[id] = nx to ny
        }
        livePositions.putAll(posUpdates)

        // ── Injury countdown ────────────────────────────────────────────
        if (injuryCountdown > 0) {
            injuryCountdown--
            if (injuryCountdown == 0) {
                triggerInjury()
                injuryCountdown = -1
            }
        }

        // ── Worsen injured soldier vitals over time ─────────────────────
        injuredSoldierId?.let { id ->
            val v = liveVitals[id] ?: return@let
            val ticksSinceInjury = tick
            val newHr = (v.hr + rng.nextInt(0, 3)).coerceIn(140, 200)
            val newBr = (v.br + if (rng.nextFloat() > 0.5f) 1 else 0).coerceIn(28, 44)
            val newSpo2 = (v.spo2 - if (rng.nextFloat() > 0.7f) 1 else 0).coerceIn(78, 95)
            val newTemp = (v.coreTemp + rng.nextFloat() * 0.05f).coerceAtMost(40.0f)
            val newRisk = (v.risk + rng.nextFloat() * 0.1f).coerceAtMost(10.0f)
            liveVitals[id] = v.copy(
                hr = newHr,
                br = newBr,
                spo2 = newSpo2,
                coreTemp = newTemp,
                risk = newRisk,
                status = SoldierStatus.CRITICAL,
                lastUpdateSec = 0,
            )
        }
    }

    private fun triggerInjury() {
        // Pick a random OK soldier
        val candidates = liveVitals.filter { it.value.status == SoldierStatus.OK }
        if (candidates.isEmpty()) return
        val targetId = candidates.keys.random(rng)
        injuredSoldierId = targetId
        injuryNotificationPending = true

        // Immediately spike their vitals
        val v = liveVitals[targetId] ?: return
        liveVitals[targetId] = v.copy(
            hr = 165 + rng.nextInt(0, 25),
            br = 30 + rng.nextInt(0, 6),
            spo2 = 85 + rng.nextInt(0, 5),
            coreTemp = 38.2f + rng.nextFloat() * 0.6f,
            risk = 7.5f + rng.nextFloat() * 1.5f,
            status = SoldierStatus.CRITICAL,
        )
    }
}

/**
 * Snapshot of a soldier's vitals as mutated by the simulation engine.
 */
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
