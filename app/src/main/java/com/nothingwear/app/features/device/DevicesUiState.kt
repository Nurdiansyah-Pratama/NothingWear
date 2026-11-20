package com.nothingwear.app.features.device

import com.nothingwear.app.domain.model.BleDeviceSummary
import com.nothingwear.app.domain.model.BluetoothAdapterStatus
import com.nothingwear.app.domain.model.DiagnosticLogEntry
import com.nothingwear.app.domain.model.GattInspectionSession

data class DevicesUiState(
    val requiredPermissions: List<String> = emptyList(),
    val hasBlePermissions: Boolean = false,
    val permissionDeniedOnce: Boolean = false,
    val adapterStatus: BluetoothAdapterStatus = BluetoothAdapterStatus.DISABLED,
    val isScanning: Boolean = false,
    val showXiaomiOrBondedOnly: Boolean = false,
    val devices: List<BleDeviceSummary> = emptyList(),
    val totalDiscoveredCount: Int = 0,
    val gattSession: GattInspectionSession = GattInspectionSession(),
    val expandedServiceUuids: Set<String> = emptySet(),
    val logs: List<DiagnosticLogEntry> = emptyList(),
    val reportCopiedNotice: String? = null,
)
