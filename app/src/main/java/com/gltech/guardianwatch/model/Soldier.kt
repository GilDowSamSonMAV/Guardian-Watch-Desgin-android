package com.gltech.guardianwatch.model

/**
 * A single soldier in the unit hierarchy.
 * Matches the reference data schema from data.jsx.
 * Live BLE vitals (hr/br etc.) can be overridden from VitalsRepository for
 * soldiers whose watch is paired — otherwise demo values are used.
 */
data class Soldier(
    val id: String,              // e.g. "2A-02"
    val pos: Int,                // position 1–8 in squad
    val last: String,            // last name e.g. "LEVI"
    val role: SoldierRole,
    val squadId: String,

    // Vitals — demo values; BLE updates override hr at runtime
    val hr: Int,                 // bpm
    val br: Int,                 // breaths per minute
    val spo2: Int,               // % — shows "--" if sensor unavailable
    val coreTemp: Float,         // °C — shows "--" if sensor unavailable

    val risk: Float,             // composite risk score 0.0–10.0
    val status: SoldierStatus,
    val batteryPct: Int,         // watch battery %
    val meshSignal: MeshSignal,
    val lastUpdateSec: Int,      // seconds since last update (staleness)
)

enum class SoldierRole(val display: String) {
    TL("TL"), MED("MED"), RTO("RTO"), SAW("SAW"),
    M203("M203"), DM("DM"), RIF("RIF"),
}

enum class MeshSignal { STRONG, WEAK, NONE }
