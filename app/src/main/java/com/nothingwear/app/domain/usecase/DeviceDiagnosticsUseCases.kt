package com.nothingwear.app.domain.usecase

import com.nothingwear.app.domain.model.BleDiagnosticsSnapshot
import com.nothingwear.app.domain.repository.DeviceDiagnosticsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveBleDiagnosticsUseCase @Inject constructor(
    private val repository: DeviceDiagnosticsRepository,
) {
    operator fun invoke(): Flow<BleDiagnosticsSnapshot> = repository.observeDiagnostics()
}

class RefreshBleEnvironmentUseCase @Inject constructor(
    private val repository: DeviceDiagnosticsRepository,
) {
    operator fun invoke() = repository.refreshEnvironmentAndBondedDevices()
}

class StartBleScanUseCase @Inject constructor(
    private val repository: DeviceDiagnosticsRepository,
) {
    operator fun invoke() = repository.startBleScan()
}

class StopBleScanUseCase @Inject constructor(
    private val repository: DeviceDiagnosticsRepository,
) {
    operator fun invoke() = repository.stopBleScan()
}

class InspectDeviceGattUseCase @Inject constructor(
    private val repository: DeviceDiagnosticsRepository,
) {
    operator fun invoke(addressKey: String) = repository.inspectDeviceGatt(addressKey)
}

class DisconnectGattUseCase @Inject constructor(
    private val repository: DeviceDiagnosticsRepository,
) {
    operator fun invoke() = repository.disconnectGatt()
}

class ClearDiagnosticLogsUseCase @Inject constructor(
    private val repository: DeviceDiagnosticsRepository,
) {
    operator fun invoke() = repository.clearLogs()
}

class BuildRedactedReportUseCase @Inject constructor(
    private val repository: DeviceDiagnosticsRepository,
) {
    operator fun invoke(): String = repository.buildRedactedDiagnosticReport()
}
