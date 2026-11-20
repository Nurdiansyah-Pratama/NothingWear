package com.nothingwear.app.domain.model

import com.nothingwear.app.core.bluetooth.WatchMatchConfidence

enum class BluetoothAdapterStatus {
    UNSUPPORTED,
    DISABLED,
    ENABLED,
}

enum class DiscoveredDeviceSource(val label: String) {
    BLE_SCAN("BLE Scan"),
    BONDED("Paired / Bonded"),
    SYSTEM_CONNECTED("System GATT"),
}

/**
 * Represents a discovered or already-paired BLE device.
 * UI & logs MUST display [redactedAddress], never raw MAC addresses.
 */
data class BleDeviceSummary(
    val addressKey: String,
    val redactedAddress: String,
    val displayName: String,
    val rssi: Int?,
    val isBonded: Boolean,
    val isSystemConnected: Boolean,
    val source: DiscoveredDeviceSource,
    val matchConfidence: WatchMatchConfidence,
    val matchReason: String,
    val advertisedServiceUuids: List<String> = emptyList(),
    val lastSeenAtMillis: Long,
)

data class GattCharacteristicInfo(
    val uuid: String,
    val displayName: String,
    val properties: List<String>,
    val descriptorUuids: List<String>,
    val isReadOnlyInspected: Boolean = false,
)

data class GattServiceInfo(
    val uuid: String,
    val displayName: String,
    val isPrimary: Boolean,
    val characteristics: List<GattCharacteristicInfo>,
)

data class StandardDeviceInfo(
    val manufacturerName: String? = null,
    val modelNumber: String? = null,
    val firmwareRevision: String? = null,
    val hardwareRevision: String? = null,
    val softwareRevision: String? = null,
    val batteryPercent: Int? = null,
)

data class GattInspectionSession(
    val deviceAddressKey: String? = null,
    val deviceName: String? = null,
    val redactedAddress: String? = null,
    val connectionState: WatchConnectionState = WatchConnectionState.DISCONNECTED,
    val services: List<GattServiceInfo> = emptyList(),
    val standardDeviceInfo: StandardDeviceInfo = StandardDeviceInfo(),
    val lastStatusNote: String? = null,
)

enum class DiagnosticLogLevel {
    INFO,
    WARN,
    ERROR,
}

data class DiagnosticLogEntry(
    val id: Long,
    val timestampLabel: String,
    val level: DiagnosticLogLevel,
    val message: String,
)

data class BleDiagnosticsSnapshot(
    val adapterStatus: BluetoothAdapterStatus = BluetoothAdapterStatus.DISABLED,
    val isScanning: Boolean = false,
    val devices: List<BleDeviceSummary> = emptyList(),
    val gattSession: GattInspectionSession = GattInspectionSession(),
    val logs: List<DiagnosticLogEntry> = emptyList(),
)
