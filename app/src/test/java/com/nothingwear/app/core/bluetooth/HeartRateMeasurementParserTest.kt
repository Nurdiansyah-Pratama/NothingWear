package com.nothingwear.app.core.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HeartRateMeasurementParserTest {

    @Test
    fun parsesStandard8BitBpmWithContactDetected() {
        // Flags = 0x06 (8-bit BPM, contact detected = 11b), BPM = 74 (0x4A)
        val result = HeartRateMeasurementParser.parse(byteArrayOf(0x06, 0x4A))
        assertNotNull(result)
        assertEquals(74, result!!.bpm)
        assertEquals(HeartRateSensorContact.CONTACT_DETECTED, result.sensorContact)
        assertNull(result.energyExpendedKj)
        assertEquals(emptyList<Int>(), result.rrIntervalsMs)
    }

    @Test
    fun parses16BitBpmWithEnergyExpendedAndRrIntervals() {
        // Flags = 0x1D (bit0=16bit, bits1-2=10 no contact, bit3=energy, bit4=RR)
        // BPM = 132 (0x0084 LE), Energy = 250 kJ (0x00FA LE), RR = 1024 (1000ms)
        val payload = byteArrayOf(
            0x1D,
            0x84.toByte(), 0x00,
            0xFA.toByte(), 0x00,
            0x00, 0x04,
        )
        val result = HeartRateMeasurementParser.parse(payload)
        assertNotNull(result)
        assertEquals(132, result!!.bpm)
        assertEquals(HeartRateSensorContact.NO_CONTACT, result.sensorContact)
        assertEquals(250, result.energyExpendedKj)
        assertEquals(listOf(1000), result.rrIntervalsMs)
    }

    @Test
    fun rejectsEmptyShortOrZeroBpmPayloads() {
        assertNull(HeartRateMeasurementParser.parse(byteArrayOf()))
        assertNull(HeartRateMeasurementParser.parse(byteArrayOf(0x00)))
        assertNull(HeartRateMeasurementParser.parse(byteArrayOf(0x00, 0x00)))
        assertNull(HeartRateMeasurementParser.parse(byteArrayOf(0x01, 0x4A))) // 16-bit flag but only 1 byte
    }
}
