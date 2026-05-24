package com.gltech.guardianwatch.ble

import com.gltech.guardianwatch.anomaly.AnomalyDetector
import com.gltech.guardianwatch.casualty.Casualty
import com.gltech.guardianwatch.casualty.VitalAssessment
import com.gltech.guardianwatch.casualty.VitalsFrame
import com.gltech.guardianwatch.casualty.withAuditEntry
import com.gltech.guardianwatch.db.CasualtyDao
import com.gltech.guardianwatch.db.CasualtyEntity
import com.gltech.guardianwatch.db.VitalSignsDao
import com.gltech.guardianwatch.db.VitalSignsEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
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
    val hrHistory: List<Pair<Long, Int>> = emptyList(),
)

/**
 * Central repository fanning vitals from [WatchBleManager]s into per-casualty state.
 * Persists both casualties and vital-signs history to Room (SQLite).
 */
@Singleton
class VitalsRepository @Inject constructor(
    private val vitalsDao: VitalSignsDao,
    private val casualtyDao: CasualtyDao,
) {
    private val _streams = MutableStateFlow<Map<String, CasualtyStream>>(emptyMap())
    val streams: StateFlow<Map<String, CasualtyStream>> = _streams.asStateFlow()

    private val detectors = mutableMapOf<String, AnomalyDetector>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val HR_HISTORY_WINDOW_MS = 10 * 60_000L
    private val json = Json { ignoreUnknownKeys = true }

    fun registerCasualty(casualty: Casualty) {
        _streams.update { it + (casualty.id to CasualtyStream(casualty)) }
        detectors[casualty.id] = AnomalyDetector()
        scope.launch { saveCasualty(casualty) }
    }

    fun removeCasualty(casualtyId: String) {
        _streams.update { it - casualtyId }
        detectors.remove(casualtyId)
        scope.launch { casualtyDao.delete(casualtyId) }
    }

    /** Wire a BLE manager's vitals flow into this repository for a given casualty. */
    fun attachWatch(coroutineScope: CoroutineScope, casualtyId: String, manager: WatchBleManager) {
        coroutineScope.launch {
            manager.vitals.collect { frame -> onFrame(casualtyId, frame) }
        }
    }

    private fun onFrame(casualtyId: String, frame: VitalsFrame) {
        val detector = detectors[casualtyId] ?: return
        val assessment = detector.assess(frame)

        _streams.update { current ->
            val existing = current[casualtyId] ?: return@update current
            val hrSample = frame.hrBpm?.let { frame.timestampMs to it }
            val newHistory = if (hrSample != null) {
                (existing.hrHistory + hrSample)
                    .dropWhile { frame.timestampMs - it.first > HR_HISTORY_WINDOW_MS }
            } else existing.hrHistory

            current + (casualtyId to existing.copy(
                latest = frame,
                assessment = assessment,
                hrHistory = newHistory,
            ))
        }

        // Persist to Room in background
        scope.launch {
            val assessmentJson = assessment?.let { json.encodeToString(it) }
            vitalsDao.insert(VitalSignsEntity.from(casualtyId, frame, assessmentJson))
        }
    }

    fun appendAudit(casualtyId: String, event: String, handlerInitials: String) {
        var updated: Casualty? = null
        _streams.update { current ->
            val existing = current[casualtyId] ?: return@update current
            val updatedCasualty = existing.casualty.withAuditEntry(
                event, handlerInitials, System.currentTimeMillis()
            )
            updated = updatedCasualty
            current + (casualtyId to existing.copy(casualty = updatedCasualty))
        }
        updated?.let { casualty ->
            scope.launch { saveCasualty(casualty) }
        }
    }

    /** Load all casualties from Room on startup (replaces SharedPreferences). */
    suspend fun loadPersistedCasualties(): List<Casualty> =
        casualtyDao.all().mapNotNull { entity ->
            runCatching { json.decodeFromString<Casualty>(entity.json) }.getOrNull()
        }

    private suspend fun saveCasualty(casualty: Casualty) {
        casualtyDao.upsert(
            CasualtyEntity(
                id          = casualty.id,
                json        = json.encodeToString(casualty),
                updatedAtMs = System.currentTimeMillis(),
            )
        )
    }

    /** Expose recent vitals as a Flow for the HR chart. */
    fun recentVitals(casualtyId: String, limit: Int = 60) =
        vitalsDao.recentForCasualty(casualtyId, limit)
}
