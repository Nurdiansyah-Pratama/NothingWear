package com.nothingwear.app.core.bluetooth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GattStatusDecoderTest {

    @Test
    fun decodesCommonGattErrorCodesWithHumanReadableExplanations() {
        assertTrue(GattStatusDecoder.describeStatus(0).contains("GATT_SUCCESS"))
        assertTrue(GattStatusDecoder.describeStatus(133).contains("133"))
        assertTrue(GattStatusDecoder.describeStatus(19).contains("Watch S4"))
        assertTrue(GattStatusDecoder.describeStatus(8).contains("TIMEOUT"))
    }

    @Test
    fun triggersAutoAttachFallbackOnlyForTransientStackOrAdvertisingErrors() {
        assertTrue(GattStatusDecoder.shouldFallbackToAutoAttach(133))
        assertTrue(GattStatusDecoder.shouldFallbackToAutoAttach(8))
        assertTrue(GattStatusDecoder.shouldFallbackToAutoAttach(62))
        assertFalse(GattStatusDecoder.shouldFallbackToAutoAttach(0))
        assertFalse(GattStatusDecoder.shouldFallbackToAutoAttach(19))
    }
}
