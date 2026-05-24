package com.gltech.guardianwatch.ble

import java.util.UUID

/**
 * Guardian Watch BLE GATT profile.
 *
 * Uses standard Bluetooth SIG services where possible for interop with
 * off-the-shelf tools (nRF Connect etc.), with a custom Guardian Watch
 * service for the anomaly-detection payload that the Connect IQ watchapp
 * publishes.
 *
 * Heart Rate: standard 180D/2A37 profile. Garmin Connect IQ exposes this natively.
 * Battery:    standard 180F/2A19.
 * Guardian:   custom service for packed telemetry (HR+accel+trend+tone).
 */
object GuardianWatchProfile {

    // --- Standard services ---
    val HEART_RATE_SERVICE: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    val HEART_RATE_MEASUREMENT: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    val BATTERY_SERVICE: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    val BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    // --- Guardian Watch custom service (reserved prefix f1ac0000-...) ---
    val GW_SERVICE: UUID = UUID.fromString("f1ac0000-4743-4c54-4543-48475700754d")
    /** Telemetry notifications: 20 bytes packed — see [parseTelemetry] below. */
    val GW_TELEMETRY: UUID = UUID.fromString("f1ac0001-4743-4c54-4543-48475700754d")
    /** Command write (ack, shock, etc.). */
    val GW_COMMAND: UUID = UUID.fromString("f1ac0002-4743-4c54-4543-48475700754d")

    // CCCD — needed to enable notifications on any characteristic.
    val CLIENT_CHARACTERISTIC_CONFIG: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // ----- Packed telemetry payload (matches Connect IQ app's BlePublisher) -----
    //
    //  Version 0x01 (20 bytes):
    //   byte 0      : schema version = 0x01
    //   byte 1      : HR (bpm, 0=unavailable)
    //   byte 2      : overall tone (0=ok, 1=warn, 2=critical)
    //   bytes 3..4  : accel magnitude ×100 (uint16 LE)
    //   bytes 5..6  : HR trend (int16 LE, signed, bpm per minute)
    //   byte 7      : movement class (0=still,1=walk,2=run,3=fall,4=seizure)
    //   byte 8      : battery pct
    //   bytes 9..12 : epoch seconds (uint32 LE)
    //   bytes 13..19: reserved
    //
    //  Version 0x02 (22 bytes) — adds SpO2 + GPS:
    //   bytes 0..12 : same as v0x01
    //   byte 13     : SpO2 pct (0=unavailable)
    //   bytes 14..17: latitude × 1e6 as int32 LE (0x80000000 = unavailable)
    //   bytes 18..21: longitude × 1e6 as int32 LE (0x80000000 = unavailable)

    data class Telemetry(
        val version: Int,
        val hrBpm: Int?,
        val tone: Int,
        val accelMagG: Float,
        val hrTrendBpmPerMin: Int,
        val movementClass: Int,
        val batteryPct: Int,
        val tsSec: Long,
        // v0x02 extensions — null when not present
        val spo2Pct: Int? = null,
        val latDeg: Double? = null,
        val lonDeg: Double? = null,
    )

    private val GPS_UNAVAILABLE = 0x80000000.toInt()

    fun parseTelemetry(bytes: ByteArray): Telemetry? {
        if (bytes.size < 13) return null
        val ver = bytes[0].toInt() and 0xFF
        if (ver != 0x01 && ver != 0x02) return null
        val hrRaw = bytes[1].toInt() and 0xFF
        val hr = if (hrRaw == 0) null else hrRaw
        val tone = bytes[2].toInt() and 0xFF
        val accelRaw = ((bytes[3].toInt() and 0xFF) or ((bytes[4].toInt() and 0xFF) shl 8))
        val accelMag = accelRaw / 100f
        val trendRaw = ((bytes[5].toInt() and 0xFF) or ((bytes[6].toInt() and 0xFF) shl 8))
        val trend = if (trendRaw >= 0x8000) trendRaw - 0x10000 else trendRaw
        val movement = bytes[7].toInt() and 0xFF
        val battery = bytes[8].toInt() and 0xFF
        val ts: Long = (bytes[9].toLong() and 0xFFL) or
                       ((bytes[10].toLong() and 0xFFL) shl 8) or
                       ((bytes[11].toLong() and 0xFFL) shl 16) or
                       ((bytes[12].toLong() and 0xFFL) shl 24)

        // Version 0x02 extensions
        var spo2: Int? = null
        var lat: Double? = null
        var lon: Double? = null
        if (ver == 0x02 && bytes.size >= 22) {
            val spo2Raw = bytes[13].toInt() and 0xFF
            spo2 = if (spo2Raw == 0) null else spo2Raw

            val latRaw = (bytes[14].toInt() and 0xFF) or
                         ((bytes[15].toInt() and 0xFF) shl 8) or
                         ((bytes[16].toInt() and 0xFF) shl 16) or
                         (bytes[17].toInt() shl 24)  // signed shift for sign extension
            if (latRaw != GPS_UNAVAILABLE) lat = latRaw / 1_000_000.0

            val lonRaw = (bytes[18].toInt() and 0xFF) or
                         ((bytes[19].toInt() and 0xFF) shl 8) or
                         ((bytes[20].toInt() and 0xFF) shl 16) or
                         (bytes[21].toInt() shl 24)
            if (lonRaw != GPS_UNAVAILABLE) lon = lonRaw / 1_000_000.0
        }

        return Telemetry(ver, hr, tone, accelMag, trend, movement, battery, ts, spo2, lat, lon)
    }
}
