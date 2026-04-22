package com.gltech.guardianwatch.casualty

import kotlinx.serialization.Serializable
import java.time.Instant

/** NATO/TCCC triage category. UI must pair color with shape glyph (colorblind-safe). */
enum class Triage { MINOR, DELAYED, IMMEDIATE, EXPECTANT }

/** Chain-of-custody stage. */
enum class ChainStage { POI, MEDIC, CASEVAC, ROLE2, ROLE3 }

/** Single vitals frame — what arrives from the watch over BLE. */
@Serializable
data class VitalsFrame(
    val timestampMs: Long,
    val hrBpm: Int?,                // null = sensor dropped
    val spo2Pct: Int?,              // null on Instinct 2 (no SpO2 sensor — see user memory)
    val skinTempC: Float?,          // null on Instinct 2
    val accelMagG: Float?,          // magnitude of 3-axis accel, for movement analysis
    val batteryPct: Int,
    val rssiDbm: Int,
    /** Movement classifier from the watch: 0=still, 1=walk, 2=run, 3=fall, 4=seizure.
     *  null when only the standard HR-fallback channel is active (no Guardian app on the watch). */
    val movementClass: Int? = null,
)

/** Derived health state from the anomaly engine. */
enum class HealthTone { OK, WARN, CRITICAL }

@Serializable
data class VitalAssessment(
    val hrTone: HealthTone,
    val spo2Tone: HealthTone,
    val tempTone: HealthTone,
    val overallTone: HealthTone,
    val suspected: String? = null,  // e.g. "HEMORRHAGE_SUSPECTED"
)

/** A casualty currently under observation. */
@Serializable
data class Casualty(
    val id: String,                          // e.g. "CAS-0147"
    val name: String,                        // e.g. "Weiss, J."
    val age: Int,
    val mgrs: String,                        // grid reference
    val triage: Triage,
    val watchDeviceAddress: String,          // BLE MAC — one watch per casualty
    val openedAtMs: Long,
    val chain: List<ChainStage> = listOf(ChainStage.POI, ChainStage.MEDIC),
    /** Tamper-evident chain of custody. Each entry's contentHash depends on the prior one. */
    val audit: List<AuditEntry> = emptyList(),
)

/** Immutable audit entry — chain-of-custody. Append-only. */
@Serializable
data class AuditEntry(
    val timestampMs: Long,
    val handlerInitials: String,             // e.g. "M.ROSEN"
    val event: String,                        // e.g. "HANDOFF → ROLE-2"
    val contentHash: String,                 // SHA-256 prefix of prior-state + event
)

fun Long.toInstant(): Instant = Instant.ofEpochMilli(this)

/**
 * Append an immutable audit entry to this casualty's chain of custody.
 *
 * The entry's `contentHash` is SHA-256 of `priorHash | timestamp | handler | event`,
 * where `priorHash` is the previous entry's hash or `"GENESIS:<casualtyId>"` if this
 * is the first entry. Any tampering with an earlier entry breaks every subsequent hash.
 */
fun Casualty.withAuditEntry(event: String, handlerInitials: String, nowMs: Long): Casualty {
    val priorHash = audit.lastOrNull()?.contentHash ?: "GENESIS:$id"
    val canonical = "$priorHash|$nowMs|$handlerInitials|$event"
    val digest = java.security.MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray(Charsets.UTF_8))
    val hexHash = digest.joinToString("") { "%02x".format(it) }
    val entry = AuditEntry(
        timestampMs = nowMs,
        handlerInitials = handlerInitials,
        event = event,
        contentHash = hexHash,
    )
    return this.copy(audit = this.audit + entry)
}
