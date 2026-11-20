package com.nothingwear.app.features.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothingwear.app.core.bluetooth.WatchMatchConfidence
import com.nothingwear.app.core.permissions.BluetoothPermissionChecker
import com.nothingwear.app.domain.usecase.BuildRedactedReportUseCase
import com.nothingwear.app.domain.usecase.ClearDiagnosticLogsUseCase
import com.nothingwear.app.domain.usecase.DisconnectGattUseCase
import com.nothingwear.app.domain.usecase.InspectDeviceGattUseCase
import com.nothingwear.app.domain.usecase.ObserveBleDiagnosticsUseCase
import com.nothingwear.app.domain.usecase.RefreshBleEnvironmentUseCase
import com.nothingwear.app.domain.usecase.StartBleScanUseCase
import com.nothingwear.app.domain.usecase.StopBleScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    observeBleDiagnostics: ObserveBleDiagnosticsUseCase,
    private val refreshBleEnvironment: RefreshBleEnvironmentUseCase,
    private val startBleScan: StartBleScanUseCase,
    private val stopBleScan: StopBleScanUseCase,
    private val inspectDeviceGatt: InspectDeviceGattUseCase,
    private val disconnectGatt: DisconnectGattUseCase,
    private val clearDiagnosticLogs: ClearDiagnosticLogsUseCase,
    private val buildRedactedReport: BuildRedactedReportUseCase,
    private val permissionChecker: BluetoothPermissionChecker,
) : ViewModel() {

    private val localControls = MutableStateFlow(
        LocalDeviceControls(
            hasBlePermissions = permissionChecker.hasAllBlePermissions(),
        ),
    )

    val uiState: StateFlow<DevicesUiState> = combine(
        localControls,
        observeBleDiagnostics(),
    ) { controls, snapshot ->
        val filteredDevices = if (controls.showXiaomiOrBondedOnly) {
            snapshot.devices.filter {
                it.matchConfidence != WatchMatchConfidence.OTHER_PERIPHERAL ||
                    it.isBonded ||
                    it.isSystemConnected
            }
        } else {
            snapshot.devices
        }

        DevicesUiState(
            requiredPermissions = permissionChecker.requiredPermissions(),
            hasBlePermissions = controls.hasBlePermissions,
            permissionDeniedOnce = controls.permissionDeniedOnce,
            adapterStatus = snapshot.adapterStatus,
            isScanning = snapshot.isScanning,
            showXiaomiOrBondedOnly = controls.showXiaomiOrBondedOnly,
            devices = filteredDevices,
            totalDiscoveredCount = snapshot.devices.size,
            gattSession = snapshot.gattSession,
            expandedServiceUuids = controls.expandedServiceUuids,
            logs = snapshot.logs,
            reportCopiedNotice = controls.reportCopiedNotice,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = DevicesUiState(
            requiredPermissions = permissionChecker.requiredPermissions(),
            hasBlePermissions = permissionChecker.hasAllBlePermissions(),
        ),
    )

    init {
        refreshBleEnvironment()
    }

    fun onRefreshEnvironment() {
        val granted = permissionChecker.hasAllBlePermissions()
        localControls.update { it.copy(hasBlePermissions = granted) }
        refreshBleEnvironment()
    }

    fun onPermissionsResult(results: Map<String, Boolean>) {
        val allGranted = results.values.all { it } && permissionChecker.hasAllBlePermissions()
        localControls.update {
            it.copy(
                hasBlePermissions = allGranted,
                permissionDeniedOnce = !allGranted,
            )
        }
        refreshBleEnvironment()
    }

    fun onToggleScan() {
        if (!permissionChecker.hasAllBlePermissions()) return
        if (uiState.value.isScanning) {
            stopBleScan()
        } else {
            startBleScan()
        }
    }

    fun onToggleFilterXiaomiOnly() {
        localControls.update {
            it.copy(showXiaomiOrBondedOnly = !it.showXiaomiOrBondedOnly)
        }
    }

    fun onInspectDevice(addressKey: String) {
        if (!permissionChecker.hasConnectPermission()) return
        localControls.update { it.copy(expandedServiceUuids = emptySet()) }
        inspectDeviceGatt(addressKey)
    }

    fun onDisconnectGatt() {
        disconnectGatt()
    }

    fun onToggleServiceExpanded(serviceUuid: String) {
        localControls.update { state ->
            val updated = if (serviceUuid in state.expandedServiceUuids) {
                state.expandedServiceUuids - serviceUuid
            } else {
                state.expandedServiceUuids + serviceUuid
            }
            state.copy(expandedServiceUuids = updated)
        }
    }

    fun onClearLogs() {
        clearDiagnosticLogs()
    }

    fun generateRedactedReport(): String {
        val report = buildRedactedReport()
        localControls.update {
            it.copy(reportCopiedNotice = "Redacted diagnostic report copied to clipboard (MACs masked).")
        }
        return report
    }

    fun onDismissReportNotice() {
        localControls.update { it.copy(reportCopiedNotice = null) }
    }

    override fun onCleared() {
        stopBleScan()
        super.onCleared()
    }

    private data class LocalDeviceControls(
        val hasBlePermissions: Boolean = false,
        val permissionDeniedOnce: Boolean = false,
        val showXiaomiOrBondedOnly: Boolean = false,
        val expandedServiceUuids: Set<String> = emptySet(),
        val reportCopiedNotice: String? = null,
    )
}
