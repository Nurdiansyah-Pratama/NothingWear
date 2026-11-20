package com.nothingwear.app.core.bluetooth

/**
 * Lookup table for standard Bluetooth SIG GATT UUIDs and strict read-only safety whitelist.
 *
 * SAFETY RULE (Master Project Plan Section 9):
 * Only verified standard Bluetooth SIG read-only characteristics (Battery Level & Device
 * Information strings) may be read automatically. Proprietary or unknown characteristics
 * are NEVER written and NEVER auto-read.
 */
object GattUuidLookup {

    const val SERVICE_GENERIC_ACCESS = "00001800-0000-1000-8000-00805f9b34fb"
    const val SERVICE_GENERIC_ATTRIBUTE = "00001801-0000-1000-8000-00805f9b34fb"
    const val SERVICE_IMMEDIATE_ALERT = "00001802-0000-1000-8000-00805f9b34fb"
    const val SERVICE_CURRENT_TIME = "00001805-0000-1000-8000-00805f9b34fb"
    const val SERVICE_DEVICE_INFORMATION = "0000180a-0000-1000-8000-00805f9b34fb"
    const val SERVICE_HEART_RATE = "0000180d-0000-1000-8000-00805f9b34fb"
    const val SERVICE_BATTERY = "0000180f-0000-1000-8000-00805f9b34fb"
    const val SERVICE_ALERT_NOTIFICATION = "00001811-0000-1000-8000-00805f9b34fb"
    const val SERVICE_XIAOMI_MI = "0000fe95-0000-1000-8000-00805f9b34fb"
    const val SERVICE_XIAOMI_WEARABLE = "0000fdab-0000-1000-8000-00805f9b34fb"

    const val CHAR_DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb"
    const val CHAR_BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb"
    const val CHAR_MODEL_NUMBER = "00002a24-0000-1000-8000-00805f9b34fb"
    const val CHAR_FIRMWARE_REVISION = "00002a26-0000-1000-8000-00805f9b34fb"
    const val CHAR_HARDWARE_REVISION = "00002a27-0000-1000-8000-00805f9b34fb"
    const val CHAR_SOFTWARE_REVISION = "00002a28-0000-1000-8000-00805f9b34fb"
    const val CHAR_MANUFACTURER_NAME = "00002a29-0000-1000-8000-00805f9b34fb"
    const val CHAR_HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb"

    private val serviceNames = mapOf(
        SERVICE_GENERIC_ACCESS to "Generic Access (0x1800)",
        SERVICE_GENERIC_ATTRIBUTE to "Generic Attribute (0x1801)",
        SERVICE_IMMEDIATE_ALERT to "Immediate Alert (0x1802)",
        SERVICE_CURRENT_TIME to "Current Time Service (0x1805)",
        SERVICE_DEVICE_INFORMATION to "Device Information (0x180A)",
        SERVICE_HEART_RATE to "Heart Rate Service (0x180D)",
        SERVICE_BATTERY to "Battery Service (0x180F)",
        SERVICE_ALERT_NOTIFICATION to "Alert Notification (0x1811)",
        SERVICE_XIAOMI_MI to "Xiaomi Inc. Service (0xFE95)",
        SERVICE_XIAOMI_WEARABLE to "Xiaomi Wearable Service (0xFDAB)",
    )

    private val characteristicNames = mapOf(
        CHAR_DEVICE_NAME to "Device Name (0x2A00)",
        CHAR_BATTERY_LEVEL to "Battery Level (0x2A19)",
        CHAR_MODEL_NUMBER to "Model Number String (0x2A24)",
        CHAR_FIRMWARE_REVISION to "Firmware Revision String (0x2A26)",
        CHAR_HARDWARE_REVISION to "Hardware Revision String (0x2A27)",
        CHAR_SOFTWARE_REVISION to "Software Revision String (0x2A28)",
        CHAR_MANUFACTURER_NAME to "Manufacturer Name String (0x2A29)",
        CHAR_HEART_RATE_MEASUREMENT to "Heart Rate Measurement (0x2A37)",
    )

