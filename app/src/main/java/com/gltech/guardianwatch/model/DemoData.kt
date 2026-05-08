package com.gltech.guardianwatch.model

object DemoData {

    // ── IDF STRUCTURE ─────────────────────────────────────────────────────────
    // Golani Brigade · 13th Battalion · Aleph Company
    // 3 platoons (מחלקות) × 4 squads (כיתות) × 8 soldiers = 96 personnel

    private val names = listOf(
        "COHEN","LEVI","MIZRAHI","PERETZ","KATZ","AVRAHAM","TZUR","DAYAN",
        "SHAPIRO","BARAK","GOLAN","NAVON","AMIR","OFER","HAIM","IDAN",
        "YARDEN","PELED","ROEE","EYAL","ARIEL","CARMI","BINYAMIN","NOAM",
        "MATAN","TIROSH","ARNON","LAVY","MAOR","ALON","GUY","RAN",
        "ORI","ITAI","AYALON","EGOZ","FEINER","GIDEON","LIDOR","OMRI",
        "TAMARI","UZIEL","WOLF","YANIV","VIDAL","ZAMIR","ROSH","SADE",
        "DRORI","HAREL","GALOR","NIR","TAL","OZ","PAZ","BAR",
        "EDEN","YOAV","DOTAN","MOKED","SAAR","SARID","ERAN","GIORA",
        "ZOHAR","BOREN","AGMON","DORI","ARAD","AMIT","AMOS","NEVO",
        "PARDO","RAZ","SAGIV","TOMER","UZAN","VERED","GABI","TAMIR",
        "SNIR","RONEN","KOBI","LIRON","NATAN","EFRAT","GALIL","ASSAF",
        "BITON","HADAD","NACHUM","YITZHAK","SHLOMO","MOSHE","YOSEF","YAIR",
    )
    private var nameIdx = 0
    private fun nextName() = names[nameIdx++ % names.size]

    private fun soldier(
        squadId: String, pos: Int, role: SoldierRole,
        status: SoldierStatus = SoldierStatus.OK,
        hr: Int = (62..88).random(),
        br: Int = (13..18).random(),
        spo2: Int = (97..99).random(),
        coreTemp: Float = ((365..372).random() / 10f),
        risk: Float = ((8..25).random() / 10f),
        battery: Int = (60..95).random(),
        signal: MeshSignal = if ((1..10).random() > 2) MeshSignal.STRONG else MeshSignal.WEAK,
        lastUpdate: Int = (2..35).random(),
    ) = Soldier(
        id = "$squadId-${pos.toString().padStart(2,'0')}",
        pos = pos, last = nextName(), role = role, squadId = squadId,
        hr = hr, br = br, spo2 = spo2, coreTemp = coreTemp,
        risk = risk, status = status, batteryPct = battery,
        meshSignal = signal, lastUpdateSec = lastUpdate,
    )

    private fun mkSquad(platoonId: String, squadSuffix: String, squadName: String): Squad {
        val sid = "$platoonId$squadSuffix"
        return Squad(sid, squadName, platoonId, listOf(
            soldier(sid, 1, SoldierRole.TL),
            soldier(sid, 2, SoldierRole.RIF),
            soldier(sid, 3, SoldierRole.MED),
            soldier(sid, 4, SoldierRole.RTO),
            soldier(sid, 5, SoldierRole.SAW),
            soldier(sid, 6, SoldierRole.M203),
            soldier(sid, 7, SoldierRole.RIF),
            soldier(sid, 8, SoldierRole.DM),
        ))
    }

    val COMPANY: Company
    val ACTIVE_ALERTS: List<CriticalAlert>

