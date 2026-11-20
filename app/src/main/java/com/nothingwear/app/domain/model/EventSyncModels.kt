package com.nothingwear.app.domain.model

import com.nothingwear.protocol.watchs4.NativeSyncCapabilityReport

enum class ReminderLeadTime(
    val minutes: Int,
    val label: String,
) {
    AT_START(0, "At start"),
    FIVE_MIN(5, "5 min"),
    FIFTEEN_MIN(15, "15 min"),
    THIRTY_MIN(30, "30 min"),
    ;

    companion object {
        fun fromMinutes(minutes: Int?): ReminderLeadTime =
            entries.firstOrNull { it.minutes == minutes } ?: FIFTEEN_MIN
    }
}

data class ReminderBridgeSettings(
    val isEnabled: Boolean = false,
    val leadTime: ReminderLeadTime = ReminderLeadTime.FIFTEEN_MIN,
    val lastScheduledCount: Int = 0,
    val lastScheduledAtMillis: Long? = null,
)

data class PlannedEventReminder(
    val instanceKey: String,
    val title: String,
    val timeRangeLabel: String,
    val triggerAtMillis: Long,
    val triggerLabel: String,
    val calendarName: String,
    val location: String?,
)

data class EventSyncOverview(
    val nativeCapability: NativeSyncCapabilityReport,
    val bridgeSettings: ReminderBridgeSettings,
    val upcomingPlannedReminders: List<PlannedEventReminder>,
    val validatedPayloadCount: Int,
)

sealed interface BridgeActionResult {
    data class Scheduled(val count: Int) : BridgeActionResult
    data object Cleared : BridgeActionResult
    data object TestNotificationSent : BridgeActionResult
    data object NotificationPermissionRequired : BridgeActionResult
    data class Failed(val message: String) : BridgeActionResult
}
