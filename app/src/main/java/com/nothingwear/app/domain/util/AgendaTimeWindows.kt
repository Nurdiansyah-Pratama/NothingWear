package com.nothingwear.app.domain.util

import com.nothingwear.app.domain.model.AgendaRange
import com.nothingwear.app.domain.model.CalendarEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TimeWindow(
    val startMillis: Long,
    val endMillis: Long,
)

/**
 * Pure Kotlin date/time window calculation and formatting for Calendar Engine (M1).
 * Properly normalizes Android Calendar Provider UTC all-day timestamps into local dates.
 */
object AgendaTimeWindows {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val dateHeaderFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
    private val fullDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
    private val syncTimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault())

    fun windowFor(range: AgendaRange, nowMillis: Long, zoneId: ZoneId): TimeWindow {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val startOfToday = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endDate = when (range) {
            AgendaRange.TODAY -> today.plusDays(1)
            AgendaRange.WEEK -> today.plusDays(7)
            AgendaRange.UPCOMING -> today.plusDays(30)
        }
        val endMillis = endDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        return TimeWindow(startMillis = startOfToday, endMillis = endMillis)
    }

    /**
     * Window used when querying Android Calendar Provider Instances.
     * Covers -60 days to +120 days so Month Grid browsing across adjacent months shows real events.
     */
    fun providerFetchWindow(nowMillis: Long, zoneId: ZoneId): TimeWindow {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val startMillis = today.minusDays(60).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = today.plusDays(120).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return TimeWindow(startMillis = startMillis, endMillis = endMillis)
    }

    fun dayWindow(date: LocalDate, zoneId: ZoneId): TimeWindow {
        val startMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return TimeWindow(startMillis = startMillis, endMillis = endMillis)
    }

    fun normalizedInterval(event: CalendarEvent, zoneId: ZoneId): Pair<Long, Long> {
        return if (event.isAllDay) {
            val startDate = Instant.ofEpochMilli(event.startTimeMillis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
            val rawEndDate = Instant.ofEpochMilli(event.endTimeMillis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
            val endDate = if (rawEndDate.isAfter(startDate)) rawEndDate else startDate.plusDays(1)
            val startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endMillis = endDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            startMillis to endMillis
        } else {
            val endMillis = maxOf(event.endTimeMillis, event.startTimeMillis)
            event.startTimeMillis to endMillis
        }
    }

    fun overlaps(event: CalendarEvent, window: TimeWindow, zoneId: ZoneId): Boolean {
        val (eventStart, eventEnd) = normalizedInterval(event, zoneId)
        return if (eventStart == eventEnd) {
            eventStart in window.startMillis until window.endMillis
        } else {
            eventStart < window.endMillis && eventEnd > window.startMillis
        }
    }

    fun startLocalDate(event: CalendarEvent, zoneId: ZoneId): LocalDate {
        return if (event.isAllDay) {
            Instant.ofEpochMilli(event.startTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
        } else {
            Instant.ofEpochMilli(event.startTimeMillis).atZone(zoneId).toLocalDate()
        }
    }

    fun matchesQuery(event: CalendarEvent, rawQuery: String): Boolean {
        val query = rawQuery.trim()
        if (query.isEmpty()) return true
        return event.title.contains(query, ignoreCase = true) ||
            event.calendarName.contains(query, ignoreCase = true) ||
            (event.location?.contains(query, ignoreCase = true) == true) ||
            (event.description?.contains(query, ignoreCase = true) == true)
    }

    fun formatTimeRange(event: CalendarEvent, zoneId: ZoneId): String {
        if (event.isAllDay) return "ALL DAY"
        val start = Instant.ofEpochMilli(event.startTimeMillis).atZone(zoneId).toLocalTime()
        val end = Instant.ofEpochMilli(event.endTimeMillis).atZone(zoneId).toLocalTime()
        return if (event.endTimeMillis > event.startTimeMillis) {
            "${start.format(timeFormatter)} – ${end.format(timeFormatter)}"
        } else {
            start.format(timeFormatter)
        }
    }

    fun formatDateBadge(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "TODAY · ${date.format(dateHeaderFormatter).uppercase()}"
        today.plusDays(1) -> "TOMORROW · ${date.format(dateHeaderFormatter).uppercase()}"
        else -> date.format(dateHeaderFormatter).uppercase()
    }

    fun formatFullDate(event: CalendarEvent, zoneId: ZoneId): String {
        return startLocalDate(event, zoneId).format(fullDateFormatter)
    }

    fun formatSyncTimestamp(epochMillis: Long, zoneId: ZoneId): String {
        return Instant.ofEpochMilli(epochMillis).atZone(zoneId).format(syncTimestampFormatter)
    }
}
