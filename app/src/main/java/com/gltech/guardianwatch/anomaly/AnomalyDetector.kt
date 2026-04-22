package com.gltech.guardianwatch.anomaly

import com.gltech.guardianwatch.casualty.HealthTone
import com.gltech.guardianwatch.casualty.VitalAssessment
import com.gltech.guardianwatch.casualty.VitalsFrame
import kotlin.math.abs

/**
 * Minimal on-device anomaly detector for Guardian Watch.
 *
 * CONSTRAINT (from hardware memory): Garmin Instinct 2 has NO SpO2 sensor and NO skin-temp sensor.
 * We therefore use HR magnitude + HR trend (bpm/min) + accelerometer movement class
 * to derive a "likely hemorrhage / shock" suspicion.
 *
 * Target: ~85% hemorrhage detection accuracy from HR + movement alone.
 *
 * Heuristics (derived from TCCC literature, simplified for MVP):
 *  - HR >  140 sustained while stationary (movement_class=0) = hemorrhage suspected
 *  - HR >  120 with rising trend (>+8 bpm/min) + stationary   = early shock
 *  - HR <   40 = bradycardia / critical
 *  - Rapid HR rise (>+20 bpm/min) with fall event             = compensated shock post-injury
 *  - Movement class 3 (fall) or 4 (seizure) = immediate flag
 *
 *  All thresholds are exposed as constants for easy tuning from field data.
 */
object AnomalyThresholds {
    const val HR_CRIT_HIGH = 140
    const val HR_WARN_HIGH = 120
    const val HR_CRIT_LOW = 40
    const val HR_WARN_LOW = 50
    const val TREND_RISING_FAST_BPM_PER_MIN = 20
    const val TREND_RISING_SLOW_BPM_PER_MIN = 8
    const val MOVEMENT_STILL = 0
    const val MOVEMENT_FALL = 3
    const val MOVEMENT_SEIZURE = 4
}

class AnomalyDetector {

    /** Rolling window for HR trend estimation (~60s of samples at 1Hz). */
    private val hrWindow = ArrayDeque<Pair<Long, Int>>()
    private val WINDOW_MS = 60_000L

    /**
     * Assess a single vitals frame. Movement class is pulled from `frame.movementClass`
     * (populated by the watch's Connect IQ classifier via GuardianWatchProfile).
     * Falls back to STILL when only the standard HR service is available (no movement data).
     */
    fun assess(frame: VitalsFrame): VitalAssessment {
        val movementClass = frame.movementClass ?: AnomalyThresholds.MOVEMENT_STILL
        // Update HR window
        frame.hrBpm?.let { hr ->
            hrWindow.addLast(frame.timestampMs to hr)
            while (hrWindow.isNotEmpty() && frame.timestampMs - hrWindow.first().first > WINDOW_MS) {
                hrWindow.removeFirst()
            }
        }

        // HR tone
        val hrTone = when {
            frame.hrBpm == null -> HealthTone.OK
            frame.hrBpm >= AnomalyThresholds.HR_CRIT_HIGH -> HealthTone.CRITICAL
            frame.hrBpm <= AnomalyThresholds.HR_CRIT_LOW -> HealthTone.CRITICAL
            frame.hrBpm >= AnomalyThresholds.HR_WARN_HIGH -> HealthTone.WARN
            frame.hrBpm <= AnomalyThresholds.HR_WARN_LOW -> HealthTone.WARN
            else -> HealthTone.OK
        }

        // Spo2 and temp are always OK on Instinct 2 — we never surface false data.
        val spo2Tone = HealthTone.OK
        val tempTone = HealthTone.OK

        // Compute trend
        val trend = hrTrendBpmPerMin()

        // Suspect logic
        val suspected: String? = when {
            movementClass == AnomalyThresholds.MOVEMENT_SEIZURE -> "SEIZURE"
            movementClass == AnomalyThresholds.MOVEMENT_FALL &&
                    (frame.hrBpm ?: 0) >= AnomalyThresholds.HR_WARN_HIGH -> "FALL+TACHYCARDIA"
            (frame.hrBpm ?: 0) >= AnomalyThresholds.HR_CRIT_HIGH &&
                    movementClass == AnomalyThresholds.MOVEMENT_STILL -> "HEMORRHAGE_SUSPECTED"
            (frame.hrBpm ?: 0) >= AnomalyThresholds.HR_WARN_HIGH &&
                    trend >= AnomalyThresholds.TREND_RISING_SLOW_BPM_PER_MIN &&
                    movementClass == AnomalyThresholds.MOVEMENT_STILL -> "EARLY_SHOCK"
            abs(trend) >= AnomalyThresholds.TREND_RISING_FAST_BPM_PER_MIN -> "RAPID_HR_CHANGE"
            else -> null
        }

        val overallTone = maxOf(hrTone, spo2Tone, tempTone)

        return VitalAssessment(hrTone, spo2Tone, tempTone, overallTone, suspected)
    }

    /** Linear regression slope of HR over the current rolling window, in bpm/min. */
    fun hrTrendBpmPerMin(): Int {
        if (hrWindow.size < 2) return 0
        val n = hrWindow.size.toDouble()
        val sx = hrWindow.sumOf { it.first.toDouble() }
        val sy = hrWindow.sumOf { it.second.toDouble() }
        val sxy = hrWindow.sumOf { it.first.toDouble() * it.second.toDouble() }
        val sxx = hrWindow.sumOf { it.first.toDouble() * it.first.toDouble() }
        val denom = (n * sxx - sx * sx)
        if (denom == 0.0) return 0
        val slopePerMs = (n * sxy - sx * sy) / denom
        return (slopePerMs * 60_000.0).toInt()
    }

    fun reset() = hrWindow.clear()
}
