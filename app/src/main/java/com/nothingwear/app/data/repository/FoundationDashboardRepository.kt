package com.nothingwear.app.data.repository

import com.nothingwear.app.core.bluetooth.WatchMatchConfidence
import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.core.permissions.CalendarPermissionChecker
import com.nothingwear.app.domain.model.AgendaItem
import com.nothingwear.app.domain.model.AgendaRange
import com.nothingwear.app.domain.model.BluetoothAdapterStatus
import com.nothingwear.app.domain.model.CalendarEvent
import com.nothingwear.app.domain.model.CalendarOverview
import com.nothingwear.app.domain.model.CalendarSyncState
import com.nothingwear.app.domain.model.DashboardHealthOverview
import com.nothingwear.app.domain.model.DashboardOverview
import com.nothingwear.app.domain.model.DeviceStatusSummary
import com.nothingwear.app.domain.model.HealthConnectAvailability
import com.nothingwear.app.domain.model.MetricAvailabilityStatus
import com.nothingwear.app.domain.model.WatchConnectionState
import com.nothingwear.app.domain.repository.CalendarRepository
import com.nothingwear.app.domain.repository.DashboardRepository
import com.nothingwear.app.domain.repository.DeviceDiagnosticsRepository
import com.nothingwear.app.domain.repository.EventSyncRepository
import com.nothingwear.app.domain.repository.HealthRepository
import com.nothingwear.app.domain.util.AgendaTimeWindows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * M6 implementation of [DashboardRepository].
 *
 * Combines live Room-backed local calendar data, live read-only BLE diagnostic state
 * (including standard BLE Heart Rate 0x2A37 when streaming), verified Android Health Connect
 * metrics, and isolated Watch S4 protocol-adapter / Reminder Bridge status honestly.
 */
