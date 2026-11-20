package com.nothingwear.app.domain.repository

import com.nothingwear.app.domain.model.BridgeActionResult
import com.nothingwear.app.domain.model.EventSyncOverview
import com.nothingwear.app.domain.model.ReminderLeadTime
import kotlinx.coroutines.flow.Flow

interface EventSyncRepository {
    fun observeEventSyncOverview(): Flow<EventSyncOverview>

    suspend fun setReminderBridgeEnabled(enabled: Boolean): BridgeActionResult

    suspend fun setReminderLeadTime(leadTime: ReminderLeadTime): BridgeActionResult

    suspend fun rescheduleUpcomingReminders(): BridgeActionResult

    suspend fun sendTestWatchAlert(): BridgeActionResult
}
