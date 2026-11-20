package com.nothingwear.protocol.watchs4

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XiaomiWatchS4ProtocolAdapterTest {

    private val adapter = XiaomiWatchS4ProtocolAdapter()

    @Test
    fun reportsHonestUnsupportedStateWithVerifiedM2AndM3Metadata() {
        val report = adapter.getCapabilityReport()

        assertEquals(WatchS4AdapterState.UNSUPPORTED, report.state)
        assertEquals(XiaomiWatchS4ProtocolAdapter.VERIFIED_GATT_SERVICE_FDAB, report.verifiedGattServiceUuid)
        assertEquals(XiaomiWatchS4ProtocolAdapter.VERIFIED_CLASSIC_SPP_1101, report.verifiedSppUuid)
        assertTrue(report.authenticationMechanism.contains("WearAuthV2"))
    }

    @Test
    fun sanitizesEventPayloadsAndBlocksUnauthorizedNativeTransmission() = runBlocking {
        val longTitle = "A".repeat(120)
        val raw = listOf(
            WatchS4EventPayload(
                title = longTitle,
                location = "   ",
                description = "Detailed description",
                startEpochSeconds = 2_000L,
                endEpochSeconds = 1_500L, // invalid end < start
                isAllDay = false,
                reminderMinutes = -5,
                calendarName = "",
            ),
        )

        val prepared = adapter.prepareValidatedPayloads(raw)
        assertEquals(1, prepared.size)
        assertEquals(WatchS4EventPayload.MAX_TITLE_CHARS, prepared.first().title.length)
        assertNull(prepared.first().location)
        assertEquals(2_000L, prepared.first().endEpochSeconds)
        assertEquals(0, prepared.first().reminderMinutes)
        assertEquals("Calendar", prepared.first().calendarName)

        val outcome = adapter.attemptNativeSync(raw)
        assertTrue(outcome is WatchS4SyncOutcome.BlockedUnsupported)
        val blocked = outcome as WatchS4SyncOutcome.BlockedUnsupported
        assertEquals(WatchS4AdapterState.UNSUPPORTED, blocked.state)
        assertEquals(1, blocked.validatedPayloadCount)
    }
}
