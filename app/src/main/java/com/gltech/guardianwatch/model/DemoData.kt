package com.gltech.guardianwatch.model

import kotlin.math.abs
import kotlin.random.Random

/**
 * Demo data seeded from the reference data.jsx file.
 * All 3 platoons × 3 squads × 8 soldiers = 72 personnel, matching the reference exactly.
 * Used for demo/MVP. BLE live data overrides individual soldiers' hr field at runtime.
 */
object DemoData {

    // Random seed for repeatable demo
    private val rng = Random(42)

    private fun soldier(
        squadId: String,
        pos: Int,
        last: String,
        role: SoldierRole,
        status: SoldierStatus = SoldierStatus.OK,
        hr: Int = 70 + abs(rng.nextInt()) % 20,
        br: Int = 13 + abs(rng.nextInt()) % 4,
        spo2: Int = 96 + abs(rng.nextInt()) % 3,
        coreTemp: Float = (367 + abs(rng.nextInt()) % 4) / 10f,
        risk: Float = ((abs(rng.nextInt()) % 15 + 4) / 10f),
        battery: Int = 70 + abs(rng.nextInt()) % 25,
        signal: MeshSignal = if (rng.nextFloat() > 0.15f) MeshSignal.STRONG else MeshSignal.WEAK,
        lastUpdate: Int = abs(rng.nextInt()) % 40,
    ) = Soldier(
        id = "$squadId-${pos.toString().padStart(2, '0')}",
        pos = pos,
        last = last,
        role = role,
        squadId = squadId,
        hr = hr,
        br = br,
        spo2 = spo2,
        coreTemp = coreTemp,
        risk = risk,
        status = status,
        batteryPct = battery,
        meshSignal = signal,
        lastUpdateSec = lastUpdate,
    )

