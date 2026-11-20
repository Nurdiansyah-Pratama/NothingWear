package com.nothingwear.app.domain.usecase

import com.nothingwear.app.domain.model.BridgeActionResult
import com.nothingwear.app.domain.model.EventSyncOverview
import com.nothingwear.app.domain.model.ReminderLeadTime
import com.nothingwear.app.domain.repository.EventSyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveEventSyncOverviewUseCase @Inject constructor(
    private val repository: EventSyncRepository,
) {
    operator fun invoke(): Flow<EventSyncOverview> = repository.observeEventSyncOverview()
}

class SetReminderBridgeEnabledUseCase @Inject constructor(
    private val repository: EventSyncRepository,
) {
    suspend operator fun invoke(enabled: Boolean): BridgeActionResult =
        repository.setReminderBridgeEnabled(enabled)
}

class SetReminderLeadTimeUseCase @Inject constructor(
    private val repository: EventSyncRepository,
) {
    suspend operator fun invoke(leadTime: ReminderLeadTime): BridgeActionResult =
        repository.setReminderLeadTime(leadTime)
}

class RescheduleWatchRemindersUseCase @Inject constructor(
    private val repository: EventSyncRepository,
) {
    suspend operator fun invoke(): BridgeActionResult =
        repository.rescheduleUpcomingReminders()
}

class SendTestWatchAlertUseCase @Inject constructor(
    private val repository: EventSyncRepository,
) {
    suspend operator fun invoke(): BridgeActionResult =
        repository.sendTestWatchAlert()
}