    private val safeStandardReadOnlyCharacteristics = setOf(
        CHAR_BATTERY_LEVEL,
        CHAR_MANUFACTURER_NAME,
        CHAR_MODEL_NUMBER,
        CHAR_FIRMWARE_REVISION,
        CHAR_HARDWARE_REVISION,
        CHAR_SOFTWARE_REVISION,
    )

    // Standard Android BluetoothGattCharacteristic property bitmask constants
    private const val PROP_BROADCAST = 0x01
    private const val PROP_READ = 0x02
    private const val PROP_WRITE_NO_RESPONSE = 0x04
    private const val PROP_WRITE = 0x08
    private const val PROP_NOTIFY = 0x10
    private const val PROP_INDICATE = 0x20
    private const val PROP_SIGNED_WRITE = 0x40

    private val sdpProfileNames = mapOf(
        "00001101-0000-1000-8000-00805f9b34fb" to "Serial Port Profile / SPP (0x1101)",
        "0000110a-0000-1000-8000-00805f9b34fb" to "A2DP Audio Source (0x110A)",
        "0000110b-0000-1000-8000-00805f9b34fb" to "A2DP Audio Sink (0x110B)",
        "0000110c-0000-1000-8000-00805f9b34fb" to "AVRCP Target (0x110C)",
        "0000110e-0000-1000-8000-00805f9b34fb" to "AVRCP Controller (0x110E)",
        "0000111e-0000-1000-8000-00805f9b34fb" to "Hands-Free Profile / HFP (0x111E)",
        "0000111f-0000-1000-8000-00805f9b34fb" to "HFP Audio Gateway (0x111F)",
        "00001124-0000-1000-8000-00805f9b34fb" to "Human Interface Device / HID (0x1124)",
        "0000112f-0000-1000-8000-00805f9b34fb" to "Phonebook Access / PBAP (0x112F)",
        "00001132-0000-1000-8000-00805f9b34fb" to "Message Access / MAP (0x1132)",
        "00001200-0000-1000-8000-00805f9b34fb" to "PnP Information (0x1200)",
        "00001800-0000-1000-8000-00805f9b34fb" to "Generic Access over BR/EDR (0x1800)",
        "00001801-0000-1000-8000-00805f9b34fb" to "Generic Attribute over BR/EDR (0x1801)",
    )

    fun serviceDisplayName(uuid: String): String =
        serviceNames[uuid.lowercase()] ?: "Custom / Vendor Service"

    fun sdpUuidDisplayName(uuid: String): String =
        sdpProfileNames[uuid.lowercase()]
            ?: serviceNames[uuid.lowercase()]
            ?: "Custom / Vendor RFCOMM/SDP UUID"

    fun characteristicDisplayName(uuid: String): String =
        characteristicNames[uuid.lowercase()] ?: "Vendor Characteristic"

    fun isStandardSafeReadOnlyCharacteristic(uuid: String): Boolean =
        uuid.lowercase() in safeStandardReadOnlyCharacteristics

    fun decodeProperties(propertiesBitmask: Int): List<String> = buildList {
        if (propertiesBitmask and PROP_READ != 0) add("READ")
        if (propertiesBitmask and PROP_WRITE != 0) add("WRITE")
        if (propertiesBitmask and PROP_WRITE_NO_RESPONSE != 0) add("WRITE_NR")
        if (propertiesBitmask and PROP_NOTIFY != 0) add("NOTIFY")
        if (propertiesBitmask and PROP_INDICATE != 0) add("INDICATE")
        if (propertiesBitmask and PROP_BROADCAST != 0) add("BROADCAST")
        if (propertiesBitmask and PROP_SIGNED_WRITE != 0) add("SIGNED_WRITE")
    }
}
