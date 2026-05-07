package com.gltech.guardianwatch.model

data class Squad(
    val id: String,       // e.g. "2A"
    val name: String,     // e.g. "Delta"
    val platoonId: String,
    val soldiers: List<Soldier>,
)

data class Platoon(
    val id: String,       // e.g. "PLT-2"
    val name: String,     // e.g. "Platoon 2"
    val callsign: String, // e.g. "ALEPH-2"
    val sector: String,   // e.g. "GRID 18S TJ 4486 8741"
    val squads: List<Squad>,
)

data class Company(
    val id: String,       // e.g. "ALEPH"
    val callsign: String, // e.g. "ALEPH-6"
    val name: String,     // e.g. "Aleph Company"
    val platoons: List<Platoon>,
)

/** Active critical medical alert — drives the global red banner. */
data class CriticalAlert(
    val soldierId: String,       // e.g. "2A-02"
    val type: String,            // e.g. "VITALS"
    val message: String,         // e.g. "TACHYCARDIA · POSSIBLE HEMORRHAGE"
    val triggeredSec: Int,       // seconds since trigger
    val acknowledged: Boolean = false,
)
