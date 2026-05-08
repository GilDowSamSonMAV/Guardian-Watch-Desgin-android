package com.gltech.guardianwatch.model

object DemoData {

    // ── IDF STRUCTURE ─────────────────────────────────────────────────────────
    // Golani Brigade · 13th Battalion · Aleph Company
    // 3 platoons (מחלקות) × 4 squads (כיתות) × 8 soldiers = 96 personnel

    private val names = listOf(
        "כהן","לוי","מזרחי","פרץ","כץ","אברהם","צור","דיין",
        "שפירא","ברק","גולן","נבון","אמיר","עופר","חיים","עידן",
        "ירדן","פלד","רועי","אייל","אריאל","כרמי","בנימין","נועם",
        "מתן","תירוש","ארנון","לביא","מאור","אלון","גיא","רן",
        "אורי","איתי","איילון","אגוז","פיינר","גדעון","לידור","עמרי",
        "תמרי","עוזיאל","וולף","יניב","וידל","זמיר","ראש","שדה",
        "דרורי","הראל","גלעד","ניר","טל","עוז","פז","בר",
        "עדן","יואב","דותן","מוקד","סער","שריד","ערן","גיורא",
        "זוהר","בורן","אגמון","דורי","ערד","עמית","עמוס","נבו",
        "פרדו","רז","שגיב","תומר","אוזן","ורד","גבי","תמיר",
        "שניר","רונן","קובי","לירון","נתן","אפרת","גליל","אסף",
        "ביטון","חדד","נחום","יצחק","שלמה","משה","יוסף","יאיר",
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
    var CRITICAL_ALERT: CriticalAlert? = null   // set in init after ACTIVE_ALERTS is built

    init {
        nameIdx = 0

        // ── PLATOON 1 — Machlaka Aleph ────────────────────────────────────────
        val sq1A = mkSquad("1", "A", "כיתה א'")
        val sq1B = run {
            val sid = "1B"
            Squad(sid, "כיתה ב'", "1", listOf(
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
        val sq1C = mkSquad("1", "C", "כיתה ג'")
        val sq1D = mkSquad("1", "D", "כיתה ד'")

        val plt1 = Platoon("PLT-1","מחלקה 1","גולני-1",
            "GRID 18S TJ 4421 8896", listOf(sq1A,sq1B,sq1C,sq1D))

        // ── PLATOON 2 — Machlaka Bet ──────────────────────────────────────────
        val sq2A = run {
            val sid = "2A"
            Squad(sid,"כיתה א'","2", listOf(
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
            Squad(sid,"כיתה ב'","2", listOf(
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
        val sq2C = mkSquad("2","C","כיתה ג'")
        val sq2D = run {
            val sid = "2D"
            Squad(sid,"כיתה ד'","2", listOf(
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
        val plt2 = Platoon("PLT-2","מחלקה 2","גולני-2",
            "GRID 18S TJ 4486 8741", listOf(sq2A,sq2B,sq2C,sq2D))

        // ── PLATOON 3 — Machlaka Gimel ────────────────────────────────────────
        val sq3A = mkSquad("3","A","כיתה א'")
        val sq3B = run {
            val sid = "3B"
            Squad(sid,"כיתה ב'","3", listOf(
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
        val sq3C = mkSquad("3","C","כיתה ג'")
        val sq3D = mkSquad("3","D","כיתה ד'")

        val plt3 = Platoon("PLT-3","מחלקה 3","גולני-3",
            "GRID 18S TJ 4538 8804", listOf(sq3A,sq3B,sq3C,sq3D))

        COMPANY = Company(
            id       = "GOLANI-13A",
            callsign = "גולני-6",
            name     = "גולני · גדוד 13 · פלוגה א'",
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
                        s.spo2 < 90 -> "טכיקרדיה · היפוקסיה · דימום מסיבי"
                        s.hr  > 180 -> "טכיקרדיה חריפה · סכנת דימום"
                        else        -> "טכיקרדיה · מדדים קריטיים"
                    },
                    triggeredSec = (i + 1) * 12,
                )
            }

        // Legacy single-alert for backward compat
        CRITICAL_ALERT = ACTIVE_ALERTS.firstOrNull()
    }

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
