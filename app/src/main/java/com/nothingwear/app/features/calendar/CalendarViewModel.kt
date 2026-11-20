package com.nothingwear.app.features.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.core.permissions.CalendarPermissionChecker
import com.nothingwear.app.domain.model.AgendaRange
import com.nothingwear.app.domain.model.CalendarEvent
import com.nothingwear.app.domain.model.CalendarSyncResult
import com.nothingwear.app.domain.repository.CalendarRepository
import com.nothingwear.app.domain.usecase.ObserveCalendarSyncMetadataUseCase
import com.nothingwear.app.domain.usecase.ObserveCalendarsUseCase
import com.nothingwear.app.domain.usecase.RescheduleWatchRemindersUseCase
import com.nothingwear.app.domain.usecase.SyncDeviceCalendarsUseCase
import com.nothingwear.app.domain.usecase.ToggleCalendarSelectionUseCase
import com.nothingwear.app.domain.util.AgendaTimeWindows
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    observeCalendars: ObserveCalendarsUseCase,
    observeCalendarSyncMetadata: ObserveCalendarSyncMetadataUseCase,
    private val syncDeviceCalendars: SyncDeviceCalendarsUseCase,
    private val toggleCalendarSelection: ToggleCalendarSelectionUseCase,
    private val rescheduleWatchReminders: RescheduleWatchRemindersUseCase,
    private val permissionChecker: CalendarPermissionChecker,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val localControls = MutableStateFlow(
        LocalCalendarControls(
            hasPermission = permissionChecker.hasReadCalendarPermission(),
        ),
    )

    private var hasAutoSynced = false

    val uiState: StateFlow<CalendarUiState> = combine(
        localControls,
        observeCalendars(),
        calendarRepository.observeSelectedEvents(),
        observeCalendarSyncMetadata(),
    ) { controls, calendars, selectedEvents, metadata ->
        val nowMillis = timeProvider.nowMillis()
        val zoneId = timeProvider.zoneId()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val window = AgendaTimeWindows.windowFor(controls.selectedRange, nowMillis, zoneId)

        val filteredEvents = selectedEvents
            .asSequence()
            .filter { AgendaTimeWindows.overlaps(it, window, zoneId) }
            .filter { AgendaTimeWindows.matchesQuery(it, controls.searchQuery) }
            .sortedWith(
                compareBy<CalendarEvent> { AgendaTimeWindows.normalizedInterval(it, zoneId).first }
                    .thenByDescending { it.isAllDay }
                    .thenBy { it.title.lowercase() },
            )
            .toList()

        val grouped = filteredEvents
            .groupBy { AgendaTimeWindows.startLocalDate(it, zoneId) }
            .toSortedMap()
            .map { (date, eventsOnDay) ->
                AgendaDayGroup(
                    dateHeader = AgendaTimeWindows.formatDateBadge(date, today),
                    events = eventsOnDay.map { event ->
                        CalendarEventUiModel(
                            instanceKey = event.instanceKey,
                            title = event.title,
                            timeLabel = AgendaTimeWindows.formatTimeRange(event, zoneId),
                            calendarName = event.calendarName,
                            calendarColor = event.calendarColor,
                            location = event.location,
                            isAllDay = event.isAllDay,
                        )
                    },
                )
            }

        CalendarUiState(
            hasPermission = controls.hasPermission,
            permissionDeniedOnce = controls.permissionDeniedOnce,
            selectedRange = controls.selectedRange,
            searchQuery = controls.searchQuery,
            isSyncing = controls.isSyncing,
            syncFeedbackMessage = controls.syncFeedbackMessage,
            lastSyncedLabel = metadata.lastSyncedAtMillis?.let {
                AgendaTimeWindows.formatSyncTimestamp(it, zoneId)
            },
            syncMetadata = metadata,
            calendars = calendars,
            groupedEvents = grouped,
            totalFilteredEvents = filteredEvents.size,
            showCalendarFilterSheet = controls.showCalendarFilterSheet,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = CalendarUiState(
            hasPermission = permissionChecker.hasReadCalendarPermission(),
        ),
    )

    init {
        if (permissionChecker.hasReadCalendarPermission()) {
            hasAutoSynced = true
            onSyncNow()
        }
    }

    fun onRefreshPermissionState() {
        val granted = permissionChecker.hasReadCalendarPermission()
        localControls.update { it.copy(hasPermission = granted) }
        if (granted && !hasAutoSynced) {
            hasAutoSynced = true
            onSyncNow()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        localControls.update {
            it.copy(
                hasPermission = granted,
                permissionDeniedOnce = if (granted) false else true,
                syncFeedbackMessage = if (granted) {
                    null
                } else {
                    "READ_CALENDAR denied. Showing offline cache only."
                },
            )
        }
        if (granted) {
            hasAutoSynced = true
            onSyncNow()
        }
    }

    fun onSelectRange(range: AgendaRange) {
        localControls.update { it.copy(selectedRange = range) }
    }

    fun onSearchQueryChanged(query: String) {
        localControls.update { it.copy(searchQuery = query) }
    }

    fun onShowCalendarFilterSheet(show: Boolean) {
        localControls.update { it.copy(showCalendarFilterSheet = show) }
    }

    fun onToggleCalendar(calendarId: Long, selected: Boolean) {
        viewModelScope.launch {
            toggleCalendarSelection(calendarId = calendarId, selected = selected)
            rescheduleWatchReminders()
        }
    }

    fun onSyncNow() {
        if (localControls.value.isSyncing) return
        viewModelScope.launch {
            localControls.update { it.copy(isSyncing = true, syncFeedbackMessage = null) }
            val result = syncDeviceCalendars()
            if (result is CalendarSyncResult.Success) {
                rescheduleWatchReminders()
            }
            localControls.update { state ->
                when (result) {
                    is CalendarSyncResult.Success -> state.copy(
                        hasPermission = true,
                        isSyncing = false,
                        syncFeedbackMessage = "Cached ${result.syncedEvents} events from ${result.syncedCalendars} calendars locally.",
                    )

                    CalendarSyncResult.PermissionDenied -> state.copy(
                        hasPermission = false,
                        isSyncing = false,
                        syncFeedbackMessage = "READ_CALENDAR permission required to refresh device calendars.",
                    )

                    is CalendarSyncResult.Error -> state.copy(
                        isSyncing = false,
                        syncFeedbackMessage = "Sync error: ${result.message}",
                    )
                }
            }
        }
    }

    private data class LocalCalendarControls(
        val hasPermission: Boolean = false,
        val permissionDeniedOnce: Boolean = false,
        val selectedRange: AgendaRange = AgendaRange.WEEK,
        val searchQuery: String = "",
        val isSyncing: Boolean = false,
        val syncFeedbackMessage: String? = null,
        val showCalendarFilterSheet: Boolean = false,
    )
}
