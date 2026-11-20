package com.nothingwear.app.domain.util

import com.nothingwear.app.domain.model.AgendaRange
import com.nothingwear.app.domain.model.CalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class AgendaTimeWindowsTest {

    private val wibZone: ZoneId = ZoneId.of("Asia/Jakarta")
    private val nowMillis: Long = LocalDate.of(2026, 11, 20)
        .atTime(10, 0)
        .atZone(wibZone)
        .toInstant()
        .toEpochMilli()

    @Test
    fun allDayUtcEventStaysStrictlyOnItsLocalDateInPositiveOffsetTimezone() {
        val nov21StartUtc = LocalDate.of(2026, 11, 21).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val nov22StartUtc = LocalDate.of(2026, 11, 22).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val allDayEvent = sampleEvent(
            title = "Conference Day",
            startMillis = nov21StartUtc,
            endMillis = nov22StartUtc,
            isAllDay = true,
        )

        val nov20Window = AgendaTimeWindows.windowFor(AgendaRange.TODAY, nowMillis, wibZone)
        val nov21Millis = LocalDate.of(2026, 11, 21).atTime(9, 0).atZone(wibZone).toInstant().toEpochMilli()
        val nov22Millis = LocalDate.of(2026, 11, 22).atTime(9, 0).atZone(wibZone).toInstant().toEpochMilli()
        val nov21Window = AgendaTimeWindows.windowFor(AgendaRange.TODAY, nov21Millis, wibZone)
        val nov22Window = AgendaTimeWindows.windowFor(AgendaRange.TODAY, nov22Millis, wibZone)

        assertFalse(AgendaTimeWindows.overlaps(allDayEvent, nov20Window, wibZone))
        assertTrue(AgendaTimeWindows.overlaps(allDayEvent, nov21Window, wibZone))
        assertFalse(AgendaTimeWindows.overlaps(allDayEvent, nov22Window, wibZone))
        assertEquals(LocalDate.of(2026, 11, 21), AgendaTimeWindows.startLocalDate(allDayEvent, wibZone))
        assertEquals("ALL DAY", AgendaTimeWindows.formatTimeRange(allDayEvent, wibZone))
    }

    @Test
    fun windowRangesFilterTimedEventsAccurately() {
        val todayEvent = sampleEvent(
            title = "Standup",
            startMillis = LocalDate.of(2026, 11, 20).atTime(14, 0).atZone(wibZone).toInstant().toEpochMilli(),
            endMillis = LocalDate.of(2026, 11, 20).atTime(14, 30).atZone(wibZone).toInstant().toEpochMilli(),
        )
        val day5Event = sampleEvent(
            title = "Weekly Review",
            startMillis = LocalDate.of(2026, 11, 25).atTime(16, 0).atZone(wibZone).toInstant().toEpochMilli(),
            endMillis = LocalDate.of(2026, 11, 25).atTime(17, 0).atZone(wibZone).toInstant().toEpochMilli(),
        )
        val day15Event = sampleEvent(
            title = "Monthly Planning",
            startMillis = LocalDate.of(2026, 12, 5).atTime(9, 0).atZone(wibZone).toInstant().toEpochMilli(),
            endMillis = LocalDate.of(2026, 12, 5).atTime(10, 0).atZone(wibZone).toInstant().toEpochMilli(),
        )

        val todayWin = AgendaTimeWindows.windowFor(AgendaRange.TODAY, nowMillis, wibZone)
        val weekWin = AgendaTimeWindows.windowFor(AgendaRange.WEEK, nowMillis, wibZone)
        val upcomingWin = AgendaTimeWindows.windowFor(AgendaRange.UPCOMING, nowMillis, wibZone)

        assertTrue(AgendaTimeWindows.overlaps(todayEvent, todayWin, wibZone))
        assertFalse(AgendaTimeWindows.overlaps(day5Event, todayWin, wibZone))

        assertTrue(AgendaTimeWindows.overlaps(day5Event, weekWin, wibZone))
        assertFalse(AgendaTimeWindows.overlaps(day15Event, weekWin, wibZone))

        assertTrue(AgendaTimeWindows.overlaps(day15Event, upcomingWin, wibZone))
    }

    @Test
    fun matchesQuerySearchesTitleLocationDescriptionAndCalendarName() {
        val event = sampleEvent(
            title = "Design Sync",
            calendarName = "Work Google",
            location = "Room 4B",
            description = "Watch face 466px layout review",
            startMillis = nowMillis,
            endMillis = nowMillis + 3_600_000L,
        )

        assertTrue(AgendaTimeWindows.matchesQuery(event, ""))
        assertTrue(AgendaTimeWindows.matchesQuery(event, "design"))
        assertTrue(AgendaTimeWindows.matchesQuery(event, "room 4b"))
        assertTrue(AgendaTimeWindows.matchesQuery(event, "466px"))
        assertTrue(AgendaTimeWindows.matchesQuery(event, "google"))
        assertFalse(AgendaTimeWindows.matchesQuery(event, "dentist"))
    }

    private fun sampleEvent(
        title: String,
        startMillis: Long,
        endMillis: Long,
        isAllDay: Boolean = false,
        calendarName: String = "Primary",
        location: String? = null,
        description: String? = null,
    ) = CalendarEvent(
        instanceKey = "1_$startMillis",
        eventId = 1L,
        calendarId = 10L,
        calendarName = calendarName,
        calendarColor = 0xFFD71921.toInt(),
        title = title,
        description = description,
        location = location,
        startTimeMillis = startMillis,
        endTimeMillis = endMillis,
        isAllDay = isAllDay,
        timezone = "Asia/Jakarta",
        lastSyncedAtMillis = nowMillis,
    )
}
