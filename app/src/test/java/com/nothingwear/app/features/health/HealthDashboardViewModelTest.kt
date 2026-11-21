package com.nothingwear.app.features.health

import com.nothingwear.app.core.bluetooth.HeartRateSensorContact
import com.nothingwear.app.core.bluetooth.WatchMatchConfidence
import com.nothingwear.app.domain.model.BleDeviceSummary
import com.nothingwear.app.domain.model.BleDiagnosticsSnapshot
import com.nothingwear.app.domain.model.BleHeartRateMonitorState
import com.nothingwear.app.domain.model.BleHeartRateSample
import com.nothingwear.app.domain.model.BluetoothAdapterStatus
import com.nothingwear.app.domain.model.BluetoothTransportType
import com.nothingwear.app.domain.model.DiscoveredDeviceSource
import com.nothingwear.app.domain.model.GattInspectionSession
import com.nothingwear.app.domain.model.HealthConnectAvailability
import com.nothingwear.app.domain.model.HealthDashboardSnapshot
import com.nothingwear.app.domain.model.HealthMetricSummary
import com.nothingwear.app.domain.model.LastVerifiedHeartRate
import com.nothingwear.app.domain.model.MetricAvailabilityStatus
import com.nothingwear.app.domain.model.UnifiedHeartRateSource
import com.nothingwear.app.domain.model.WatchConnectionState
import com.nothingwear.app.domain.repository.DeviceDiagnosticsRepository
import com.nothingwear.app.domain.repository.HealthRepository
import com.nothingwear.app.domain.usecase.GetRequiredHealthPermissionsUseCase
import com.nothingwear.app.domain.usecase.InspectDeviceGattUseCase
import com.nothingwear.app.domain.usecase.ObserveBleDiagnosticsUseCase
import com.nothingwear.app.domain.usecase.ObserveHealthSnapshotUseCase
import com.nothingwear.app.domain.usecase.RecordVerifiedBleHeartRateUseCase
import com.nothingwear.app.domain.usecase.RefreshBleEnvironmentUseCase
import com.nothingwear.app.domain.usecase.RefreshHealthDataUseCase
import com.nothingwear.app.domain.usecase.StartStandardHeartRateMonitorUseCase
import com.nothingwear.app.domain.usecase.StopStandardHeartRateMonitorUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HealthDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun surfacesVerifiedHealthMetricsAndControlsBleHeartRateMonitoring() = runTest {
        val fakeHealthRepo = FakeHealthRepository()
        val fakeBleRepo = FakeBleRepository()

        val viewModel = HealthDashboardViewModel(
            observeHealthSnapshot = ObserveHealthSnapshotUseCase(fakeHealthRepo),
            observeBleDiagnostics = ObserveBleDiagnosticsUseCase(fakeBleRepo),
            refreshHealthData = RefreshHealthDataUseCase(fakeHealthRepo),
            refreshBleEnvironment = RefreshBleEnvironmentUseCase(fakeBleRepo),
            getRequiredHealthPermissions = GetRequiredHealthPermissionsUseCase(fakeHealthRepo),
            recordVerifiedBleHeartRate = RecordVerifiedBleHeartRateUseCase(fakeHealthRepo),
            inspectDeviceGatt = InspectDeviceGattUseCase(fakeBleRepo),
            startStandardHeartRateMonitor = StartStandardHeartRateMonitorUseCase(fakeBleRepo),
            stopStandardHeartRateMonitor = StopStandardHeartRateMonitorUseCase(fakeBleRepo),
        )

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals("6,420", viewModel.uiState.value.healthSnapshot.stepsToday.primaryValue)
        assertEquals(MetricAvailabilityStatus.VERIFIED, viewModel.uiState.value.healthSnapshot.stepsToday.status)
        assertEquals("Xiaomi Watch S4 06A3", viewModel.uiState.value.detectedWatch?.displayName)

        // Start live BLE HR -> switches unified source to LIVE_BLE and persists last verified HR
        viewModel.onToggleLiveBleHeartRate()
        advanceUntilIdle()
        assertTrue(fakeBleRepo.startedHrMonitor)
        assertTrue(viewModel.uiState.value.bleHeartRate.isListening)
        assertEquals(76, viewModel.uiState.value.bleHeartRate.latestSample?.bpm)
        assertEquals(UnifiedHeartRateSource.LIVE_BLE, viewModel.uiState.value.unifiedHeartRate.source)
        assertEquals("76", viewModel.uiState.value.unifiedHeartRate.primaryBpmText)
        assertEquals(76, fakeHealthRepo.lastRecordedBleBpm)

        // Stop live BLE HR -> automatically falls back to LAST_VERIFIED_CACHE with 76 BPM
        viewModel.onToggleLiveBleHeartRate()
        advanceUntilIdle()
        assertTrue(fakeBleRepo.stoppedHrMonitor)
        assertEquals(UnifiedHeartRateSource.LAST_VERIFIED_CACHE, viewModel.uiState.value.unifiedHeartRate.source)
        assertEquals("76", viewModel.uiState.value.unifiedHeartRate.primaryBpmText)

        viewModel.onHealthPermissionsResult(emptySet())
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.feedbackMessage?.contains("not granted") == true)

        job.cancel()
    }

    private class FakeHealthRepository : HealthRepository {
        var lastRecordedBleBpm: Int? = null
        private val snapshot = MutableStateFlow(
            HealthDashboardSnapshot(
                healthConnectAvailability = HealthConnectAvailability.AVAILABLE,
                hasHealthPermissions = true,
                stepsToday = HealthMetricSummary(
                    title = "Steps Today",
                    primaryValue = "6,420",
                    unit = "steps",
                    status = MetricAvailabilityStatus.VERIFIED,
                    sourceLabel = "Health Connect",
                    detailNote = "Verified via Health Connect (com.xiaomi.wearable)",
                ),
            ),
        )

        override fun observeHealthSnapshot(): Flow<HealthDashboardSnapshot> = snapshot
        override fun requiredHealthConnectPermissions(): Set<String> = setOf("READ_STEPS", "READ_HEART_RATE")
        override suspend fun refreshHealthData() = Unit
        override suspend fun recordVerifiedBleHeartRate(bpm: Int, timestampMillis: Long) {
            lastRecordedBleBpm = bpm
            snapshot.value = snapshot.value.copy(
                lastVerifiedHeartRate = LastVerifiedHeartRate(
                    bpm = bpm,
                    timestampMillis = timestampMillis,
                    sourceLabel = "Watch S4 Live BLE (0x2A37)",
                ),
            )
        }
    }

    private class FakeBleRepository : DeviceDiagnosticsRepository {
        var startedHrMonitor = false
        var stoppedHrMonitor = false

        private val diagnostics = MutableStateFlow(
            BleDiagnosticsSnapshot(
                adapterStatus = BluetoothAdapterStatus.ENABLED,
                devices = listOf(
                    BleDeviceSummary(
                        addressKey = "C4:57:00:00:00:A3",
                        redactedAddress = "C4:57:**:**:**:A3",
                        displayName = "Xiaomi Watch S4 06A3",
                        rssi = -54,
                        isBonded = true,
                        isSystemConnected = false,
                        transportType = BluetoothTransportType.DUAL,
                        source = DiscoveredDeviceSource.BONDED,
                        matchConfidence = WatchMatchConfidence.LIKELY_WATCH_S4,
                        matchReason = "Name matches Watch S4",
                        lastSeenAtMillis = 1_763_690_000_000L,
                    ),
                ),
                gattSession = GattInspectionSession(
                    deviceAddressKey = "C4:57:00:00:00:A3",
                    deviceName = "Xiaomi Watch S4 06A3",
                    redactedAddress = "C4:57:**:**:**:A3",
                    connectionState = WatchConnectionState.PROTOCOL_UNKNOWN,
                    heartRateState = BleHeartRateMonitorState(isServiceExposed = true),
                ),
            ),
        )

        override fun observeDiagnostics(): Flow<BleDiagnosticsSnapshot> = diagnostics
        override fun refreshEnvironmentAndBondedDevices() = Unit
        override fun startBleScan() = Unit
        override fun stopBleScan() = Unit
        override fun inspectDeviceGatt(addressKey: String) = Unit
        override fun startStandardHeartRateMonitor() {
            startedHrMonitor = true
            val sample = BleHeartRateSample(76, 1_763_690_005_000L, HeartRateSensorContact.CONTACT_DETECTED)
            diagnostics.value = diagnostics.value.copy(
                gattSession = diagnostics.value.gattSession.copy(
                    heartRateState = BleHeartRateMonitorState(
                        isServiceExposed = true,
                        isListening = true,
                        latestSample = sample,
                        recentSamples = listOf(sample),
                    ),
                ),
            )
        }

        override fun stopStandardHeartRateMonitor() {
            stoppedHrMonitor = true
            diagnostics.value = diagnostics.value.copy(
                gattSession = diagnostics.value.gattSession.copy(
                    heartRateState = diagnostics.value.gattSession.heartRateState.copy(isListening = false),
                ),
            )
        }

        override fun disconnectGatt() = Unit
        override fun clearLogs() = Unit
        override fun buildRedactedDiagnosticReport(): String = ""
    }
}