    init {
        nameIdx = 0

        // ── PLATOON 1 — Machlaka Aleph ────────────────────────────────────────
        val sq1A = mkSquad("1", "A", "Kita Aleph")
        val sq1B = run {
            val sid = "1B"
            Squad(sid, "Kita Bet", "1", listOf(
                soldier(sid,1,SoldierRole.TL),
                soldier(sid,2,SoldierRole.RIF,
                    status=SoldierStatus.CAUTION, hr=128, br=22, risk=3.8f),
                soldier(sid,3,SoldierRole.MED),
                soldier(sid,4,SoldierRole.RTO),
                soldier(sid,5,SoldierRole.SAW),
                soldier(sid,6,SoldierRole.M203,
                    status=SoldierStatus.HIGH, hr=155, br=26, risk=6.4f),
                soldier(sid,7,SoldierRole.RIF),
                soldier(sid,8,SoldierRole.DM),
            ))
        }
        val sq1C = mkSquad("1", "C", "Kita Gimel")
        val sq1D = mkSquad("1", "D", "Kita Dalet")

        val plt1 = Platoon("PLT-1","1st Machlaka","GOLANI-1",
            "GRID 18S TJ 4421 8896", listOf(sq1A,sq1B,sq1C,sq1D))

        // ── PLATOON 2 — Machlaka Bet ──────────────────────────────────────────
        val sq2A = run {
            val sid = "2A"
            Squad(sid,"Kita Aleph","2", listOf(
                soldier(sid,1,SoldierRole.TL),
                soldier(sid,2,SoldierRole.RIF,        // ★ CRITICAL
                    status=SoldierStatus.CRITICAL, hr=187, br=30,
                    spo2=88, coreTemp=38.4f, risk=8.8f,
                    battery=83, signal=MeshSignal.STRONG, lastUpdate=12),
                soldier(sid,3,SoldierRole.MED),
                soldier(sid,4,SoldierRole.RTO),
                soldier(sid,5,SoldierRole.SAW,
                    status=SoldierStatus.HIGH, hr=158, br=27, risk=6.7f),
                soldier(sid,6,SoldierRole.M203),
                soldier(sid,7,SoldierRole.RIF),
                soldier(sid,8,SoldierRole.DM,
                    status=SoldierStatus.CAUTION, hr=132, br=23, risk=4.1f),
            ))
        }
        val sq2B = run {
            val sid = "2B"
            Squad(sid,"Kita Bet","2", listOf(
                soldier(sid,1,SoldierRole.TL),
                soldier(sid,2,SoldierRole.RIF,        // ★ CRITICAL
                    status=SoldierStatus.CRITICAL, hr=194, br=32,
                    spo2=85, coreTemp=38.8f, risk=9.2f,
                    battery=71, signal=MeshSignal.STRONG, lastUpdate=5),
                soldier(sid,3,SoldierRole.MED),
                soldier(sid,4,SoldierRole.RTO,
                    status=SoldierStatus.HIGH, hr=152, br=25, risk=6.1f),
                soldier(sid,5,SoldierRole.SAW),
                soldier(sid,6,SoldierRole.M203),
                soldier(sid,7,SoldierRole.RIF,
                    status=SoldierStatus.OFFLINE, risk=0f,
                    lastUpdate=280, signal=MeshSignal.NONE),
                soldier(sid,8,SoldierRole.DM),
            ))
        }
        val sq2C = mkSquad("2","C","Kita Gimel")
        val sq2D = run {
            val sid = "2D"
            Squad(sid,"Kita Dalet","2", listOf(
                soldier(sid,1,SoldierRole.TL),
                soldier(sid,2,SoldierRole.RIF,
                    status=SoldierStatus.CAUTION, hr=120, risk=3.3f),
                soldier(sid,3,SoldierRole.MED),
                soldier(sid,4,SoldierRole.RTO),
                soldier(sid,5,SoldierRole.SAW,
                    status=SoldierStatus.CRITICAL, hr=178, br=29,
                    spo2=90, coreTemp=38.2f, risk=8.1f,
                    battery=66, signal=MeshSignal.WEAK, lastUpdate=18),
                soldier(sid,6,SoldierRole.M203),
                soldier(sid,7,SoldierRole.RIF),
                soldier(sid,8,SoldierRole.DM),
            ))
        }
        val plt2 = Platoon("PLT-2","2nd Machlaka","GOLANI-2",
            "GRID 18S TJ 4486 8741", listOf(sq2A,sq2B,sq2C,sq2D))

        // ── PLATOON 3 — Machlaka Gimel ────────────────────────────────────────
        val sq3A = mkSquad("3","A","Kita Aleph")
        val sq3B = run {
            val sid = "3B"
            Squad(sid,"Kita Bet","3", listOf(
                soldier(sid,1,SoldierRole.TL),
                soldier(sid,2,SoldierRole.RIF,
                    status=SoldierStatus.CAUTION, hr=118, risk=3.5f),
                soldier(sid,3,SoldierRole.MED),
                soldier(sid,4,SoldierRole.RTO),
                soldier(sid,5,SoldierRole.SAW,
                    status=SoldierStatus.HIGH, hr=148, br=24, risk=5.8f),
                soldier(sid,6,SoldierRole.M203),
                soldier(sid,7,SoldierRole.RIF),
                soldier(sid,8,SoldierRole.DM),
            ))
        }
        val sq3C = mkSquad("3","C","Kita Gimel")
        val sq3D = mkSquad("3","D","Kita Dalet")

        val plt3 = Platoon("PLT-3","3rd Machlaka","GOLANI-3",
            "GRID 18S TJ 4538 8804", listOf(sq3A,sq3B,sq3C,sq3D))

        COMPANY = Company(
            id       = "GOLANI-13A",
            callsign = "GOLANI-6",
            name     = "Golani · 13th Btn · Aleph Coy",
            platoons = listOf(plt1, plt2, plt3),
        )

        // ── Build alerts from all CRITICAL soldiers ───────────────────────────
        ACTIVE_ALERTS = COMPANY.platoons
            .flatMap { p -> p.squads.flatMap { s -> s.soldiers } }
            .filter { it.status == SoldierStatus.CRITICAL }
            .mapIndexed { i, s ->
                CriticalAlert(
                    soldierId    = s.id,
                    type         = "VITALS",
                    message      = when {
                        s.spo2 < 90 -> "TACHYCARDIA · HYPOXIA · HEMORRHAGE"
                        s.hr  > 180 -> "SEVERE TACHYCARDIA · HEMORRHAGE RISK"
                        else        -> "TACHYCARDIA · VITALS CRITICAL"
                    },
                    triggeredSec = (i + 1) * 12,
                )
            }

        // Legacy single-alert for backward compat
        CRITICAL_ALERT = ACTIVE_ALERTS.firstOrNull()
    }

