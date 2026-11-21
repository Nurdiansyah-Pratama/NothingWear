package com.nothingwear.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.core.database.dao.CalendarDao
import com.nothingwear.app.core.database.entity.CalendarAccountEntity
import com.nothingwear.app.core.database.entity.CalendarEventEntity
import com.nothingwear.app.core.permissions.CalendarPermissionChecker
import com.nothingwear.app.data.calendar.DeviceCalendarDataSource
import com.nothingwear.app.data.calendar.RawDeviceCalendar
import com.nothingwear.app.data.calendar.RawDeviceEventInstance
import com.nothingwear.app.domain.model.CalendarMutationResult
import com.nothingwear.app.domain.model.CalendarSyncResult
import com.nothingwear.app.domain.model.CreateCalendarEventInput
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class OfflineFirstCalendarRepositoryTest {

    private val fixedTimeProvider = object : TimeProvider {
        override fun nowMillis(): Long = 1_763_632_800_000L
        override fun zoneId(): ZoneId = ZoneId.of("Asia/Jakarta")
    }

    @Test
    fun syncReturnsPermissionDeniedWithoutTouchingDatabase() = runTest {
        val dao = mockk<CalendarDao>(relaxed = true)
        val dataSource = mockk<DeviceCalendarDataSource>()
        val dataStore = mockk<DataStore<Preferences>>()
        every { dataStore.data } returns flowOf(emptyPreferences())
        val permissionChecker = CalendarPermissionChecker { false }

        val repository = OfflineFirstCalendarRepository(
            calendarDao = dao,
            deviceCalendarDataSource = dataSource,
            dataStore = dataStore,
            permissionChecker = permissionChecker,
            timeProvider = fixedTimeProvider,
        )

        val result = repository.syncFromDevice()

        assertEquals(CalendarSyncResult.PermissionDenied, result)
        coVerify(exactly = 0) { dao.replaceCalendarCache(any(), any()) }
    }

    @Test
    fun syncPreservesExistingUserCalendarSelectionAndDeduplicatesInstances() = runTest {
        val dao = mockk<CalendarDao>(relaxed = true)
        val dataSource = mockk<DeviceCalendarDataSource>()
        val dataStore = mockk<DataStore<Preferences>>()
        every { dataStore.data } returns flowOf(emptyPreferences())
        coEvery { dataStore.updateData(any()) } returns emptyPreferences()

        // Existing user previously unchecked calendar #2
        coEvery { dao.getAllCalendars() } returns listOf(
            CalendarAccountEntity(
                calendarId = 2L,
                displayName = "Holidays",
                accountName = "user@gmail.com",
                accountType = "com.google",
                color = 0xFF10B981.toInt(),
                isSelected = false,
                isPrimary = false,
            ),
        )

        coEvery { dataSource.fetchCalendars() } returns listOf(
            RawDeviceCalendar(
                calendarId = 1L,
                displayName = "Personal",
                accountName = "user@gmail.com",
                accountType = "com.google",
                color = 0xFFD71921.toInt(),
                isVisible = true,
                isPrimary = true,
            ),
            RawDeviceCalendar(
                calendarId = 2L,
                displayName = "Holidays",
                accountName = "user@gmail.com",
                accountType = "com.google",
                color = 0xFF10B981.toInt(),
                isVisible = true,
                isPrimary = false,
            ),
        )

        val duplicateInstance = RawDeviceEventInstance(
            eventId = 100L,
            calendarId = 1L,
            calendarDisplayName = "Personal",
            calendarColor = 0xFFD71921.toInt(),
            title = "Team Sync",
            description = null,
            location = "Online",
            startTimeMillis = 1_763_636_400_000L,
            endTimeMillis = 1_763_640_000_000L,
            isAllDay = false,
            timezone = "Asia/Jakarta",
        )
        coEvery { dataSource.fetchEventInstances(any(), any()) } returns listOf(
            duplicateInstance,
            duplicateInstance,
        )

        val calendarsSlot = slot<List<CalendarAccountEntity>>()
        val eventsSlot = slot<List<CalendarEventEntity>>()
        coEvery { dao.replaceCalendarCache(capture(calendarsSlot), capture(eventsSlot)) } returns Unit

        val repository = OfflineFirstCalendarRepository(
            calendarDao = dao,
            deviceCalendarDataSource = dataSource,
            dataStore = dataStore,
            permissionChecker = { true },
            timeProvider = fixedTimeProvider,
        )

        val result = repository.syncFromDevice()

        assertTrue(result is CalendarSyncResult.Success)
        val success = result as CalendarSyncResult.Success
        assertEquals(2, success.syncedCalendars)
        assertEquals(1, success.syncedEvents)
        assertTrue(calendarsSlot.captured.first { it.calendarId == 1L }.isSelected)
        assertFalse(calendarsSlot.captured.first { it.calendarId == 2L }.isSelected)
        assertEquals(1, eventsSlot.captured.size)
    }

    @Test
    fun createAndDeleteEventRespectWritePermissionAndResyncCache() = runTest {
        val dao = mockk<CalendarDao>(relaxed = true)
        val dataSource = mockk<DeviceCalendarDataSource>(relaxed = true)
        val dataStore = mockk<DataStore<Preferences>>()
        every { dataStore.data } returns flowOf(emptyPreferences())
        coEvery { dataStore.updateData(any()) } returns emptyPreferences()
        coEvery { dataSource.createEvent(any()) } returns 501L
        coEvery { dataSource.deleteEvent(501L) } returns 1
        coEvery { dataSource.fetchCalendars() } returns emptyList()
        coEvery { dataSource.fetchEventInstances(any(), any()) } returns emptyList()
        coEvery { dao.getAllCalendars() } returns emptyList()

        val repository = OfflineFirstCalendarRepository(
            calendarDao = dao,
            deviceCalendarDataSource = dataSource,
            dataStore = dataStore,
            permissionChecker = { true },
            timeProvider = fixedTimeProvider,
        )

        val created = repository.createEvent(
            CreateCalendarEventInput(
                calendarId = 1L,
                title = "New Meeting",
                startTimeMillis = 1_763_636_400_000L,
                endTimeMillis = 1_763_640_000_000L,
                isAllDay = false,
                timezone = "Asia/Jakarta",
            ),
        )
        assertEquals(CalendarMutationResult.Created(501L), created)

        val deleted = repository.deleteEvent(501L)
        assertEquals(CalendarMutationResult.Deleted, deleted)
    }
}
