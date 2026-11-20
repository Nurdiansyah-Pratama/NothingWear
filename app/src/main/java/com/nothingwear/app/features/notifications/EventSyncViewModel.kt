package com.nothingwear.app.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.core.permissions.NotificationPermissionChecker
import com.nothingwear.app.domain.model.BridgeActionResult
import com.nothingwear.app.domain.model.ReminderLeadTime
import com.nothingwear.app.domain.usecase.ObserveEventSyncOverviewUseCase
import com.nothingwear.app.domain.usecase.RescheduleWatchRemindersUseCase
import com.nothingwear.app.domain.usecase.SendTestWatchAlertUseCase
import com.nothingwear.app.domain.usecase.SetReminderBridgeEnabledUseCase
import com.nothingwear.app.domain.usecase.SetReminderLeadTimeUseCase
import com.nothingwear.app.domain.util.AgendaTimeWindows
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventSyncViewModel @Inject constructor(
    observeEventSyncOverview: ObserveEventSyncOverviewUseCase,
    private val setReminderBridgeEnabled: SetReminderBridgeEnabledUseCase,
    private val setReminderLeadTime: SetReminderLeadTimeUseCase,
    private val rescheduleWatchReminders: RescheduleWatchRemindersUseCase,
    private val sendTestWatchAlert: SendTestWatchAlertUseCase,
    private val notificationPermissionChecker: NotificationPermissionChecker,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val localControls = MutableStateFlow(
        LocalSyncControls(
            hasNotificationPermission = notificationPermissionChecker.hasPostNotificationsPermission(),
        ),
    )

    val uiState: StateFlow<EventSyncUiState> = combine(
        localControls,
        observeEventSyncOverview(),
    ) { controls, overview ->
        val settings = overview.bridgeSettings
        EventSyncUiState(
            requiresNotificationPermission = notificationPermissionChecker.requiresRuntimePermission(),
            hasNotificationPermission = controls.hasNotificationPermission,
            permissionDeniedOnce = controls.permissionDeniedOnce,
            isBridgeEnabled = settings.isEnabled,
            selectedLeadTime = settings.leadTime,
            lastScheduledCount = settings.lastScheduledCount,
            lastScheduledLabel = settings.lastScheduledAtMillis?.let {
                AgendaTimeWindows.formatSyncTimestamp(it, timeProvider.zoneId())
            },
            validatedPayloadCount = overview.validatedPayloadCount,
            upcomingReminders = overview.upcomingPlannedReminders,
            nativeCapability = overview.nativeCapability,
            feedbackMessage = controls.feedbackMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = EventSyncUiState(
            requiresNotificationPermission = notificationPermissionChecker.requiresRuntimePermission(),
            hasNotificationPermission = notificationPermissionChecker.hasPostNotificationsPermission(),
        ),
    )

    fun onRefreshPermissionState() {
        localControls.update {
            it.copy(hasNotificationPermission = notificationPermissionChecker.hasPostNotificationsPermission())
        }
    }

    fun onNotificationPermissionResult(granted: Boolean, enableAfterGrant: Boolean = false) {
        val effectiveGranted = granted && notificationPermissionChecker.hasPostNotificationsPermission()
        localControls.update {
            it.copy(
                hasNotificationPermission = effectiveGranted,
                permissionDeniedOnce = !effectiveGranted,
                feedbackMessage = if (!effectiveGranted) {
                    "Notification permission denied. Reminder Bridge cannot post watch alerts."
                } else {
                    null
                },
            )
        }
        if (effectiveGranted && enableAfterGrant) {
            onToggleBridgeEnabled(true)
        }
    }

    fun onToggleBridgeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            handleActionResult(setReminderBridgeEnabled(enabled))
        }
    }

    fun onSelectLeadTime(leadTime: ReminderLeadTime) {
        viewModelScope.launch {
            handleActionResult(setReminderLeadTime(leadTime))
        }
    }

    fun onRescheduleNow() {
        viewModelScope.launch {
            handleActionResult(rescheduleWatchReminders())
        }
    }

    fun onSendTestWatchAlert() {
        viewModelScope.launch {
            handleActionResult(sendTestWatchAlert())
        }
    }

    private fun handleActionResult(result: BridgeActionResult) {
        val message = when (result) {
            is BridgeActionResult.Scheduled ->
                "Scheduled ${result.count} upcoming calendar reminders (next 48h)."

            BridgeActionResult.Cleared ->
                "Watch Reminder Bridge disabled. All scheduled reminders cleared."

            BridgeActionResult.TestNotificationSent ->
                "Test alert posted! Check your Xiaomi Watch S4 (ensure NothingWear is enabled in Mi Fitness App Notifications)."

            BridgeActionResult.NotificationPermissionRequired ->
                "Grant POST_NOTIFICATIONS permission first to enable watch alerts."

            is BridgeActionResult.Failed ->
                "Reminder scheduling failed: ${result.message}"
        }
        localControls.update {
            it.copy(
                hasNotificationPermission = notificationPermissionChecker.hasPostNotificationsPermission(),
                feedbackMessage = message,
            )
        }
    }

    private data class LocalSyncControls(
        val hasNotificationPermission: Boolean = false,
        val permissionDeniedOnce: Boolean = false,
        val feedbackMessage: String? = null,
    )
}
