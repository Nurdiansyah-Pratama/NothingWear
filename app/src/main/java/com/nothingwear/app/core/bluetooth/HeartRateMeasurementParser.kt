package com.nothingwear.app.core.bluetooth

/**
 * Sensor contact status bits (bits 1–2) of the Bluetooth SIG Heart Rate Measurement (0x2A37) Flags byte.
 */
enum class HeartRateSensorContact(val label: String) {
    NOT_SUPPORTED("Contact feature N/A"),
    NO_CONTACT("No skin contact"),
    CONTACT_DETECTED("Skin contact OK"),
}

data class ParsedHeartRateMeasurement(
    val bpm: Int,
    val sensorContact: HeartRateSensorContact,
    val energyExpendedKj: Int? = null,
    val rrIntervalsMs: List<Int> = emptyList(),
)

/**
 * Official Bluetooth SIG Heart Rate Measurement (UUID 0x2A37) payload parser.
 *
 * Byte 0 (Flags):
 * - Bit 0: 0 = UINT8 HR value format (1 byte), 1 = UINT16 HR value format (2 bytes LE)
 * - Bits 1–2: Sensor Contact Status (00/01 = not supported, 10 = no contact, 11 = contact detected)
 * - Bit 3: Energy Expended Status present (UINT16 LE, kJ)
 * - Bit 4: RR-Interval values present (one or more UINT16 LE in 1/1024 sec units)
 */
object HeartRateMeasurementParser {

    fun parse(payload: ByteArray): ParsedHeartRateMeasurement? {
        if (payload.size < 2) return null

        val flags = payload[0].toInt() and 0xFF
        val is16BitBpm = (flags and 0x01) != 0
        var offset = 1

        val bpm = if (is16BitBpm) {
            if (payload.size < offset + 2) return null
            val low = payload[offset].toInt() and 0xFF
            val high = payload[offset + 1].toInt() and 0xFF
            offset += 2
            low or (high shl 8)
        } else {
            val value = payload[offset].toInt() and 0xFF
            offset += 1
            value
        }

        if (bpm !in 1..300) return null

        val contactBits = (flags shr 1) and 0x03
        val sensorContact = when (contactBits) {
            0b10 -> HeartRateSensorContact.NO_CONTACT
            0b11 -> HeartRateSensorContact.CONTACT_DETECTED
            else -> HeartRateSensorContact.NOT_SUPPORTED
        }

        val hasEnergyExpended = (flags and 0x08) != 0
        val energyExpendedKj = if (hasEnergyExpended && payload.size >= offset + 2) {
            val low = payload[offset].toInt() and 0xFF
            val high = payload[offset + 1].toInt() and 0xFF
            offset += 2
            low or (high shl 8)
        } else {
            null
        }

        val hasRrIntervals = (flags and 0x10) != 0
        val rrIntervals = mutableListOf<Int>()
        if (hasRrIntervals) {
            while (payload.size >= offset + 2) {
                val low = payload[offset].toInt() and 0xFF
                val high = payload[offset + 1].toInt() and 0xFF
                offset += 2
                val raw = low or (high shl 8)
                val ms = ((raw * 1000L) / 1024L).toInt()
                if (ms > 0) rrIntervals.add(ms)
            }
        }

        return ParsedHeartRateMeasurement(
            bpm = bpm,
            sensorContact = sensorContact,
            energyExpendedKj = energyExpendedKj,
            rrIntervalsMs = rrIntervals,
        )
    }
}
