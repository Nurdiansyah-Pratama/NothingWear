package com.nothingwear.app.domain.repository

import com.nothingwear.app.domain.model.BleDiagnosticsSnapshot
import kotlinx.coroutines.flow.Flow

interface DeviceDiagnosticsRepository {
    fun observeDiagnostics(): Flow<BleDiagnosticsSnapshot>

    fun refreshEnvironmentAndBondedDevices()

    fun startBleScan()

    fun stopBleScan()

    fun inspectDeviceGatt(addressKey: String)

    fun startStandardHeartRateMonitor() = Unit

    fun stopStandardHeartRateMonitor() = Unit

    fun disconnectGatt()

    fun clearLogs()

    fun buildRedactedDiagnosticReport(): String
}
