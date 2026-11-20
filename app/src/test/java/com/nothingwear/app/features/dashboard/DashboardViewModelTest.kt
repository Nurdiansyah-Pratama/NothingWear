package com.nothingwear.app.features.dashboard

import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.core.permissions.CalendarPermissionChecker
import com.nothingwear.app.data.repository.FoundationDashboardRepository
import com.nothingwear.app.domain.model.BleDiagnosticsSnapshot
import com.nothingwear.app.domain.model.BluetoothAdapterStatus
import com.nothingwear.app.domain.model.BridgeActionResult
import com.nothingwear.app.domain.model.CalendarEvent
import com.nothingwear.app.domain.model.CalendarSource
import com.nothingwear.app.domain.model.CalendarSyncMetadata
import com.nothingwear.app.domain.model.CalendarSyncResult
import com.nothingwear.app.domain.model.CalendarSyncState
import com.nothingwear.app.domain.model.EventSyncOverview
import com.nothingwear.app.domain.model.GattInspectionSession
import com.nothingwear.app.domain.model.ReminderBridgeSettings
import com.nothingwear.app.domain.model.ReminderLeadTime
import com.nothingwear.app.domain.model.StandardDeviceInfo
import com.nothingwear.app.domain.model.WatchConnectionState
import com.nothingwear.app.domain.repository.CalendarRepository
import com.nothingwear.app.domain.repository.DeviceDiagnosticsRepository
import com.nothingwear.app.domain.repository.EventSyncRepository
import com.nothingwear.app.domain.usecase.ObserveDashboardOverviewUseCase
import com.nothingwear.protocol.watchs4.XiaomiWatchS4ProtocolAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val zone = ZoneId.of("Asia/Jakarta")
    private val nowMillis = LocalDate.of(2026, 11, 20).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
    private val fixedTime = object : TimeProvider {
        override fun nowMillis(): Long = nowMillis
        override fun zoneId(): ZoneId = zone
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun dashboardPromptsForPermissionWhenUncachedAndKeepsWatchStatesHonest() = runTest {
        val repository = FoundationDashboardRepository(
            calendarRepository = StubCalendarRepository(emptyList(), CalendarSyncMetadata()),
            deviceDiagnosticsRepository = StubDeviceDiagnosticsRepository(
                BleDiagnosticsSnapshot(adapterStatus = BluetoothAdapterStatus.ENABLED),
            ),
            eventSyncRepository = StubEventSyncRepository(),
            permissionChecker = CalendarPermissionChecker { false },
            timeProvider = fixedTime,
        )
        val viewModel = DashboardViewModel(ObserveDashboardOverviewUseCase(repository))

        val readyState = viewModel.uiState.first { it is DashboardUiState.Ready } as DashboardUiState.Ready
        val overview = readyState.overview

        assertEquals("Xiaomi Watch S4", overview.deviceStatus.targetDeviceName)
        assertEquals(WatchConnectionState.DISCONNECTED, overview.deviceStatus.connectionState)
        assertNull(overview.deviceStatus.batteryPercent)
        assertEquals(CalendarSyncState.PERMISSION_REQUIRED, overview.calendarOverview.localCalendarState)
        assertEquals(WatchConnectionState.UNSUPPORTED, overview.calendarOverview.nativeWatchSyncState)
        assertTrue(overview.calendarOverview.todayEvents.isEmpty())
        assertTrue(overview.calendarOverview.upcomingEvents.isEmpty())
    }

    @Test
    fun dashboardSurfacesCachedEventsAndVerifiedStandardBatteryWhenReadOnlyGattIsActive() = runTest {
        val todayEvent = CalendarEvent(
            instanceKey = "1_today",
            eventId = 1L,
            calendarId = 1L,
            calendarName = "Google",
            calendarColor = 0xFFD71921.toInt(),
            title = "Morning Sync",
            description = null,
            location = "Office",
            startTimeMillis = LocalDate.of(2026, 11, 20).atTime(10, 0).atZone(zone).toInstant().toEpochMilli(),
            endTimeMillis = LocalDate.of(2026, 11, 20).atTime(11, 0).atZone(zone).toInstant().toEpochMilli(),
            isAllDay = false,
            timezone = "Asia/Jakarta",
            lastSyncedAtMillis = nowMillis,
        )
        val tomorrowEvent = CalendarEvent(
            instanceKey = "2_tomorrow",
            eventId = 2L,
            calendarId = 1L,
            calendarName = "Google",
            calendarColor = 0xFF3B82F6.toInt(),
            title = "Watch Research Notes",
            description = null,
            location = null,
            startTimeMillis = LocalDate.of(2026, 11, 21).atTime(14, 0).atZone(zone).toInstant().toEpochMilli(),
            endTimeMillis = LocalDate.of(2026, 11, 21).atTime(15, 0).atZone(zone).toInstant().toEpochMilli(),
            isAllDay = false,
            timezone = "Asia/Jakarta",
            lastSyncedAtMillis = nowMillis,
        )

        val repository = FoundationDashboardRepository(
            calendarRepository = StubCalendarRepository(
                events = listOf(todayEvent, tomorrowEvent),
                metadata = CalendarSyncMetadata(
                    lastSyncedAtMillis = nowMillis,
                    selectedCalendarCount = 1,
                    totalCalendarCount = 1,
                    cachedEventCount = 2,
                ),
            ),
            deviceDiagnosticsRepository = StubDeviceDiagnosticsRepository(
                BleDiagnosticsSnapshot(
                    adapterStatus = BluetoothAdapterStatus.ENABLED,
                    gattSession = GattInspectionSession(
                        deviceAddressKey = "AA:BB:CC:DD:EE:FF",
                        deviceName = "Xiaomi Watch S4",
                        redactedAddress = "AA:BB:**:**:**:FF",
                        connectionState = WatchConnectionState.PROTOCOL_UNKNOWN,
                        standardDeviceInfo = StandardDeviceInfo(batteryPercent = 84),
                    ),
                ),
            ),
            eventSyncRepository = StubEventSyncRepository(),
            permissionChecker = CalendarPermissionChecker { true },
            timeProvider = fixedTime,
        )
        val viewModel = DashboardViewModel(ObserveDashboardOverviewUseCase(repository))

        val readyState = viewModel.uiState.first { it is DashboardUiState.Ready } as DashboardUiState.Ready
        val overview = readyState.overview

        assertEquals(CalendarSyncState.LOCAL_ONLY, overview.calendarOverview.localCalendarState)
        assertEquals(1, overview.calendarOverview.todayEvents.size)
        assertEquals("Morning Sync", overview.calendarOverview.todayEvents.first().title)
        assertEquals(1, overview.calendarOverview.upcomingEvents.size)
        assertEquals("Watch Research Notes", overview.calendarOverview.upcomingEvents.first().title)
        assertEquals(WatchConnectionState.PROTOCOL_UNKNOWN, overview.deviceStatus.connectionState)
        assertEquals(84, overview.deviceStatus.batteryPercent)
        assertEquals(WatchConnectionState.UNSUPPORTED, overview.calendarOverview.nativeWatchSyncState)
    }

    private class StubEventSyncRepository : EventSyncRepository {
        override fun observeEventSyncOverview(): Flow<EventSyncOverview> = flowOf(
            EventSyncOverview(
                nativeCapability = XiaomiWatchS4ProtocolAdapter().getCapabilityReport(),
                bridgeSettings = ReminderBridgeSettings(),
                upcomingPlannedReminders = emptyList(),
                validatedPayloadCount = 0,
            ),
        )

        override suspend fun setReminderBridgeEnabled(enabled: Boolean) = BridgeActionResult.Cleared
        override suspend fun setReminderLeadTime(leadTime: ReminderLeadTime) = BridgeActionResult.Scheduled(0)
        override suspend fun rescheduleUpcomingReminders() = BridgeActionResult.Scheduled(0)
        override suspend fun sendTestWatchAlert() = BridgeActionResult.TestNotificationSent
    }

    private class StubCalendarRepository(
        private val events: List<CalendarEvent>,
        private val metadata: CalendarSyncMetadata,
    ) : CalendarRepository {
        override fun observeCalendars(): Flow<List<CalendarSource>> = flowOf(emptyList())
        override fun observeSelectedEvents(): Flow<List<CalendarEvent>> = flowOf(events)
        override fun observeEventDetail(instanceKey: String): Flow<CalendarEvent?> = flowOf(null)
        override fun observeSyncMetadata(): Flow<CalendarSyncMetadata> = flowOf(metadata)
        override suspend fun syncFromDevice(): CalendarSyncResult = CalendarSyncResult.PermissionDenied
        override suspend fun setCalendarSelected(calendarId: Long, selected: Boolean) = Unit
    }

    private class StubDeviceDiagnosticsRepository(
        private val snapshot: BleDiagnosticsSnapshot,
    ) : DeviceDiagnosticsRepository {
        override fun observeDiagnostics(): Flow<BleDiagnosticsSnapshot> = flowOf(snapshot)
        override fun refreshEnvironmentAndBondedDevices() = Unit
        override fun startBleScan() = Unit
        override fun stopBleScan() = Unit
        override fun inspectDeviceGatt(addressKey: String) = Unit
        override fun disconnectGatt() = Unit
        override fun clearLogs() = Unit
        override fun buildRedactedDiagnosticReport(): String = ""
    }
}
