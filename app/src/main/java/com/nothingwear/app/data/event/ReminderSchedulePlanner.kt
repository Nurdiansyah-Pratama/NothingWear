package com.nothingwear.app.data.event

import com.nothingwear.app.domain.model.CalendarEvent
import com.nothingwear.app.domain.model.PlannedEventReminder
import com.nothingwear.app.domain.model.ReminderLeadTime
import com.nothingwear.app.domain.util.AgendaTimeWindows
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Pure Kotlin planner selecting which upcoming Room-cached events should be scheduled
 * for Watch Reminder Bridge notifications (48-hour lookahead window).
 */
object ReminderSchedulePlanner {

    private const val LOOKAHEAD_HOURS = 48L
    private const val ALL_DAY_DEFAULT_HOUR = 8

    fun planReminders(
        events: List<CalendarEvent>,
        leadTime: ReminderLeadTime,
        nowMillis: Long,
        zoneId: ZoneId,
    ): List<PlannedEventReminder> {
        val horizonMillis = nowMillis + TimeUnit.HOURS.toMillis(LOOKAHEAD_HOURS)
        val leadMillis = TimeUnit.MINUTES.toMillis(leadTime.minutes.toLong())

        return events
            .asSequence()
            .mapNotNull { event ->
                val effectiveStartMillis = if (event.isAllDay) {
                    AgendaTimeWindows.startLocalDate(event, zoneId)
                        .atTime(ALL_DAY_DEFAULT_HOUR, 0)
                        .atZone(zoneId)
                        .toInstant()
                        .toEpochMilli()
                } else {
                    event.startTimeMillis
                }

                // Skip events that already ended or started in the past
                if (effectiveStartMillis <= nowMillis || effectiveStartMillis > horizonMillis) {
                    return@mapNotNull null
                }

                // If lead time puts trigger slightly in the past while event itself is still upcoming,
                // schedule immediately (+5s) instead of silently dropping the reminder.
                val rawTrigger = effectiveStartMillis - leadMillis
                val triggerAt = maxOf(rawTrigger, nowMillis + 5_000L)

                PlannedEventReminder(
                    instanceKey = event.instanceKey,
                    title = event.title,
                    timeRangeLabel = AgendaTimeWindows.formatTimeRange(event, zoneId),
                    triggerAtMillis = triggerAt,
                    triggerLabel = AgendaTimeWindows.formatSyncTimestamp(triggerAt, zoneId),
                    calendarName = event.calendarName,
                    location = event.location,
                )
            }
            .sortedBy { it.triggerAtMillis }
            .take(MAX_SCHEDULED_REMINDERS)
            .toList()
    }

    const val MAX_SCHEDULED_REMINDERS = 25
}
