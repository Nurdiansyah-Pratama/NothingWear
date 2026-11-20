package com.nothingwear.app.core.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchS4IdentifierTest {

    @Test
    fun identifiesLikelyXiaomiWatchS4ByName() {
        val result = WatchS4Identifier.classify(name = "Xiaomi Watch S4 1234")
        assertEquals(WatchMatchConfidence.LIKELY_WATCH_S4, result.confidence)
    }

    @Test
    fun identifiesXiaomiEcosystemByManufacturerIdOrMiServiceUuid() {
        val byMfg = WatchS4Identifier.classify(
            name = null,
            manufacturerIds = setOf(WatchS4Identifier.XIAOMI_MANUFACTURER_ID),
        )
        assertEquals(WatchMatchConfidence.XIAOMI_ECOSYSTEM, byMfg.confidence)

        val byUuid = WatchS4Identifier.classify(
            name = "Unnamed BLE Device",
            advertisedServiceUuids = listOf("0000fe95-0000-1000-8000-00805f9b34fb"),
        )
        assertEquals(WatchMatchConfidence.XIAOMI_ECOSYSTEM, byUuid.confidence)
    }

    @Test
    fun classifiesUnrelatedPeripheralHonestyAsOtherPeripheral() {
        val result = WatchS4Identifier.classify(
            name = "Living Room Speaker",
            manufacturerIds = setOf(0x004C),
        )
        assertEquals(WatchMatchConfidence.OTHER_PERIPHERAL, result.confidence)
    }
}