    var CRITICAL_ALERT: CriticalAlert? = null

    fun allSoldiers(): List<Soldier> =
        COMPANY.platoons.flatMap { p -> p.squads.flatMap { it.soldiers } }

    fun findSoldier(id: String) = allSoldiers().firstOrNull { it.id == id }
    fun findSquad(id: String)   = COMPANY.platoons.flatMap { it.squads }.firstOrNull { it.id == id }
    fun findPlatoon(id: String) = COMPANY.platoons.firstOrNull { it.id == id }
    fun soldierSquadId(sid: String) =
        COMPANY.platoons.flatMap { it.squads }.firstOrNull { sq -> sq.soldiers.any { it.id == sid } }?.id

    /** All wounded (CRITICAL + HIGH + CAUTION), sorted by severity. */
    fun allWounded(): List<Triple<Platoon, Squad, Soldier>> =
        COMPANY.platoons.flatMap { plt ->
            plt.squads.flatMap { sq ->
                sq.soldiers
                    .filter { it.status in listOf(SoldierStatus.CRITICAL, SoldierStatus.HIGH, SoldierStatus.CAUTION) }
                    .map { Triple(plt, sq, it) }
            }
        }.sortedBy { severityRank(it.third.status) }

    private fun severityRank(s: SoldierStatus) = when(s) {
        SoldierStatus.CRITICAL -> 0
        SoldierStatus.HIGH     -> 1
        SoldierStatus.CAUTION  -> 2
        else                   -> 3
    }
}
