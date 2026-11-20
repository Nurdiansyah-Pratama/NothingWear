package com.nothingwear.app.core.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogRedactorTest {

    @Test
    fun redactMacAddressMasksMiddleOctetsAndHandlesNullOrMalformedInput() {
        assertEquals("AA:BB:**:**:**:FF", LogRedactor.redactMacAddress("aa:bb:cc:dd:ee:ff"))
        assertEquals("12:34:**:**:**:AB", LogRedactor.redactMacAddress("12-34-56-78-90-ab"))
        assertEquals("**:**:**:**:**:**", LogRedactor.redactMacAddress(null))
        assertEquals("REDACTED-ADDR", LogRedactor.redactMacAddress("not-a-mac"))
    }

    @Test
    fun redactMessageMasksEmbeddedMacAddressesAndEmails() {
        val raw = "Connected to Watch S4 [C4:11:22:33:44:9A] for owner alice.smith@gmail.com"
        val redacted = LogRedactor.redactMessage(raw)

        assertTrue(redacted.contains("C4:11:**:**:**:9A"))
        assertFalse(redacted.contains("22:33:44"))
        assertFalse(redacted.contains("alice.smith"))
        assertTrue(redacted.contains("al***@gmail.com"))
    }
}
