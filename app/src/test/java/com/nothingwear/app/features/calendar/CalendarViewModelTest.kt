package com.nothingwear.app.features.calendar

import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.core.permissions.CalendarPermissionChecker
import com.nothingwear.app.domain.model.AgendaRange
import com.nothingwear.app.domain.model.CalendarEvent
import com.nothingwear.app.domain.model.CalendarSource
import com.nothingwear.app.domain.model.CalendarSyncMetadata
import com.nothingwear.app.domain.model.CalendarSyncResult
import com.nothingwear.app.domain.repository.CalendarRepository
import com.nothingwear.app.domain.usecase.ObserveCalendarSyncMetadataUseCase
import com.nothingwear.app.domain.usecase.ObserveCalendarsUseCase
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

        val viewModel = CalendarViewModel(
            calendarRepository = fakeRepo,
            observeCalendars = ObserveCalendarsUseCase(fakeRepo),
            observeCalendarSyncMetadata = ObserveCalendarSyncMetadataUseCase(fakeRepo),
            syncDeviceCalendars = SyncDeviceCalendarsUseCase(fakeRepo),
            toggleCalendarSelection = ToggleCalendarSelectionUseCase(fakeRepo),
            permissionChecker = CalendarPermissionChecker { false },
            timeProvider = fixedTime,
        )

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        // Even with permission denied, offline Room cache in WEEK window (default) shows todayEvent
        assertFalse(viewModel.uiState.value.hasPermission)
        assertEquals(1, viewModel.uiState.value.totalFilteredEvents)

        // Switching to UPCOMING (30 days) reveals both events
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
    }
}
