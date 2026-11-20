package com.nothingwear.app.data.event

import com.nothingwear.app.domain.model.CalendarEvent
import com.nothingwear.app.domain.model.ReminderLeadTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class ReminderSchedulePlannerTest {

    private val wib = ZoneId.of("Asia/Jakarta")
    private val nowMillis = LocalDate.of(2026, 11, 21).atTime(9, 0).atZone(wib).toInstant().toEpochMilli()

    @Test
    fun plansUpcoming48hEventsWithLeadTimeAndSkipsPastOrFarFutureEvents() {
        val pastEvent = event(
            key = "past",
            title = "Past Meeting",
            startMillis = LocalDate.of(2026, 11, 21).atTime(8, 0).atZone(wib).toInstant().toEpochMilli(),
        )
        val in2Hours = event(
            key = "soon",
            title = "Design Sync",
            startMillis = LocalDate.of(2026, 11, 21).atTime(11, 0).atZone(wib).toInstant().toEpochMilli(),
        )
        val in5Days = event(
            key = "far",
            title = "Next Week",
            startMillis = LocalDate.of(2026, 11, 26).atTime(11, 0).atZone(wib).toInstant().toEpochMilli(),
        )

        val planned = ReminderSchedulePlanner.planReminders(
            events = listOf(pastEvent, in2Hours, in5Days),
            leadTime = ReminderLeadTime.FIFTEEN_MIN,
            nowMillis = nowMillis,
            zoneId = wib,
        )

        assertEquals(1, planned.size)
        assertEquals("Design Sync", planned.first().title)
        val expectedTrigger = LocalDate.of(2026, 11, 21).atTime(10, 45).atZone(wib).toInstant().toEpochMilli()
        assertEquals(expectedTrigger, planned.first().triggerAtMillis)
    }

    @Test
    fun schedulesAllDayTomorrowEventAt0800LocalMinusLeadTime() {
        val tomorrowAllDayUtc = LocalDate.of(2026, 11, 22).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endUtc = LocalDate.of(2026, 11, 23).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val allDay = event(
            key = "allday",
            title = "Team Offsite",
            startMillis = tomorrowAllDayUtc,
            endMillis = endUtc,
            isAllDay = true,
        )

        val planned = ReminderSchedulePlanner.planReminders(
            events = listOf(allDay),
            leadTime = ReminderLeadTime.THIRTY_MIN,
            nowMillis = nowMillis,
            zoneId = wib,
        )

        assertEquals(1, planned.size)
        val expected = LocalDate.of(2026, 11, 22).atTime(7, 30).atZone(wib).toInstant().toEpochMilli()
        assertEquals(expected, planned.first().triggerAtMillis)
        assertTrue(planned.first().timeRangeLabel == "ALL DAY")
    }

    private fun event(
        key: String,
        title: String,
        startMillis: Long,
        endMillis: Long = startMillis + 3_600_000L,
        isAllDay: Boolean = false,
    ) = CalendarEvent(
        instanceKey = key,
        eventId = 1L,
        calendarId = 1L,
        calendarName = "Google",
        calendarColor = 0xFFD71921.toInt(),
        title = title,
        description = null,
        location = "Office",
        startTimeMillis = startMillis,
        endTimeMillis = endMillis,
        isAllDay = isAllDay,
        timezone = "Asia/Jakarta",
        lastSyncedAtMillis = nowMillis,
    )
}
