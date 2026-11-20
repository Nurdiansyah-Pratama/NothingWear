package com.nothingwear.app.features.device

import com.nothingwear.app.core.bluetooth.WatchMatchConfidence
import com.nothingwear.app.core.permissions.BluetoothPermissionChecker
import com.nothingwear.app.domain.model.BleDeviceSummary
import com.nothingwear.app.domain.model.BleDiagnosticsSnapshot
import com.nothingwear.app.domain.model.BluetoothAdapterStatus
import com.nothingwear.app.domain.model.DiscoveredDeviceSource
import com.nothingwear.app.domain.model.GattInspectionSession
import com.nothingwear.app.domain.model.WatchConnectionState
import com.nothingwear.app.domain.repository.DeviceDiagnosticsRepository
import com.nothingwear.app.domain.usecase.BuildRedactedReportUseCase
import com.nothingwear.app.domain.usecase.ClearDiagnosticLogsUseCase
import com.nothingwear.app.domain.usecase.DisconnectGattUseCase
import com.nothingwear.app.domain.usecase.InspectDeviceGattUseCase
import com.nothingwear.app.domain.usecase.ObserveBleDiagnosticsUseCase
import com.nothingwear.app.domain.usecase.RefreshBleEnvironmentUseCase
import com.nothingwear.app.domain.usecase.StartBleScanUseCase
import com.nothingwear.app.domain.usecase.StopBleScanUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DevicesViewModelTest {

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
    fun filtersXiaomiOrBondedDevicesAndControlsScanAndGattInspection() = runTest {
        val watchS4 = BleDeviceSummary(
            addressKey = "AA:BB:CC:DD:EE:01",
            redactedAddress = "AA:BB:**:**:**:01",
            displayName = "Xiaomi Watch S4",
            rssi = -52,
            isBonded = true,
            isSystemConnected = true,
            source = DiscoveredDeviceSource.SYSTEM_CONNECTED,
            matchConfidence = WatchMatchConfidence.LIKELY_WATCH_S4,
            matchReason = "Device name matches Xiaomi Watch S4",
            lastSeenAtMillis = 1_000L,
        )
        val otherSpeaker = BleDeviceSummary(
            addressKey = "11:22:33:44:55:66",
            redactedAddress = "11:22:**:**:**:66",
            displayName = "Random Speaker",
            rssi = -78,
            isBonded = false,
            isSystemConnected = false,
            source = DiscoveredDeviceSource.BLE_SCAN,
            matchConfidence = WatchMatchConfidence.OTHER_PERIPHERAL,
            matchReason = "Standard BLE advertisement",
            lastSeenAtMillis = 1_000L,
        )

        val fakeRepo = FakeDeviceDiagnosticsRepository(
            BleDiagnosticsSnapshot(
                adapterStatus = BluetoothAdapterStatus.ENABLED,
                devices = listOf(watchS4, otherSpeaker),
            ),
        )
        val permissionChecker = object : BluetoothPermissionChecker {
            override fun requiredPermissions(): List<String> = listOf("BLUETOOTH_SCAN", "BLUETOOTH_CONNECT")
            override fun hasScanPermission(): Boolean = true
            override fun hasConnectPermission(): Boolean = true
        }

        val viewModel = DevicesViewModel(
            observeBleDiagnostics = ObserveBleDiagnosticsUseCase(fakeRepo),
            refreshBleEnvironment = RefreshBleEnvironmentUseCase(fakeRepo),
            startBleScan = StartBleScanUseCase(fakeRepo),
            stopBleScan = StopBleScanUseCase(fakeRepo),
            inspectDeviceGatt = InspectDeviceGattUseCase(fakeRepo),
            disconnectGatt = DisconnectGattUseCase(fakeRepo),
            clearDiagnosticLogs = ClearDiagnosticLogsUseCase(fakeRepo),
            buildRedactedReport = BuildRedactedReportUseCase(fakeRepo),
            permissionChecker = permissionChecker,
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.devices.size)

        // Toggle filter to Xiaomi/Paired only
        viewModel.onToggleFilterXiaomiOnly()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.devices.size)
        assertEquals("Xiaomi Watch S4", viewModel.uiState.value.devices.first().displayName)

        // Toggle scan start & stop
        viewModel.onToggleScan()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isScanning)
        viewModel.onToggleScan()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isScanning)

        // Inspect GATT and disconnect
        viewModel.onInspectDevice(watchS4.addressKey)
        advanceUntilIdle()
        assertEquals(WatchConnectionState.PROTOCOL_UNKNOWN, viewModel.uiState.value.gattSession.connectionState)

        viewModel.onDisconnectGatt()
        advanceUntilIdle()
        assertEquals(WatchConnectionState.DISCONNECTED, viewModel.uiState.value.gattSession.connectionState)

        collectJob.cancel()
    }

    private class FakeDeviceDiagnosticsRepository(
        initial: BleDiagnosticsSnapshot,
    ) : DeviceDiagnosticsRepository {
        private val state = MutableStateFlow(initial)
        override fun observeDiagnostics(): Flow<BleDiagnosticsSnapshot> = state
        override fun refreshEnvironmentAndBondedDevices() = Unit

        override fun startBleScan() {
            state.value = state.value.copy(isScanning = true)
        }

        override fun stopBleScan() {
            state.value = state.value.copy(isScanning = false)
        }

        override fun inspectDeviceGatt(addressKey: String) {
            state.value = state.value.copy(
                gattSession = GattInspectionSession(
                    deviceAddressKey = addressKey,
                    deviceName = "Xiaomi Watch S4",
                    redactedAddress = "AA:BB:**:**:**:01",
                    connectionState = WatchConnectionState.PROTOCOL_UNKNOWN,
                ),
            )
        }

        override fun disconnectGatt() {
            state.value = state.value.copy(
                gattSession = GattInspectionSession(connectionState = WatchConnectionState.DISCONNECTED),
            )
        }

        override fun clearLogs() {
            state.value = state.value.copy(logs = emptyList())
        }

        override fun buildRedactedDiagnosticReport(): String = "Redacted Report"
    }
}
