package com.nothingwear.app.features.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.core.permissions.CalendarPermissionChecker
import com.nothingwear.app.domain.model.CalendarMutationResult
import com.nothingwear.app.domain.usecase.DeleteCalendarEventUseCase
import com.nothingwear.app.domain.usecase.ObserveEventDetailUseCase
import com.nothingwear.app.domain.usecase.ObserveEventSyncOverviewUseCase
import com.nothingwear.app.domain.usecase.RescheduleWatchRemindersUseCase
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

data class EventDetailModel(
    val instanceKey: String,
    val eventId: Long,
    val title: String,
    val fullDateLabel: String,
    val timeRangeLabel: String,
    val calendarName: String,
    val calendarColor: Int,
    val location: String?,
    val description: String?,
    val timezone: String?,
    val lastSyncedLabel: String,
    val bridgeEnabled: Boolean,
    val bridgeLeadLabel: String,
    val isScheduledForWatchAlert: Boolean,
)

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState
    data object NotFound : EventDetailUiState
    data object Deleted : EventDetailUiState
    data class Loaded(
        val event: EventDetailModel,
        val hasWritePermission: Boolean,
        val isDeleting: Boolean = false,
        val showDeleteConfirm: Boolean = false,
        val feedbackMessage: String? = null,
    ) : EventDetailUiState
}

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeEventDetail: ObserveEventDetailUseCase,
    observeEventSyncOverview: ObserveEventSyncOverviewUseCase,
    private val deleteCalendarEvent: DeleteCalendarEventUseCase,
    private val rescheduleWatchReminders: RescheduleWatchRemindersUseCase,
    private val permissionChecker: CalendarPermissionChecker,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val instanceKey: String = checkNotNull(savedStateHandle["instanceKey"])
    private val controls = MutableStateFlow(
        LocalControls(hasWritePermission = permissionChecker.hasWriteCalendarPermission()),
    )

    val uiState: StateFlow<EventDetailUiState> = combine(
        observeEventDetail(instanceKey),
        observeEventSyncOverview(),
        controls,
    ) { event, syncOverview, local ->
        if (local.deleted) {
            return@combine EventDetailUiState.Deleted
        }
        if (event == null) {
            return@combine EventDetailUiState.NotFound
        }
        val zoneId = timeProvider.zoneId()
        val bridgeSettings = syncOverview.bridgeSettings
        val isPlanned = syncOverview.upcomingPlannedReminders.any {
            it.instanceKey == event.instanceKey
        }
        EventDetailUiState.Loaded(
            event = EventDetailModel(
                instanceKey = event.instanceKey,
                eventId = event.eventId,
                title = event.title,
                fullDateLabel = AgendaTimeWindows.formatFullDate(event, zoneId),
                timeRangeLabel = AgendaTimeWindows.formatTimeRange(event, zoneId),
                calendarName = event.calendarName,
                calendarColor = event.calendarColor,
                location = event.location,
                description = event.description,
                timezone = event.timezone,
                lastSyncedLabel = AgendaTimeWindows.formatSyncTimestamp(
                    event.lastSyncedAtMillis,
                    zoneId,
                ),
                bridgeEnabled = bridgeSettings.isEnabled,
                bridgeLeadLabel = bridgeSettings.leadTime.label,
                isScheduledForWatchAlert = bridgeSettings.isEnabled && isPlanned,
            ),
            hasWritePermission = local.hasWritePermission,
            isDeleting = local.isDeleting,
            showDeleteConfirm = local.showDeleteConfirm,
            feedbackMessage = local.feedbackMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = EventDetailUiState.Loading,
    )

    fun onShowDeleteConfirm(show: Boolean) {
        controls.update { it.copy(showDeleteConfirm = show) }
    }

    fun onWritePermissionResult(granted: Boolean) {
        controls.update {
            it.copy(
                hasWritePermission = granted,
                feedbackMessage = if (granted) null else "WRITE_CALENDAR permission is required to delete events.",
            )
        }
        if (granted) {
            onConfirmDelete()
        }
    }

    fun onConfirmDelete() {
        val loaded = uiState.value as? EventDetailUiState.Loaded ?: return
        if (controls.value.isDeleting) return
        viewModelScope.launch {
            controls.update { it.copy(isDeleting = true, showDeleteConfirm = false, feedbackMessage = null) }
            val result = deleteCalendarEvent(loaded.event.eventId)
            when (result) {
                CalendarMutationResult.Deleted -> {
                    rescheduleWatchReminders()
                    controls.update { it.copy(isDeleting = false, deleted = true) }
                }

                CalendarMutationResult.PermissionDenied -> {
                    controls.update {
                        it.copy(
                            isDeleting = false,
                            hasWritePermission = false,
                            feedbackMessage = "WRITE_CALENDAR permission is required to delete events.",
                        )
                    }
                }

                is CalendarMutationResult.Error -> {
                    controls.update {
                        it.copy(
                            isDeleting = false,
                            feedbackMessage = result.message,
                        )
                    }
                }

                is CalendarMutationResult.Created -> Unit
            }
        }
    }

    private data class LocalControls(
        val hasWritePermission: Boolean = false,
        val isDeleting: Boolean = false,
        val showDeleteConfirm: Boolean = false,
        val deleted: Boolean = false,
        val feedbackMessage: String? = null,
    )
}
