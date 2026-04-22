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
