package com.nothingwear.app.features.notifications

import com.nothingwear.app.domain.model.PlannedEventReminder
import com.nothingwear.app.domain.model.ReminderLeadTime
import com.nothingwear.protocol.watchs4.NativeSyncCapabilityReport
import com.nothingwear.protocol.watchs4.XiaomiWatchS4ProtocolAdapter

data class EventSyncUiState(
    val requiresNotificationPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val permissionDeniedOnce: Boolean = false,
    val isBridgeEnabled: Boolean = false,
    val selectedLeadTime: ReminderLeadTime = ReminderLeadTime.FIFTEEN_MIN,
    val lastScheduledCount: Int = 0,
    val lastScheduledLabel: String? = null,
    val validatedPayloadCount: Int = 0,
    val upcomingReminders: List<PlannedEventReminder> = emptyList(),
    val nativeCapability: NativeSyncCapabilityReport = XiaomiWatchS4ProtocolAdapter().getCapabilityReport(),
    val feedbackMessage: String? = null,
)
