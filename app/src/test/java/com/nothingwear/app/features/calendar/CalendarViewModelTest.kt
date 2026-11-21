package com.nothingwear.app.features.calendar

import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.core.permissions.CalendarPermissionChecker
import com.nothingwear.app.domain.model.AgendaRange
import com.nothingwear.app.domain.model.BridgeActionResult
import com.nothingwear.app.domain.model.CalendarEvent
import com.nothingwear.app.domain.model.CalendarMutationResult
import com.nothingwear.app.domain.model.CalendarSource
import com.nothingwear.app.domain.model.CalendarSyncMetadata
import com.nothingwear.app.domain.model.CalendarSyncResult
import com.nothingwear.app.domain.model.CreateCalendarEventInput
import com.nothingwear.app.domain.model.EventSyncOverview
import com.nothingwear.app.domain.model.ReminderLeadTime
import com.nothingwear.app.domain.repository.CalendarRepository
import com.nothingwear.app.domain.repository.EventSyncRepository
import com.nothingwear.app.domain.usecase.CreateCalendarEventUseCase
import com.nothingwear.app.domain.usecase.DeleteCalendarEventUseCase
import com.nothingwear.app.domain.usecase.ObserveCalendarSyncMetadataUseCase
import com.nothingwear.app.domain.usecase.ObserveCalendarsUseCase
import com.nothingwear.app.domain.usecase.RescheduleWatchRemindersUseCase
import com.nothingwear.app.domain.usecase.SyncDeviceCalendarsUseCase
import com.nothingwear.app.domain.usecase.ToggleCalendarSelectionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
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
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

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
    fun filtersEventsByRangeAndSearchQueryAndSupportsOfflineCacheWhenPermissionDenied() = runTest {
        val todayEvent = CalendarEvent(
            instanceKey = "1_today",
            eventId = 1L,
            calendarId = 10L,
            calendarName = "Google Work",
            calendarColor = 0xFFD71921.toInt(),
            title = "Sprint Planning",
            description = "Roadmap check",
            location = "Jakarta HQ",
            startTimeMillis = LocalDate.of(2026, 11, 20).atTime(13, 0).atZone(zone).toInstant().toEpochMilli(),
            endTimeMillis = LocalDate.of(2026, 11, 20).atTime(14, 0).atZone(zone).toInstant().toEpochMilli(),
            isAllDay = false,
            timezone = "Asia/Jakarta",
            lastSyncedAtMillis = nowMillis,
        )
        val day10Event = CalendarEvent(
            instanceKey = "2_later",
            eventId = 2L,
            calendarId = 10L,
            calendarName = "Google Work",
            calendarColor = 0xFFD71921.toInt(),
            title = "Dentist Checkup",
            description = null,
            location = "Clinic",
            startTimeMillis = LocalDate.of(2026, 11, 30).atTime(15, 0).atZone(zone).toInstant().toEpochMilli(),
            endTimeMillis = LocalDate.of(2026, 11, 30).atTime(16, 0).atZone(zone).toInstant().toEpochMilli(),
            isAllDay = false,
            timezone = "Asia/Jakarta",
            lastSyncedAtMillis = nowMillis,
        )

        val fakeRepo = FakeCalendarRepository(
            initialCalendars = listOf(
                CalendarSource(
                    id = 10L,
                    displayName = "Google Work",
                    accountName = "me@gmail.com",
                    accountType = "com.google",
                    color = 0xFFD71921.toInt(),
                    isSelected = true,
                    isPrimary = true,
                ),
            ),
            initialEvents = listOf(todayEvent, day10Event),
            lastSyncedAtMillis = nowMillis,
        )

        val stubEventSyncRepo = object : EventSyncRepository {
            override fun observeEventSyncOverview(): Flow<EventSyncOverview> = MutableStateFlow(
                EventSyncOverview(
                    nativeCapability = com.nothingwear.protocol.watchs4.XiaomiWatchS4ProtocolAdapter().getCapabilityReport(),
                    bridgeSettings = com.nothingwear.app.domain.model.ReminderBridgeSettings(),
                    upcomingPlannedReminders = emptyList(),
                    validatedPayloadCount = 0,
                ),
            )
            override suspend fun setReminderBridgeEnabled(enabled: Boolean) = BridgeActionResult.Cleared
            override suspend fun setReminderLeadTime(leadTime: ReminderLeadTime) = BridgeActionResult.Scheduled(0)
            override suspend fun rescheduleUpcomingReminders() = BridgeActionResult.Scheduled(0)
            override suspend fun sendTestWatchAlert() = BridgeActionResult.TestNotificationSent
            override suspend fun scheduleDelayedTestWatchAlert(delaySeconds: Int) =
                BridgeActionResult.DelayedTestScheduled(delaySeconds)
        }

        val viewModel = CalendarViewModel(
            calendarRepository = fakeRepo,
            observeCalendars = ObserveCalendarsUseCase(fakeRepo),
            observeCalendarSyncMetadata = ObserveCalendarSyncMetadataUseCase(fakeRepo),
            syncDeviceCalendars = SyncDeviceCalendarsUseCase(fakeRepo),
            toggleCalendarSelection = ToggleCalendarSelectionUseCase(fakeRepo),
            createCalendarEvent = CreateCalendarEventUseCase(fakeRepo),
            deleteCalendarEvent = DeleteCalendarEventUseCase(fakeRepo),
            rescheduleWatchReminders = RescheduleWatchRemindersUseCase(stubEventSyncRepo),
            permissionChecker = CalendarPermissionChecker { false },
            timeProvider = fixedTime,
        )

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        // Default MONTH_GRID builds 42 cells (6x7) and shows todayEvent on Nov 20
        assertFalse(viewModel.uiState.value.hasPermission)
        assertEquals(CalendarViewMode.MONTH_GRID, viewModel.uiState.value.viewMode)
        assertEquals(42, viewModel.uiState.value.monthCells.size)
        assertEquals(1, viewModel.uiState.value.selectedDateEvents.size)
        assertEquals("Sprint Planning", viewModel.uiState.value.selectedDateEvents.first().title)

        // Selecting Nov 30 in Month Grid surfaces Dentist Checkup
        viewModel.onSelectDate(LocalDate.of(2026, 11, 30))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.selectedDateEvents.size)
        assertEquals("Dentist Checkup", viewModel.uiState.value.selectedDateEvents.first().title)

        // Switch to AGENDA_LIST & UPCOMING (30 days) reveals both events
        viewModel.onSelectViewMode(CalendarViewMode.AGENDA_LIST)
        viewModel.onSelectRange(AgendaRange.UPCOMING)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.totalFilteredEvents)

        // Search query filters down to matching event
        viewModel.onSearchQueryChanged("dentist")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.totalFilteredEvents)
        assertEquals("Dentist Checkup", viewModel.uiState.value.groupedEvents.first().events.first().title)

        // Granting permission triggers sync
        viewModel.onPermissionResult(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasPermission)
        assertEquals(1, fakeRepo.syncCount)

        // Creating a new event delegates to repository and closes sheet
        viewModel.onOpenCreateEventSheet(LocalDate.of(2026, 11, 22))
        viewModel.onDraftTitleChanged("Watch S4 Sync Test")
        viewModel.onSaveCreatedEvent()
        advanceUntilIdle()
        assertEquals(1, fakeRepo.createdInputs.size)
        assertEquals("Watch S4 Sync Test", fakeRepo.createdInputs.first().title)
        assertFalse(viewModel.uiState.value.showCreateEventSheet)

        // Deleting an event delegates to repository
        val toDelete = viewModel.uiState.value.groupedEvents.first().events.first()
        viewModel.onRequestDeleteEvent(toDelete)
        viewModel.onConfirmDeleteEvent()
        advanceUntilIdle()
        assertEquals(listOf(2L), fakeRepo.deletedEventIds)

        job.cancel()
    }

    private class FakeCalendarRepository(
        initialCalendars: List<CalendarSource>,
        initialEvents: List<CalendarEvent>,
        lastSyncedAtMillis: Long?,
    ) : CalendarRepository {
        private val calendarsFlow = MutableStateFlow(initialCalendars)
        private val eventsFlow = MutableStateFlow(initialEvents)
        private val metadataFlow = MutableStateFlow(
            CalendarSyncMetadata(
                lastSyncedAtMillis = lastSyncedAtMillis,
                selectedCalendarCount = initialCalendars.count { it.isSelected },
                totalCalendarCount = initialCalendars.size,
                cachedEventCount = initialEvents.size,
            ),
        )
        var syncCount: Int = 0
        val createdInputs = mutableListOf<CreateCalendarEventInput>()
        val deletedEventIds = mutableListOf<Long>()

        override fun observeCalendars(): Flow<List<CalendarSource>> = calendarsFlow
        override fun observeSelectedEvents(): Flow<List<CalendarEvent>> = eventsFlow
        override fun observeEventDetail(instanceKey: String): Flow<CalendarEvent?> =
            eventsFlow.map { list -> list.firstOrNull { it.instanceKey == instanceKey } }

        override fun observeSyncMetadata(): Flow<CalendarSyncMetadata> = metadataFlow

        override suspend fun syncFromDevice(): CalendarSyncResult {
            syncCount++
            return CalendarSyncResult.Success(
                syncedCalendars = calendarsFlow.value.size,
                syncedEvents = eventsFlow.value.size,
                syncedAtMillis = metadataFlow.value.lastSyncedAtMillis ?: 0L,
            )
        }

        override suspend fun setCalendarSelected(calendarId: Long, selected: Boolean) {
            calendarsFlow.value = calendarsFlow.value.map {
                if (it.id == calendarId) it.copy(isSelected = selected) else it
            }
        }

        override suspend fun createEvent(input: CreateCalendarEventInput): CalendarMutationResult {
            createdInputs += input
            return CalendarMutationResult.Created(999L)
        }

        override suspend fun deleteEvent(eventId: Long): CalendarMutationResult {
            deletedEventIds += eventId
            return CalendarMutationResult.Deleted
        }
    }
}
