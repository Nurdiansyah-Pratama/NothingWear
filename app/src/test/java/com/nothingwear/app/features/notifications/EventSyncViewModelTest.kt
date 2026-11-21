package com.nothingwear.app.features.notifications

import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.core.permissions.NotificationPermissionChecker
import com.nothingwear.app.domain.model.BridgeActionResult
import com.nothingwear.app.domain.model.EventSyncOverview
import com.nothingwear.app.domain.model.ReminderBridgeSettings
import com.nothingwear.app.domain.model.ReminderLeadTime
import com.nothingwear.app.domain.repository.EventSyncRepository
import com.nothingwear.app.domain.usecase.ObserveEventSyncOverviewUseCase
import com.nothingwear.app.domain.usecase.RescheduleWatchRemindersUseCase
import com.nothingwear.app.domain.usecase.ScheduleDelayedTestWatchAlertUseCase
import com.nothingwear.app.domain.usecase.SendTestWatchAlertUseCase
import com.nothingwear.app.domain.usecase.SetReminderBridgeEnabledUseCase
import com.nothingwear.app.domain.usecase.SetReminderLeadTimeUseCase
import com.nothingwear.protocol.watchs4.WatchS4AdapterState
import com.nothingwear.protocol.watchs4.XiaomiWatchS4ProtocolAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class EventSyncViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedTime = object : TimeProvider {
        override fun nowMillis(): Long = 1_763_690_000_000L
        override fun zoneId(): ZoneId = ZoneId.of("Asia/Jakarta")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun reportsHonestNativeUnsupportedStateAndControlsReminderBridge() = runTest {
        val fakeRepo = FakeEventSyncRepository()
        val permChecker = object : NotificationPermissionChecker {
            override fun requiresRuntimePermission(): Boolean = true
            override fun hasPostNotificationsPermission(): Boolean = true
        }
        val viewModel = EventSyncViewModel(
            observeEventSyncOverview = ObserveEventSyncOverviewUseCase(fakeRepo),
            setReminderBridgeEnabled = SetReminderBridgeEnabledUseCase(fakeRepo),
            setReminderLeadTime = SetReminderLeadTimeUseCase(fakeRepo),
            rescheduleWatchReminders = RescheduleWatchRemindersUseCase(fakeRepo),
            sendTestWatchAlert = SendTestWatchAlertUseCase(fakeRepo),
            scheduleDelayedTestWatchAlert = ScheduleDelayedTestWatchAlertUseCase(fakeRepo),
            notificationPermissionChecker = permChecker,
            timeProvider = fixedTime,
        )

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(WatchS4AdapterState.UNSUPPORTED, viewModel.uiState.value.nativeCapability.state)

        viewModel.onToggleBridgeEnabled(true)
        viewModel.onSelectLeadTime(ReminderLeadTime.FIVE_MIN)
        viewModel.onSendTestWatchAlert()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isBridgeEnabled)
        assertEquals(ReminderLeadTime.FIVE_MIN, viewModel.uiState.value.selectedLeadTime)
        assertTrue(viewModel.uiState.value.feedbackMessage?.contains("Test alert posted") == true)

        job.cancel()
    }

    private class FakeEventSyncRepository : EventSyncRepository {
        private val overview = MutableStateFlow(
            EventSyncOverview(
                nativeCapability = XiaomiWatchS4ProtocolAdapter().getCapabilityReport(),
                bridgeSettings = ReminderBridgeSettings(),
                upcomingPlannedReminders = emptyList(),
                validatedPayloadCount = 0,
            ),
        )

        override fun observeEventSyncOverview(): Flow<EventSyncOverview> = overview

        override suspend fun setReminderBridgeEnabled(enabled: Boolean): BridgeActionResult {
            overview.value = overview.value.copy(
                bridgeSettings = overview.value.bridgeSettings.copy(isEnabled = enabled),
            )
            return if (enabled) BridgeActionResult.Scheduled(2) else BridgeActionResult.Cleared
        }

        override suspend fun setReminderLeadTime(leadTime: ReminderLeadTime): BridgeActionResult {
            overview.value = overview.value.copy(
                bridgeSettings = overview.value.bridgeSettings.copy(leadTime = leadTime),
            )
            return BridgeActionResult.Scheduled(2)
        }

        override suspend fun rescheduleUpcomingReminders(): BridgeActionResult =
            BridgeActionResult.Scheduled(2)

        override suspend fun sendTestWatchAlert(): BridgeActionResult =
            BridgeActionResult.TestNotificationSent

        override suspend fun scheduleDelayedTestWatchAlert(delaySeconds: Int): BridgeActionResult =
            BridgeActionResult.DelayedTestScheduled(delaySeconds)
    }
}
