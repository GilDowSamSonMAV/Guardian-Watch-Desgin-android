package com.gltech.guardianwatch.ble

import android.content.Context
import android.content.SharedPreferences
import com.gltech.guardianwatch.anomaly.AnomalyDetector
import com.gltech.guardianwatch.casualty.Casualty
import com.gltech.guardianwatch.casualty.VitalAssessment
import com.gltech.guardianwatch.casualty.VitalsFrame
import com.gltech.guardianwatch.casualty.withAuditEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
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
class VitalsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val _streams = MutableStateFlow<Map<String, CasualtyStream>>(emptyMap())
    val streams: StateFlow<Map<String, CasualtyStream>> = _streams.asStateFlow()

    private val detectors = mutableMapOf<String, AnomalyDetector>()

    private val HR_HISTORY_WINDOW_MS = 10 * 60_000L

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("vitals_prefs", Context.MODE_PRIVATE)
    }
    private val auditLogFile: File by lazy {
        File(context.filesDir, "audit_log.jsonl")
    }

    fun registerCasualty(casualty: Casualty) {
        _streams.update { it + (casualty.id to CasualtyStream(casualty)) }
        detectors[casualty.id] = AnomalyDetector()
        saveCasualtyToDisk(casualty)
    }

    fun removeCasualty(casualtyId: String) {
        _streams.update { it - casualtyId }
        detectors.remove(casualtyId)
        removeCasualtyFromDisk(casualtyId)
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
     * Also persists the updated casualty and appends a JSON line to the audit log file.
     */
    fun appendAudit(casualtyId: String, event: String, handlerInitials: String) {
        var updated: Casualty? = null
        _streams.update { current ->
            val existing = current[casualtyId] ?: return@update current
            val nowMs = System.currentTimeMillis()
            val updatedCasualty = existing.casualty.withAuditEntry(event, handlerInitials, nowMs)
            updated = updatedCasualty
            current + (casualtyId to existing.copy(casualty = updatedCasualty))
        }
        updated?.let { casualty ->
            saveCasualtyToDisk(casualty)
            val entry = casualty.audit.lastOrNull()
            val logLine = buildString {
                append("{\"casualtyId\":")
                append(json.encodeToString(casualtyId))
                append(",\"timestamp\":")
                append(entry?.timestampMs ?: 0L)
                append(",\"handler\":")
                append(json.encodeToString(handlerInitials))
                append(",\"event\":")
                append(json.encodeToString(event))
                append(",\"hash\":")
                append(json.encodeToString(entry?.contentHash ?: ""))
                append("}")
            }
            try { auditLogFile.appendText(logLine + "\n") } catch (_: Throwable) {}
        }
    }

    fun loadPersistedCasualties(): List<Casualty> {
        val ids = prefs.getStringSet(KEY_IDS, emptySet()) ?: emptySet()
        return ids.mapNotNull { id ->
            prefs.getString("casualty_$id", null)?.let { encoded ->
                runCatching { json.decodeFromString<Casualty>(encoded) }.getOrNull()
            }
        }
    }

    private fun saveCasualtyToDisk(casualty: Casualty) {
        val encoded = json.encodeToString(casualty)
        val ids = (prefs.getStringSet(KEY_IDS, emptySet()) ?: emptySet()).toMutableSet()
        ids += casualty.id
        prefs.edit()
            .putString("casualty_${casualty.id}", encoded)
            .putStringSet(KEY_IDS, ids)
            .apply()
    }

    private fun removeCasualtyFromDisk(casualtyId: String) {
        val ids = (prefs.getStringSet(KEY_IDS, emptySet()) ?: emptySet()).toMutableSet()
        ids -= casualtyId
        prefs.edit()
            .remove("casualty_$casualtyId")
            .putStringSet(KEY_IDS, ids)
            .apply()
    }

    companion object {
        private const val KEY_IDS = "casualty_ids"
    }
}
