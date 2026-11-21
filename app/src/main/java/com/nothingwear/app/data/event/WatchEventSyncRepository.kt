package com.nothingwear.app.data.event

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.core.permissions.NotificationPermissionChecker
import com.nothingwear.app.domain.model.BridgeActionResult
import com.nothingwear.app.domain.model.CalendarEvent
import com.nothingwear.app.domain.model.EventSyncOverview
import com.nothingwear.app.domain.model.ReminderBridgeSettings
import com.nothingwear.app.domain.model.ReminderLeadTime
import com.nothingwear.app.domain.repository.CalendarRepository
import com.nothingwear.app.domain.repository.EventSyncRepository
import com.nothingwear.protocol.watchs4.WatchS4EventPayload
import com.nothingwear.protocol.watchs4.WatchS4ProtocolAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchEventSyncRepository @Inject constructor(
    private val protocolAdapter: WatchS4ProtocolAdapter,
    private val calendarRepository: CalendarRepository,
    private val dataStore: DataStore<Preferences>,
    private val workManager: WorkManager,
    private val notificationPoster: CalendarNotificationPoster,
    private val notificationPermissionChecker: NotificationPermissionChecker,
    private val timeProvider: TimeProvider,
) : EventSyncRepository {

    private val settingsFlow: Flow<ReminderBridgeSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            ReminderBridgeSettings(
                isEnabled = prefs[KEY_BRIDGE_ENABLED] ?: false,
                leadTime = ReminderLeadTime.fromMinutes(prefs[KEY_LEAD_TIME_MINUTES]),
                lastScheduledCount = prefs[KEY_LAST_SCHEDULED_COUNT] ?: 0,
                lastScheduledAtMillis = prefs[KEY_LAST_SCHEDULED_AT],
            )
        }

    override fun observeEventSyncOverview(): Flow<EventSyncOverview> = combine(
        settingsFlow,
        calendarRepository.observeSelectedEvents(),
    ) { settings, events ->
        val now = timeProvider.nowMillis()
        val zoneId = timeProvider.zoneId()
        val planned = ReminderSchedulePlanner.planReminders(
            events = events,
            leadTime = settings.leadTime,
            nowMillis = now,
            zoneId = zoneId,
        )
        val validatedPayloads = protocolAdapter.prepareValidatedPayloads(
            events.map { it.toProtocolPayload(settings.leadTime.minutes) },
        )

        EventSyncOverview(
            nativeCapability = protocolAdapter.getCapabilityReport(),
            bridgeSettings = settings,
            upcomingPlannedReminders = planned,
            validatedPayloadCount = validatedPayloads.size,
        )
    }

    override suspend fun setReminderBridgeEnabled(enabled: Boolean): BridgeActionResult {
        if (enabled && !notificationPermissionChecker.hasPostNotificationsPermission()) {
            return BridgeActionResult.NotificationPermissionRequired
        }

        dataStore.edit { prefs ->
            prefs[KEY_BRIDGE_ENABLED] = enabled
        }

        return if (enabled) {
            notificationPoster.ensureChannel()
            rescheduleUpcomingReminders()
        } else {
            cancelAllScheduledReminders()
            dataStore.edit { prefs ->
                prefs[KEY_LAST_SCHEDULED_COUNT] = 0
            }
            BridgeActionResult.Cleared
        }
    }

    override suspend fun setReminderLeadTime(leadTime: ReminderLeadTime): BridgeActionResult {
        dataStore.edit { prefs ->
            prefs[KEY_LEAD_TIME_MINUTES] = leadTime.minutes
        }
        val settings = settingsFlow.first()
        return if (settings.isEnabled) {
            rescheduleUpcomingReminders()
        } else {
            BridgeActionResult.Scheduled(0)
        }
    }

    override suspend fun rescheduleUpcomingReminders(): BridgeActionResult {
        val settings = settingsFlow.first()
        if (!settings.isEnabled) {
            cancelAllScheduledReminders()
            return BridgeActionResult.Cleared
        }

        if (!notificationPermissionChecker.hasPostNotificationsPermission()) {
            return BridgeActionResult.NotificationPermissionRequired
        }

        return try {
            notificationPoster.ensureChannel()
            cancelAllScheduledReminders()

            val now = timeProvider.nowMillis()
            val zoneId = timeProvider.zoneId()
            val events = calendarRepository.observeSelectedEvents().first()
            val planned = ReminderSchedulePlanner.planReminders(
                events = events,
                leadTime = settings.leadTime,
                nowMillis = now,
                zoneId = zoneId,
            )

            planned.forEach { reminder ->
                val delayMs = (reminder.triggerAtMillis - now).coerceAtLeast(1_000L)
                val request = OneTimeWorkRequestBuilder<CalendarReminderWorker>()
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .addTag(CalendarReminderWorker.WORK_TAG)
                    .setInputData(
                        workDataOf(
                            CalendarReminderWorker.KEY_TITLE to reminder.title,
                            CalendarReminderWorker.KEY_TIME_RANGE to reminder.timeRangeLabel,
                            CalendarReminderWorker.KEY_CALENDAR_NAME to reminder.calendarName,
                            CalendarReminderWorker.KEY_LOCATION to reminder.location,
                            CalendarReminderWorker.KEY_NOTIFICATION_ID to reminder.instanceKey.hashCode(),
                        ),
                    )
                    .build()

                workManager.enqueueUniqueWork(
                    "nw_reminder_${reminder.instanceKey}",
                    ExistingWorkPolicy.REPLACE,
                    request,
                )
            }

            dataStore.edit { prefs ->
                prefs[KEY_LAST_SCHEDULED_COUNT] = planned.size
                prefs[KEY_LAST_SCHEDULED_AT] = now
            }

            BridgeActionResult.Scheduled(planned.size)
        } catch (e: Exception) {
            BridgeActionResult.Failed(e.message ?: "Failed to schedule reminders.")
        }
    }

    override suspend fun sendTestWatchAlert(): BridgeActionResult {
        if (!notificationPermissionChecker.hasPostNotificationsPermission()) {
            return BridgeActionResult.NotificationPermissionRequired
        }
        val sent = notificationPoster.postTestWatchAlert()
        return if (sent) {
            BridgeActionResult.TestNotificationSent
        } else {
            BridgeActionResult.NotificationPermissionRequired
        }
    }

    override suspend fun scheduleDelayedTestWatchAlert(delaySeconds: Int): BridgeActionResult {
        if (!notificationPermissionChecker.hasPostNotificationsPermission()) {
            return BridgeActionResult.NotificationPermissionRequired
        }
        notificationPoster.ensureChannel()
        val uniqueId = (timeProvider.nowMillis() % Int.MAX_VALUE).toInt()
        val request = OneTimeWorkRequestBuilder<CalendarReminderWorker>()
            .setInitialDelay(delaySeconds.toLong(), TimeUnit.SECONDS)
            .addTag(CalendarReminderWorker.WORK_TAG)
            .setInputData(
                workDataOf(
                    CalendarReminderWorker.KEY_TITLE to "Watch S4 Lockscreen Test",
                    CalendarReminderWorker.KEY_TIME_RANGE to "Delayed ${delaySeconds}s Bridge Test",
                    CalendarReminderWorker.KEY_CALENDAR_NAME to "NothingWear",
                    CalendarReminderWorker.KEY_LOCATION to "Mi Fitness NotifySyncService",
                    CalendarReminderWorker.KEY_NOTIFICATION_ID to uniqueId,
                ),
            )
            .build()
        workManager.enqueueUniqueWork(
            "nw_delayed_test_alert",
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return BridgeActionResult.DelayedTestScheduled(delaySeconds)
    }

    private fun cancelAllScheduledReminders() {
        workManager.cancelAllWorkByTag(CalendarReminderWorker.WORK_TAG)
    }

    private fun CalendarEvent.toProtocolPayload(leadMinutes: Int): WatchS4EventPayload =
        WatchS4EventPayload.sanitized(
            title = title,
            location = location,
            description = description,
            startEpochSeconds = startTimeMillis / 1000L,
            endEpochSeconds = endTimeMillis / 1000L,
            isAllDay = isAllDay,
            reminderMinutes = leadMinutes,
            calendarName = calendarName,
        )

    private companion object {
        val KEY_BRIDGE_ENABLED = booleanPreferencesKey("watch_reminder_bridge_enabled")
        val KEY_LEAD_TIME_MINUTES = intPreferencesKey("watch_reminder_lead_time_minutes")
        val KEY_LAST_SCHEDULED_COUNT = intPreferencesKey("watch_reminder_last_scheduled_count")
        val KEY_LAST_SCHEDULED_AT = longPreferencesKey("watch_reminder_last_scheduled_at")
    }
}
