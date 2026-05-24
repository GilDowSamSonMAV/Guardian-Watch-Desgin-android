package com.gltech.guardianwatch.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gltech.guardianwatch.casualty.VitalsFrame

/**
 * Persistent record of a single vitals reading from the watch.
 * One row per BLE telemetry packet received.
 */
@Entity(
    tableName = "vital_signs",
    indices = [Index("casualtyId"), Index("timestampMs")]
)
data class VitalSignsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val casualtyId: String,
    val timestampMs: Long,
    val hrBpm: Int?,
    val spo2Pct: Int?,
    val accelMagG: Float?,
    val batteryPct: Int,
    val rssiDbm: Int,
    val movementClass: Int?,
    /** Serialised VitalAssessment JSON — null until anomaly engine runs. */
    val assessmentJson: String?,
) {
    companion object {
        fun from(casualtyId: String, frame: VitalsFrame, assessmentJson: String? = null) =
            VitalSignsEntity(
                casualtyId    = casualtyId,
                timestampMs   = frame.timestampMs,
                hrBpm         = frame.hrBpm,
                spo2Pct       = frame.spo2Pct,
                accelMagG     = frame.accelMagG,
                batteryPct    = frame.batteryPct,
                rssiDbm       = frame.rssiDbm,
                movementClass = frame.movementClass,
                assessmentJson = assessmentJson,
            )
    }
}
