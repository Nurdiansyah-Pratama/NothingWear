package com.nothingwear.app.core.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GattUuidLookupTest {

    @Test
    fun strictReadOnlyWhitelistAllowsOnlyStandardBatteryAndDeviceInfoStrings() {
        assertTrue(GattUuidLookup.isStandardSafeReadOnlyCharacteristic(GattUuidLookup.CHAR_BATTERY_LEVEL))
        assertTrue(GattUuidLookup.isStandardSafeReadOnlyCharacteristic(GattUuidLookup.CHAR_FIRMWARE_REVISION))
        assertTrue(GattUuidLookup.isStandardSafeReadOnlyCharacteristic(GattUuidLookup.CHAR_MANUFACTURER_NAME))

        // Proprietary / vendor UUIDs MUST NEVER be auto-read or written
        assertFalse(GattUuidLookup.isStandardSafeReadOnlyCharacteristic("0000fe95-0000-1000-8000-00805f9b34fb"))
        assertFalse(GattUuidLookup.isStandardSafeReadOnlyCharacteristic("00000001-0000-1000-8000-00805f9b34fb"))
    }

    @Test
    fun decodesStandardServiceNamesAndCharacteristicProperties() {
        assertEquals("Battery Service (0x180F)", GattUuidLookup.serviceDisplayName(GattUuidLookup.SERVICE_BATTERY))
        assertEquals("Xiaomi Inc. Service (0xFE95)", GattUuidLookup.serviceDisplayName(GattUuidLookup.SERVICE_XIAOMI_MI))
        assertEquals("Custom / Vendor Service", GattUuidLookup.serviceDisplayName("12345678-0000-1000-8000-00805f9b34fb"))

        val props = GattUuidLookup.decodeProperties(0x02 or 0x08 or 0x10)
        assertEquals(listOf("READ", "WRITE", "NOTIFY"), props)
        assertEquals(
            "Serial Port Profile / SPP (0x1101)",
            GattUuidLookup.sdpUuidDisplayName("00001101-0000-1000-8000-00805f9b34fb"),
        )
    }
}
