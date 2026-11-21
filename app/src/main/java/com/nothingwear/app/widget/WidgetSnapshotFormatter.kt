package com.nothingwear.app.widget

import com.nothingwear.app.domain.model.BleHeartRateMonitorState
import com.nothingwear.app.domain.model.CalendarEvent
import com.nothingwear.app.domain.model.HealthDashboardSnapshot
import com.nothingwear.app.domain.model.MetricAvailabilityStatus
import com.nothingwear.app.domain.util.AgendaTimeWindows
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class NothingWidgetViewData(
    val stepsText: String,
    val heartRateText: String,
    val sleepText: String,
    val nextEventMeta: String,
    val nextEventTitle: String,
    val updatedAtText: String,
)

/**
 * Pure-Kotlin formatter for the NothingWear Android Home Screen Widget.
 * Strictly renders verified metrics only and falls back to "--" when unavailable.
 */
object WidgetSnapshotFormatter {

    private val shortTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    fun format(
        events: List<CalendarEvent>,
        health: HealthDashboardSnapshot,
        bleHeartRate: BleHeartRateMonitorState,
        nowMillis: Long,
        zoneId: ZoneId,
    ): NothingWidgetViewData {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()

        val stepsText = if (health.stepsToday.status == MetricAvailabilityStatus.VERIFIED) {
            health.stepsToday.primaryValue
        } else {
            "--"
        }

        val liveBle = bleHeartRate.latestSample.takeIf { bleHeartRate.isListening }
        val heartRateText = when {
            liveBle != null -> "${liveBle.bpm} BPM"
            health.heartRateSummary.status == MetricAvailabilityStatus.VERIFIED ->
                "${health.heartRateSummary.primaryValue} BPM"
            health.lastVerifiedHeartRate != null ->
                "${health.lastVerifiedHeartRate.bpm} BPM"
            else -> "--"
        }

        val sleepText = if (health.sleepLast24h.status == MetricAvailabilityStatus.VERIFIED) {
            health.sleepLast24h.primaryValue
        } else {
            "--"
        }

        val nextEvent = events
            .filter { AgendaTimeWindows.normalizedInterval(it, zoneId).second >= nowMillis }
            .minByOrNull { AgendaTimeWindows.normalizedInterval(it, zoneId).first }

        val (eventMeta, eventTitle) = if (nextEvent != null) {
            val eventDate = AgendaTimeWindows.startLocalDate(nextEvent, zoneId)
            val dateBadge = AgendaTimeWindows.formatDateBadge(eventDate, today)
            val timeRange = AgendaTimeWindows.formatTimeRange(nextEvent, zoneId)
            "$dateBadge · $timeRange" to nextEvent.title
        } else {
            "NEXT EVENT · CALENDAR" to "No upcoming events cached"
        }

        val updatedLabel = Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .format(shortTimeFormatter)

        return NothingWidgetViewData(
            stepsText = stepsText,
            heartRateText = heartRateText,
            sleepText = sleepText,
            nextEventMeta = eventMeta,
            nextEventTitle = eventTitle,
            updatedAtText = updatedLabel,
        )
    }
}
