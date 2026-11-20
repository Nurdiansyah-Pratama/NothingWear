package com.nothingwear.app.data.repository

import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.core.permissions.CalendarPermissionChecker
import com.nothingwear.app.domain.model.AgendaItem
import com.nothingwear.app.domain.model.AgendaRange
import com.nothingwear.app.domain.model.CalendarEvent
import com.nothingwear.app.domain.model.CalendarOverview
import com.nothingwear.app.domain.model.CalendarSyncState
import com.nothingwear.app.domain.model.DashboardOverview
import com.nothingwear.app.domain.model.DeviceStatusSummary
import com.nothingwear.app.domain.model.WatchConnectionState
import com.nothingwear.app.domain.repository.CalendarRepository
import com.nothingwear.app.domain.repository.DashboardRepository
import com.nothingwear.app.domain.util.AgendaTimeWindows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * M1 implementation of [DashboardRepository].
 *
 * Combines live Room-backed local calendar data with honest uninitialized states for
 * BLE discovery (M2) and native Watch S4 event sync (M3/M4).
 */
@Singleton
class FoundationDashboardRepository @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val permissionChecker: CalendarPermissionChecker,
    private val timeProvider: TimeProvider,
) : DashboardRepository {

    // TODO(M2): Combine live read-only BLE discovery/GATT status flow from DeviceRepository.
    // TODO(M4): Combine verified protocol-adapter state once M3 reverse engineering is complete.
    override fun observeDashboardOverview(): Flow<DashboardOverview> = combine(
        calendarRepository.observeSelectedEvents(),
        calendarRepository.observeSyncMetadata(),
    ) { selectedEvents, metadata ->
        val nowMillis = timeProvider.nowMillis()
        val zoneId = timeProvider.zoneId()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val todayWindow = AgendaTimeWindows.windowFor(AgendaRange.TODAY, nowMillis, zoneId)
        val upcomingWindow = AgendaTimeWindows.windowFor(AgendaRange.UPCOMING, nowMillis, zoneId)

        val todayEvents = selectedEvents
            .filter { AgendaTimeWindows.overlaps(it, todayWindow, zoneId) }
            .sortedWith(
                compareBy<CalendarEvent> { AgendaTimeWindows.normalizedInterval(it, zoneId).first }
                    .thenByDescending { it.isAllDay },
            )
            .map { it.toAgendaItem(today, zoneId) }

        val upcomingEvents = selectedEvents
            .filter { event ->
                AgendaTimeWindows.overlaps(event, upcomingWindow, zoneId) &&
                    AgendaTimeWindows.startLocalDate(event, zoneId).isAfter(today)
            }
            .sortedWith(
                compareBy<CalendarEvent> { AgendaTimeWindows.normalizedInterval(it, zoneId).first }
                    .thenByDescending { it.isAllDay },
            )
            .take(5)
            .map { it.toAgendaItem(today, zoneId) }

        val hasPermission = permissionChecker.hasReadCalendarPermission()
        val (localState, localMessage) = when {
            metadata.lastSyncedAtMillis != null -> {
                val syncedAt = AgendaTimeWindows.formatSyncTimestamp(metadata.lastSyncedAtMillis, zoneId)
                val permissionSuffix = if (!hasPermission) " · permission revoked (offline cache)" else ""
                CalendarSyncState.LOCAL_ONLY to
                    "Local cache: ${metadata.cachedEventCount} events (${metadata.selectedCalendarCount}/${metadata.totalCalendarCount} calendars) · synced $syncedAt$permissionSuffix"
            }

            !hasPermission -> {
                CalendarSyncState.PERMISSION_REQUIRED to
                    "Grant READ_CALENDAR in the Calendar tab to read Android & Google calendars."
            }

            else -> {
                CalendarSyncState.NOT_CONFIGURED to
                    "Calendar permission granted. Open the Calendar tab and tap Sync to load events offline."
            }
        }

        DashboardOverview(
            deviceStatus = DeviceStatusSummary(
                targetDeviceName = "Xiaomi Watch S4",
                connectionState = WatchConnectionState.DISCONNECTED,
                statusDetail = "BLE discovery not yet enabled (planned in M2). Mi Fitness pairing is untouched.",
                batteryPercent = null,
                bleDiscoveryAvailable = false,
            ),
            calendarOverview = CalendarOverview(
                localCalendarState = localState,
                localStatusMessage = localMessage,
                nativeWatchSyncState = WatchConnectionState.PROTOCOL_UNKNOWN,
                nativeWatchSyncMessage = "Local calendar reading does NOT sync to Watch S4 Events. Requires M3 protocol research and M4 verification.",
                todayEvents = todayEvents,
                upcomingEvents = upcomingEvents,
            ),
            currentMilestone = "M1 · Calendar",
        )
    }

    private fun CalendarEvent.toAgendaItem(
        today: java.time.LocalDate,
        zoneId: java.time.ZoneId,
    ): AgendaItem = AgendaItem(
        id = instanceKey,
        title = title,
        timeLabel = AgendaTimeWindows.formatTimeRange(this, zoneId),
        dateLabel = AgendaTimeWindows.formatDateBadge(
            date = AgendaTimeWindows.startLocalDate(this, zoneId),
            today = today,
        ),
        calendarName = calendarName,
        calendarColor = calendarColor,
        location = location,
    )
}
