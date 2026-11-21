package com.nothingwear.app.widget

import com.nothingwear.app.core.bluetooth.HeartRateSensorContact
import com.nothingwear.app.domain.model.BleHeartRateMonitorState
import com.nothingwear.app.domain.model.BleHeartRateSample
import com.nothingwear.app.domain.model.CalendarEvent
import com.nothingwear.app.domain.model.HealthDashboardSnapshot
import com.nothingwear.app.domain.model.HealthMetricSummary
import com.nothingwear.app.domain.model.LastVerifiedHeartRate
import com.nothingwear.app.domain.model.MetricAvailabilityStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WidgetSnapshotFormatterTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val nowMillis = LocalDate.of(2026, 11, 21).atTime(11, 30).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun formatsVerifiedHealthAndNextUpcomingEventHonestly() {
        val pastEvent = CalendarEvent(
            instanceKey = "1_past",
            eventId = 1L,
            calendarId = 1L,
            calendarName = "Google",
            calendarColor = 0xFFD71921.toInt(),
            title = "Earlier Standup",
            description = null,
            location = null,
            startTimeMillis = LocalDate.of(2026, 11, 21).atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
            endTimeMillis = LocalDate.of(2026, 11, 21).atTime(10, 0).atZone(zone).toInstant().toEpochMilli(),
            isAllDay = false,
            timezone = "Asia/Jakarta",
            lastSyncedAtMillis = nowMillis,
        )
        val upcomingEvent = CalendarEvent(
            instanceKey = "2_next",
            eventId = 2L,
            calendarId = 1L,
            calendarName = "Google",
            calendarColor = 0xFFD71921.toInt(),
            title = "Design Review",
            description = null,
            location = null,
            startTimeMillis = LocalDate.of(2026, 11, 21).atTime(14, 0).atZone(zone).toInstant().toEpochMilli(),
            endTimeMillis = LocalDate.of(2026, 11, 21).atTime(15, 0).atZone(zone).toInstant().toEpochMilli(),
            isAllDay = false,
            timezone = "Asia/Jakarta",
            lastSyncedAtMillis = nowMillis,
        )

        val health = HealthDashboardSnapshot(
            stepsToday = HealthMetricSummary(
                title = "Steps Today",
                primaryValue = "5,820",
                unit = "steps",
                status = MetricAvailabilityStatus.VERIFIED,
                sourceLabel = "Health Connect",
                detailNote = "",
            ),
            sleepLast24h = HealthMetricSummary(
                title = "Sleep (Recent)",
                primaryValue = "7h 15m",
                unit = "",
                status = MetricAvailabilityStatus.VERIFIED,
                sourceLabel = "Health Connect",
                detailNote = "",
            ),
            lastVerifiedHeartRate = LastVerifiedHeartRate(
                bpm = 72,
                timestampMillis = nowMillis - 600_000L,
                sourceLabel = "Watch S4 Live BLE (0x2A37)",
            ),
        )

        val data = WidgetSnapshotFormatter.format(
            events = listOf(pastEvent, upcomingEvent),
            health = health,
            bleHeartRate = BleHeartRateMonitorState(),
            nowMillis = nowMillis,
            zoneId = zone,
        )

        assertEquals("5,820", data.stepsText)
        assertEquals("72 BPM", data.heartRateText)
        assertEquals("7h 15m", data.sleepText)
        assertEquals("Design Review", data.nextEventTitle)
        assertEquals("11:30", data.updatedAtText)
    }

    @Test
    fun prefersLiveBleHeartRateAndUsesDashesWhenNoDataAvailable() {
        val emptyData = WidgetSnapshotFormatter.format(
            events = emptyList(),
            health = HealthDashboardSnapshot(),
            bleHeartRate = BleHeartRateMonitorState(),
            nowMillis = nowMillis,
            zoneId = zone,
        )
        assertEquals("--", emptyData.stepsText)
        assertEquals("--", emptyData.heartRateText)
        assertEquals("--", emptyData.sleepText)
        assertEquals("No upcoming events cached", emptyData.nextEventTitle)

        val liveData = WidgetSnapshotFormatter.format(
            events = emptyList(),
            health = HealthDashboardSnapshot(),
            bleHeartRate = BleHeartRateMonitorState(
                isServiceExposed = true,
                isListening = true,
                latestSample = BleHeartRateSample(81, nowMillis, HeartRateSensorContact.CONTACT_DETECTED),
            ),
            nowMillis = nowMillis,
            zoneId = zone,
        )
        assertEquals("81 BPM", liveData.heartRateText)
    }
}