@Singleton
class FoundationDashboardRepository @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val deviceDiagnosticsRepository: DeviceDiagnosticsRepository,
    private val eventSyncRepository: EventSyncRepository,
    private val healthRepository: HealthRepository,
    private val permissionChecker: CalendarPermissionChecker,
    private val timeProvider: TimeProvider,
) : DashboardRepository {

    override fun observeDashboardOverview(): Flow<DashboardOverview> = combine(
        calendarRepository.observeSelectedEvents(),
        calendarRepository.observeSyncMetadata(),
        deviceDiagnosticsRepository.observeDiagnostics(),
        eventSyncRepository.observeEventSyncOverview(),
        healthRepository.observeHealthSnapshot(),
    ) { selectedEvents, metadata, bleSnapshot, syncOverview, healthSnapshot ->
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

        val session = bleSnapshot.gattSession
        val detectedWatch = bleSnapshot.devices.firstOrNull {
            it.matchConfidence == WatchMatchConfidence.LIKELY_WATCH_S4 ||
                it.matchConfidence == WatchMatchConfidence.XIAOMI_ECOSYSTEM
        }

        val deviceStatus = when {
            bleSnapshot.adapterStatus == BluetoothAdapterStatus.UNSUPPORTED -> DeviceStatusSummary(
                targetDeviceName = "Xiaomi Watch S4",
                connectionState = WatchConnectionState.UNSUPPORTED,
                statusDetail = "Bluetooth Low Energy is not supported on this hardware.",
                batteryPercent = null,
                bleDiscoveryAvailable = false,
            )

            session.deviceAddressKey != null && session.connectionState != WatchConnectionState.DISCONNECTED -> DeviceStatusSummary(
                targetDeviceName = session.deviceName ?: "Xiaomi Watch S4",
                connectionState = session.connectionState,
                statusDetail = session.lastStatusNote
                    ?: "Read-only GATT session active (${session.services.size} services discovered).",
                batteryPercent = session.standardDeviceInfo.batteryPercent,
                bleDiscoveryAvailable = true,
            )

            detectedWatch != null -> DeviceStatusSummary(
                targetDeviceName = detectedWatch.displayName,
                connectionState = WatchConnectionState.DISCONNECTED,
                statusDetail = "Detected (${detectedWatch.source.label} · ${detectedWatch.matchConfidence.label}). Open Devices tab for read-only GATT inspection.",
                batteryPercent = null,
                bleDiscoveryAvailable = true,
            )

            else -> DeviceStatusSummary(
                targetDeviceName = "Xiaomi Watch S4",
                connectionState = WatchConnectionState.DISCONNECTED,
                statusDetail = "Read-only BLE discovery ready in Devices tab. Official Mi Fitness pairing is preserved.",
                batteryPercent = null,
                bleDiscoveryAvailable = true,
            )
        }

        val bridgeSettings = syncOverview.bridgeSettings
        val watchSyncMessage = if (bridgeSettings.isEnabled) {
            "Native Events app: UNSUPPORTED (WearAuthV2). Fallback Reminder Bridge ACTIVE " +
                "(${syncOverview.upcomingPlannedReminders.size} upcoming alerts, ${bridgeSettings.leadTime.label} lead) via Mi Fitness notifications."
        } else {
            "Native Events app: UNSUPPORTED (WearAuthV2). Tap to enable the Watch Reminder Bridge fallback."
        }

        val liveBleHr = session.heartRateState.latestSample
        val effectiveHrValue = when {
            liveBleHr != null -> "${liveBleHr.bpm} BPM"
            healthSnapshot.heartRateSummary.status == MetricAvailabilityStatus.VERIFIED ->
                "${healthSnapshot.heartRateSummary.primaryValue} BPM"
            else -> "--"
        }
        val effectiveStepsValue = if (healthSnapshot.stepsToday.status == MetricAvailabilityStatus.VERIFIED) {
            healthSnapshot.stepsToday.primaryValue
        } else {
            "--"
        }
        val effectiveSleepValue = if (healthSnapshot.sleepLast24h.status == MetricAvailabilityStatus.VERIFIED) {
            healthSnapshot.sleepLast24h.primaryValue
        } else {
            "--"
        }
        val hasVerifiedHealth = liveBleHr != null ||
            healthSnapshot.stepsToday.status == MetricAvailabilityStatus.VERIFIED ||
            healthSnapshot.heartRateSummary.status == MetricAvailabilityStatus.VERIFIED ||
            healthSnapshot.sleepLast24h.status == MetricAvailabilityStatus.VERIFIED

        val healthBadge = when {
            liveBleHr != null -> "LIVE BLE HR"
            hasVerifiedHealth -> "VERIFIED"
            healthSnapshot.healthConnectAvailability == HealthConnectAvailability.AVAILABLE &&
                !healthSnapshot.hasHealthPermissions -> "GRANT ACCESS"
            healthSnapshot.healthConnectAvailability == HealthConnectAvailability.AVAILABLE -> "NO DATA"
            else -> "UNAVAILABLE"
        }
        val healthDetail = when {
            liveBleHr != null -> "Live verified BLE Heart Rate (0x2A37): ${liveBleHr.bpm} BPM. Tap for full Health & Device telemetry."
            hasVerifiedHealth -> {
                val origins = healthSnapshot.dataOrigins.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Health Connect"
                "Verified via Health Connect ($origins). Watch battery remains UNAVAILABLE unless exposed via 0x2A19."
            }
            healthSnapshot.healthConnectAvailability == HealthConnectAvailability.AVAILABLE &&
                !healthSnapshot.hasHealthPermissions ->
                "Tap to authorize read-only Health Connect (Steps, HR, Sleep) or stream live standard BLE HR (0x2A37)."
            healthSnapshot.healthConnectAvailability == HealthConnectAvailability.AVAILABLE ->
                "Health Connect authorized, no synced records yet. Enable Health Connect sync in Mi Fitness → Profile → Third-party data."
            else ->
                "Health Connect unavailable on this OS. Tap to inspect verified BLE telemetry and live 0x2A37 Heart Rate."
        }

        DashboardOverview(
            deviceStatus = deviceStatus,
            healthOverview = DashboardHealthOverview(
                stepsValue = effectiveStepsValue,
                heartRateValue = effectiveHrValue,
                sleepValue = effectiveSleepValue,
                statusBadge = healthBadge,
                detailNote = healthDetail,
                hasVerifiedData = hasVerifiedHealth,
            ),
            calendarOverview = CalendarOverview(
                localCalendarState = localState,
                localStatusMessage = localMessage,
                nativeWatchSyncState = WatchConnectionState.UNSUPPORTED,
                nativeWatchSyncMessage = watchSyncMessage,
                todayEvents = todayEvents,
                upcomingEvents = upcomingEvents,
            ),
            currentMilestone = "M6 · Health & Device",
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