    val COMPANY = Company(
        id = "ALEPH",
        callsign = "ALEPH-6",
        name = "Aleph Company",
        platoons = listOf(
            // ── PLATOON 1 ───────────────────────────────────────────────────
            Platoon(
                id = "PLT-1", name = "Platoon 1", callsign = "ALEPH-1",
                sector = "GRID 18S TJ 4421 8896",
                squads = listOf(
                    Squad("1A", "Alpha", "PLT-1", listOf(
                        soldier("1A", 1, "AMAR",   SoldierRole.TL),
                        soldier("1A", 2, "DAGAN",  SoldierRole.RIF),
                        soldier("1A", 3, "OREN",   SoldierRole.MED),
                        soldier("1A", 4, "TAL",    SoldierRole.RTO),
                        soldier("1A", 5, "ELIAS",  SoldierRole.SAW),
                        soldier("1A", 6, "BENI",   SoldierRole.M203),
                        soldier("1A", 7, "ROZEN",  SoldierRole.RIF),
                        soldier("1A", 8, "KATZ",   SoldierRole.RIF),
                    )),
                    Squad("1B", "Bravo", "PLT-1", listOf(
                        soldier("1B", 1, "ALMOG",  SoldierRole.TL),
                        soldier("1B", 2, "GAL",    SoldierRole.RIF),
                        soldier("1B", 3, "HADAR",  SoldierRole.MED),
                        soldier("1B", 4, "VARDI",  SoldierRole.RTO),
                        soldier("1B", 5, "ZAKEN",  SoldierRole.SAW,
                            status = SoldierStatus.CAUTION, hr = 128, br = 22, risk = 4.1f),
                        soldier("1B", 6, "HIRSH",  SoldierRole.M203),
                        soldier("1B", 7, "SADEH",  SoldierRole.RIF),
                        soldier("1B", 8, "PELED",  SoldierRole.RIF),
                    )),
                    Squad("1C", "Charlie", "PLT-1", listOf(
                        soldier("1C", 1, "AVITAL", SoldierRole.TL),
                        soldier("1C", 2, "BARAM",  SoldierRole.RIF),
                        soldier("1C", 3, "GINOSAR",SoldierRole.MED),
                        soldier("1C", 4, "DAYAN",  SoldierRole.RTO),
                        soldier("1C", 5, "EITAN",  SoldierRole.SAW),
                        soldier("1C", 6, "FRIED",  SoldierRole.M203),
                        soldier("1C", 7, "GUTMAN", SoldierRole.RIF),
                        soldier("1C", 8, "HERZL",  SoldierRole.DM),
                    )),
                )
            ),
            // ── PLATOON 2 ───────────────────────────────────────────────────
            Platoon(
                id = "PLT-2", name = "Platoon 2", callsign = "ALEPH-2",
                sector = "GRID 18S TJ 4486 8741",
                squads = listOf(
                    Squad("2A", "Delta", "PLT-2", listOf(
                        soldier("2A", 1, "RAVID",  SoldierRole.TL),
                        soldier("2A", 2, "LEVI",   SoldierRole.RIF,
                            status   = SoldierStatus.CRITICAL,
                            hr       = 187, br = 30,
                            spo2     = 88,  coreTemp = 38.4f,
                            risk     = 8.8f,
                            battery  = 83,  signal   = MeshSignal.STRONG,
                            lastUpdate = 12,
                        ),
                        soldier("2A", 3, "COHEN",  SoldierRole.MED),
                        soldier("2A", 4, "BARAK",  SoldierRole.RTO),
                        soldier("2A", 5, "SHANI",  SoldierRole.SAW,
                            status = SoldierStatus.CAUTION, hr = 134, br = 24, risk = 4.6f),
                        soldier("2A", 6, "AMIR",   SoldierRole.M203),
                        soldier("2A", 7, "GOLAN",  SoldierRole.RIF),
                        soldier("2A", 8, "PERETZ", SoldierRole.RIF,
                            status = SoldierStatus.HIGH, hr = 152, br = 26, risk = 6.2f),
                    )),
                    Squad("2B", "Echo", "PLT-2", listOf(
                        soldier("2B", 1, "MIZRAHI",SoldierRole.TL),
                        soldier("2B", 2, "NIR",    SoldierRole.RIF),
                        soldier("2B", 3, "OZ",     SoldierRole.MED),
                        soldier("2B", 4, "PINI",   SoldierRole.RTO),
                        soldier("2B", 5, "RAZ",    SoldierRole.SAW),
                        soldier("2B", 6, "SAGI",   SoldierRole.M203),
                        soldier("2B", 7, "TALMI",  SoldierRole.RIF),
                        soldier("2B", 8, "URI",    SoldierRole.RIF,
                            status = SoldierStatus.OFFLINE, risk = 0f,
                            lastUpdate = 252, signal = MeshSignal.NONE),
                    )),
                    Squad("2C", "Foxtrot", "PLT-2", listOf(
                        soldier("2C", 1, "YAARI",  SoldierRole.TL),
                        soldier("2C", 2, "ZAITSEV",SoldierRole.RIF),
                        soldier("2C", 3, "ARIEL",  SoldierRole.MED),
                        soldier("2C", 4, "BACHAR", SoldierRole.RTO),
                        soldier("2C", 5, "CARMI",  SoldierRole.SAW),
                        soldier("2C", 6, "DRORI",  SoldierRole.M203),
                        soldier("2C", 7, "EZRA",   SoldierRole.RIF),
                        soldier("2C", 8, "FELDMAN",SoldierRole.DM),
                    )),
                )
            ),
            // ── PLATOON 3 ───────────────────────────────────────────────────
            Platoon(
                id = "PLT-3", name = "Platoon 3", callsign = "ALEPH-3",
                sector = "GRID 18S TJ 4538 8804",
                squads = listOf(
                    Squad("3A", "Golf", "PLT-3", listOf(
                        soldier("3A", 1, "GAVRIEL",SoldierRole.TL),
                        soldier("3A", 2, "HAREL",  SoldierRole.RIF),
                        soldier("3A", 3, "ITAMAR", SoldierRole.MED),
                        soldier("3A", 4, "JONAH",  SoldierRole.RTO),
                        soldier("3A", 5, "KEDEM",  SoldierRole.SAW),
                        soldier("3A", 6, "LAVI",   SoldierRole.M203),
                        soldier("3A", 7, "MOR",    SoldierRole.RIF),
                        soldier("3A", 8, "NAVON",  SoldierRole.RIF),
                    )),
                    Squad("3B", "Hotel", "PLT-3", listOf(
                        soldier("3B", 1, "OFER",   SoldierRole.TL),
                        soldier("3B", 2, "PAZ",    SoldierRole.RIF),
                        soldier("3B", 3, "RIVKA",  SoldierRole.MED),
                        soldier("3B", 4, "SHEMER", SoldierRole.RTO),
                        soldier("3B", 5, "TZUR",   SoldierRole.SAW),
                        soldier("3B", 6, "URIEL",  SoldierRole.M203),
                        soldier("3B", 7, "VEXLER", SoldierRole.RIF),
                        soldier("3B", 8, "WERNER", SoldierRole.RIF,
                            status = SoldierStatus.CAUTION, hr = 121, risk = 3.7f),
                    )),
                    Squad("3C", "India", "PLT-3", listOf(
                        soldier("3C", 1, "YAHALOM",SoldierRole.TL),
                        soldier("3C", 2, "ZADOK",  SoldierRole.RIF),
                        soldier("3C", 3, "ALON",   SoldierRole.MED),
                        soldier("3C", 4, "BEN-AMI",SoldierRole.RTO),
                        soldier("3C", 5, "CARMEL", SoldierRole.SAW),
                        soldier("3C", 6, "DORI",   SoldierRole.M203),
                        soldier("3C", 7, "EYAL",   SoldierRole.RIF),
                        soldier("3C", 8, "FAYAD",  SoldierRole.DM),
                    )),
                )
            ),
        )
    )

    val CRITICAL_ALERT = CriticalAlert(
        soldierId    = "2A-02",
        type         = "VITALS",
        message      = "TACHYCARDIA · POSSIBLE HEMORRHAGE",
        triggeredSec = 12,
        acknowledged = false,
    )

    // ── Convenience helpers ──────────────────────────────────────────────────

    fun allSoldiers(): List<Soldier> =
        COMPANY.platoons.flatMap { p -> p.squads.flatMap { s -> s.soldiers } }

    fun findSoldier(id: String): Soldier? = allSoldiers().firstOrNull { it.id == id }

    fun findSquad(id: String): Squad? =
        COMPANY.platoons.flatMap { it.squads }.firstOrNull { it.id == id }

    fun findPlatoon(id: String): Platoon? = COMPANY.platoons.firstOrNull { it.id == id }

    fun soldierSquadId(soldierId: String): String? =
        COMPANY.platoons.flatMap { it.squads }
            .firstOrNull { sq -> sq.soldiers.any { it.id == soldierId } }?.id
}
