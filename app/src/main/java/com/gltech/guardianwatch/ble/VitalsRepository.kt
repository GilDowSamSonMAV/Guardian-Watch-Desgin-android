package com.gltech.guardianwatch.ble

import com.gltech.guardianwatch.anomaly.AnomalyDetector
import com.gltech.guardianwatch.casualty.Casualty
import com.gltech.guardianwatch.casualty.VitalAssessment
import com.gltech.guardianwatch.casualty.VitalsFrame
import com.gltech.guardianwatch.casualty.withAuditEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-casualty live stream: latest vitals frame + running anomaly assessment +
 * a short ring-buffer of recent HR samples for the chart.
 */
data class CasualtyStream(
    val casualty: Casualty,
    val latest: VitalsFrame? = null,
    val assessment: VitalAssessment? = null,
    /** Last ~10 minutes of HR samples for the chart, evicted as new arrive. */
    val hrHistory: List<Pair<Long, Int>> = emptyList(),
)

/**
 * Central repository fanning vitals from [WatchBleManager]s into per-casualty state.
 *
 * Call sites:
 *  - [BleService] creates one [WatchBleManager] per connected watch, registers via [attachWatch].
 *  - UI reads [streams] as StateFlow.
 */
@Singleton
class VitalsRepository @Inject constructor() {

    private val _streams = MutableStateFlow<Map<String, CasualtyStream>>(emptyMap())
    val streams: StateFlow<Map<String, CasualtyStream>> = _streams.asStateFlow()

    private val detectors = mutableMapOf<String, AnomalyDetector>()

    private val HR_HISTORY_WINDOW_MS = 10 * 60_000L

    fun registerCasualty(casualty: Casualty) {
        _streams.update { it + (casualty.id to CasualtyStream(casualty)) }
        detectors[casualty.id] = AnomalyDetector()
    }

    fun removeCasualty(casualtyId: String) {
        _streams.update { it - casualtyId }
        detectors.remove(casualtyId)
    }

    /** Wire a BLE manager's vitals flow into this repository for a given casualty. */
    fun attachWatch(scope: CoroutineScope, casualtyId: String, manager: WatchBleManager) {
        scope.launch {
            manager.vitals.collect { frame ->
                onFrame(casualtyId, frame)
            }
        }
    }

    private fun onFrame(casualtyId: String, frame: VitalsFrame) {
        val detector = detectors[casualtyId] ?: return
        val assessment = detector.assess(frame)
        _streams.update { current ->
            val existing = current[casualtyId] ?: return@update current
            val hrSample = frame.hrBpm?.let { frame.timestampMs to it }
            val newHistory = if (hrSample != null) {
                (existing.hrHistory + hrSample).dropWhile { frame.timestampMs - it.first > HR_HISTORY_WINDOW_MS }
            } else existing.hrHistory

            current + (casualtyId to existing.copy(
                latest = frame,
                assessment = assessment,
                hrHistory = newHistory,
            ))
        }
    }

    /**
     * Append an immutable, hash-chained audit entry to the casualty's chain of custody.
     * Safe to call from any thread — `StateFlow.update` is CAS-based. No-op if the
     * casualty isn't registered.
     */
    fun appendAudit(casualtyId: String, event: String, handlerInitials: String) {
        _streams.update { current ->
            val existing = current[casualtyId] ?: return@update current
            val nowMs = System.currentTimeMillis()
            val updatedCasualty = existing.casualty.withAuditEntry(event, handlerInitials, nowMs)
            current + (casualtyId to existing.copy(casualty = updatedCasualty))
        }
    }
}
