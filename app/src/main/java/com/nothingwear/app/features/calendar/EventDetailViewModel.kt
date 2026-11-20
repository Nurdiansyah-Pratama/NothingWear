package com.nothingwear.app.features.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.domain.usecase.ObserveEventDetailUseCase
import com.nothingwear.app.domain.util.AgendaTimeWindows
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class EventDetailModel(
    val instanceKey: String,
    val title: String,
    val fullDateLabel: String,
    val timeRangeLabel: String,
    val calendarName: String,
    val calendarColor: Int,
    val location: String?,
    val description: String?,
    val timezone: String?,
    val lastSyncedLabel: String,
)

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState
    data object NotFound : EventDetailUiState
    data class Loaded(val event: EventDetailModel) : EventDetailUiState
}

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeEventDetail: ObserveEventDetailUseCase,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val instanceKey: String = checkNotNull(savedStateHandle["instanceKey"])

    val uiState: StateFlow<EventDetailUiState> = observeEventDetail(instanceKey)
        .map { event ->
            if (event == null) {
                EventDetailUiState.NotFound
            } else {
                val zoneId = timeProvider.zoneId()
                EventDetailUiState.Loaded(
                    EventDetailModel(
                        instanceKey = event.instanceKey,
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
                    ),
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = EventDetailUiState.Loading,
        )
}
