package com.nothingwear.app.features.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.core.permissions.CalendarPermissionChecker
import com.nothingwear.app.domain.model.AgendaRange
import com.nothingwear.app.domain.model.CalendarEvent
import com.nothingwear.app.domain.model.CalendarMutationResult
import com.nothingwear.app.domain.model.CalendarSyncResult
import com.nothingwear.app.domain.model.CreateCalendarEventInput
import com.nothingwear.app.domain.repository.CalendarRepository
import com.nothingwear.app.domain.usecase.CreateCalendarEventUseCase
import com.nothingwear.app.domain.usecase.DeleteCalendarEventUseCase
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    observeCalendars: ObserveCalendarsUseCase,
    observeCalendarSyncMetadata: ObserveCalendarSyncMetadataUseCase,
    private val syncDeviceCalendars: SyncDeviceCalendarsUseCase,
    private val toggleCalendarSelection: ToggleCalendarSelectionUseCase,
    private val createCalendarEvent: CreateCalendarEventUseCase,
    private val deleteCalendarEvent: DeleteCalendarEventUseCase,
    private val rescheduleWatchReminders: RescheduleWatchRemindersUseCase,
    private val permissionChecker: CalendarPermissionChecker,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val initialToday: LocalDate = Instant.ofEpochMilli(timeProvider.nowMillis())
        .atZone(timeProvider.zoneId())
        .toLocalDate()

    private val localControls = MutableStateFlow(
        LocalCalendarControls(
            hasPermission = permissionChecker.hasReadCalendarPermission(),
            hasWritePermission = permissionChecker.hasWriteCalendarPermission(),
            displayedMonth = YearMonth.from(initialToday),
            selectedDate = initialToday,
            createDraft = CreateEventDraft(date = initialToday),
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

        val queryMatchedEvents = selectedEvents
            .asSequence()
            .filter { AgendaTimeWindows.matchesQuery(it, controls.searchQuery) }
            .sortedWith(
                compareBy<CalendarEvent> { AgendaTimeWindows.normalizedInterval(it, zoneId).first }
                    .thenByDescending { it.isAllDay }
                    .thenBy { it.title.lowercase() },
            )
            .toList()

        val filteredListEvents = queryMatchedEvents
            .filter { AgendaTimeWindows.overlaps(it, window, zoneId) }

        val grouped = filteredListEvents
            .groupBy { AgendaTimeWindows.startLocalDate(it, zoneId) }
            .toSortedMap()
            .map { (date, eventsOnDay) ->
                AgendaDayGroup(
                    dateHeader = AgendaTimeWindows.formatDateBadge(date, today),
                    events = eventsOnDay.map { it.toUiModel(zoneId) },
                )
            }

        // Build 6x7 (42-cell) Google Calendar-style Month Grid (Sunday -> Saturday)
        val month = controls.displayedMonth
        val firstOfMonth = month.atDay(1)
        val leadingDays = firstOfMonth.dayOfWeek.value % 7 // Sunday=0, Monday=1, ..., Saturday=6
        val gridStart = firstOfMonth.minusDays(leadingDays.toLong())

        val monthCells = (0 until 42).map { offset ->
            val cellDate = gridStart.plusDays(offset.toLong())
            val cellWindow = AgendaTimeWindows.dayWindow(cellDate, zoneId)
            val dayEvents = queryMatchedEvents
                .filter { AgendaTimeWindows.overlaps(it, cellWindow, zoneId) }
                .map { it.toUiModel(zoneId) }
            CalendarMonthCell(
                date = cellDate,
                isCurrentMonth = YearMonth.from(cellDate) == month,
                isToday = cellDate == today,
                isSelected = cellDate == controls.selectedDate,
                events = dayEvents,
            )
        }

        val selectedDayWindow = AgendaTimeWindows.dayWindow(controls.selectedDate, zoneId)
        val selectedDayEvents = queryMatchedEvents
            .filter { AgendaTimeWindows.overlaps(it, selectedDayWindow, zoneId) }
            .map { it.toUiModel(zoneId) }

        val defaultCalendarId = controls.createDraft.calendarId
            ?: calendars.firstOrNull { it.isSelected && it.isPrimary }?.id
            ?: calendars.firstOrNull { it.isSelected }?.id
            ?: calendars.firstOrNull()?.id

        CalendarUiState(
            hasPermission = controls.hasPermission,
            hasWritePermission = controls.hasWritePermission,
            permissionDeniedOnce = controls.permissionDeniedOnce,
            viewMode = controls.viewMode,
            selectedRange = controls.selectedRange,
            displayedMonth = month,
            displayedMonthLabel = month.format(MONTH_HEADER_FORMATTER),
            selectedDate = controls.selectedDate,
            selectedDateHeader = AgendaTimeWindows.formatDateBadge(controls.selectedDate, today),
            monthCells = monthCells,
            selectedDateEvents = selectedDayEvents,
            searchQuery = controls.searchQuery,
            isSyncing = controls.isSyncing,
            isMutatingEvent = controls.isMutatingEvent,
            syncFeedbackMessage = controls.syncFeedbackMessage,
            lastSyncedLabel = metadata.lastSyncedAtMillis?.let {
                AgendaTimeWindows.formatSyncTimestamp(it, zoneId)
            },
            syncMetadata = metadata,
            calendars = calendars,
            groupedEvents = grouped,
            totalFilteredEvents = if (controls.viewMode == CalendarViewMode.MONTH_GRID) {
                selectedDayEvents.size
            } else {
                filteredListEvents.size
            },
            showCalendarFilterSheet = controls.showCalendarFilterSheet,
            showCreateEventSheet = controls.showCreateEventSheet,
            createDraft = controls.createDraft.copy(calendarId = defaultCalendarId),
            pendingDeleteEvent = controls.pendingDeleteEvent,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = CalendarUiState(
            hasPermission = permissionChecker.hasReadCalendarPermission(),
            hasWritePermission = permissionChecker.hasWriteCalendarPermission(),
            displayedMonth = YearMonth.from(initialToday),
            displayedMonthLabel = YearMonth.from(initialToday).format(MONTH_HEADER_FORMATTER),
            selectedDate = initialToday,
            createDraft = CreateEventDraft(date = initialToday),
        ),
    )

    init {
        if (permissionChecker.hasReadCalendarPermission()) {
            hasAutoSynced = true
            onSyncNow()
        }
    }

    fun onRefreshPermissionState() {
        val readGranted = permissionChecker.hasReadCalendarPermission()
        val writeGranted = permissionChecker.hasWriteCalendarPermission()
        localControls.update {
            it.copy(
                hasPermission = readGranted,
                hasWritePermission = writeGranted,
            )
        }
        if (readGranted && !hasAutoSynced) {
            hasAutoSynced = true
            onSyncNow()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        localControls.update {
            it.copy(
                hasPermission = granted,
                hasWritePermission = permissionChecker.hasWriteCalendarPermission(),
                permissionDeniedOnce = !granted,
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

    fun onWritePermissionResult(granted: Boolean) {
        val hadPendingSave = localControls.value.pendingCreateAfterWritePermission
        val pendingDelete = localControls.value.pendingDeleteAfterWritePermission
        localControls.update {
            it.copy(
                hasWritePermission = granted,
                pendingCreateAfterWritePermission = false,
                pendingDeleteAfterWritePermission = null,
                syncFeedbackMessage = if (granted) {
                    null
                } else {
                    "WRITE_CALENDAR denied. Cannot add or delete calendar events."
                },
            )
        }
        if (granted) {
            if (hadPendingSave) {
                onSaveCreatedEvent()
            } else if (pendingDelete != null) {
                performDeleteEvent(pendingDelete)
            }
        }
    }

    fun onSelectViewMode(mode: CalendarViewMode) {
        localControls.update { it.copy(viewMode = mode) }
    }

    fun onSelectRange(range: AgendaRange) {
        localControls.update { it.copy(selectedRange = range) }
    }

    fun onPreviousMonth() {
        localControls.update { state ->
            val prev = state.displayedMonth.minusMonths(1)
            state.copy(displayedMonth = prev)
        }
    }

    fun onNextMonth() {
        localControls.update { state ->
            val next = state.displayedMonth.plusMonths(1)
            state.copy(displayedMonth = next)
        }
    }

    fun onJumpToToday() {
        val today = Instant.ofEpochMilli(timeProvider.nowMillis())
            .atZone(timeProvider.zoneId())
            .toLocalDate()
        localControls.update {
            it.copy(
                displayedMonth = YearMonth.from(today),
                selectedDate = today,
            )
        }
    }

    fun onSelectDate(date: LocalDate) {
        localControls.update {
            it.copy(
                selectedDate = date,
                displayedMonth = YearMonth.from(date),
            )
        }
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

    fun onOpenCreateEventSheet(prefillDate: LocalDate? = null) {
        val targetDate = prefillDate ?: localControls.value.selectedDate
        localControls.update { state ->
            state.copy(
                showCreateEventSheet = true,
                createDraft = CreateEventDraft(
                    date = targetDate,
                    calendarId = uiState.value.createDraft.calendarId,
                ),
            )
        }
    }

    fun onDismissCreateEventSheet() {
        localControls.update {
            it.copy(
                showCreateEventSheet = false,
                pendingCreateAfterWritePermission = false,
            )
        }
    }

    fun onDraftTitleChanged(title: String) {
        localControls.update { it.copy(createDraft = it.createDraft.copy(title = title)) }
    }

    fun onDraftCalendarSelected(calendarId: Long) {
        localControls.update { it.copy(createDraft = it.createDraft.copy(calendarId = calendarId)) }
    }

    fun onDraftShiftDate(daysDelta: Long) {
        localControls.update {
            it.copy(createDraft = it.createDraft.copy(date = it.createDraft.date.plusDays(daysDelta)))
        }
    }

    fun onDraftToggleAllDay(allDay: Boolean) {
        localControls.update { it.copy(createDraft = it.createDraft.copy(isAllDay = allDay)) }
    }

    fun onDraftAdjustStartMinutes(deltaMinutes: Int) {
        localControls.update { state ->
            val currentTotal = (state.createDraft.startHour * 60 + state.createDraft.startMinute + deltaMinutes)
                .coerceIn(0, 23 * 60 + 30)
            state.copy(
                createDraft = state.createDraft.copy(
                    startHour = currentTotal / 60,
                    startMinute = currentTotal % 60,
                ),
            )
        }
    }

    fun onDraftAdjustDurationMinutes(deltaMinutes: Int) {
        localControls.update { state ->
            val updated = (state.createDraft.durationMinutes + deltaMinutes).coerceIn(15, 720)
            state.copy(createDraft = state.createDraft.copy(durationMinutes = updated))
        }
    }

    fun onDraftLocationChanged(location: String) {
        localControls.update { it.copy(createDraft = it.createDraft.copy(location = location)) }
    }

    fun onDraftDescriptionChanged(description: String) {
        localControls.update { it.copy(createDraft = it.createDraft.copy(description = description)) }
    }

    fun onPrepareSaveCreatedEventNeedsWritePermission() {
        localControls.update { it.copy(pendingCreateAfterWritePermission = true) }
    }

    fun onSaveCreatedEvent() {
        if (localControls.value.isMutatingEvent) return
        val draft = localControls.value.createDraft
        val calendarId = draft.calendarId ?: uiState.value.createDraft.calendarId
        if (draft.title.isBlank()) {
            localControls.update { it.copy(syncFeedbackMessage = "Please enter an event title.") }
            return
        }
        if (calendarId == null) {
            localControls.update {
                it.copy(syncFeedbackMessage = "No synced Android/Google calendar found. Tap Sync first.")
            }
            return
        }

        val zoneId = timeProvider.zoneId()
        val (startMillis, endMillis) = if (draft.isAllDay) {
            val startUtc = draft.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val endUtc = draft.date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            startUtc to endUtc
        } else {
            val startTime = LocalTime.of(draft.startHour.coerceIn(0, 23), draft.startMinute.coerceIn(0, 59))
            val startZoned = draft.date.atTime(startTime).atZone(zoneId)
            val endZoned = startZoned.plusMinutes(draft.durationMinutes.toLong())
            startZoned.toInstant().toEpochMilli() to endZoned.toInstant().toEpochMilli()
        }

        viewModelScope.launch {
            localControls.update { it.copy(isMutatingEvent = true, syncFeedbackMessage = null) }
            val result = createCalendarEvent(
                CreateCalendarEventInput(
                    calendarId = calendarId,
                    title = draft.title.trim(),
                    startTimeMillis = startMillis,
                    endTimeMillis = endMillis,
                    isAllDay = draft.isAllDay,
                    location = draft.location.trim().takeIf { it.isNotEmpty() },
                    description = draft.description.trim().takeIf { it.isNotEmpty() },
                    timezone = zoneId.id,
                ),
            )
            if (result is CalendarMutationResult.Created) {
                rescheduleWatchReminders()
            }
            localControls.update { state ->
                when (result) {
                    is CalendarMutationResult.Created -> state.copy(
                        isMutatingEvent = false,
                        showCreateEventSheet = false,
                        selectedDate = draft.date,
                        displayedMonth = YearMonth.from(draft.date),
                        syncFeedbackMessage = "Added \"${draft.title.trim()}\" to Android/Google Calendar & Watch Reminder queue.",
                    )

                    CalendarMutationResult.PermissionDenied -> state.copy(
                        isMutatingEvent = false,
                        hasWritePermission = false,
                        syncFeedbackMessage = "WRITE_CALENDAR permission is required to add events.",
                    )

                    is CalendarMutationResult.Error -> state.copy(
                        isMutatingEvent = false,
                        syncFeedbackMessage = result.message,
                    )

                    CalendarMutationResult.Deleted -> state.copy(isMutatingEvent = false)
                }
            }
        }
    }

    fun onRequestDeleteEvent(event: CalendarEventUiModel) {
        localControls.update { it.copy(pendingDeleteEvent = event) }
    }

    fun onDismissDeleteEvent() {
        localControls.update { it.copy(pendingDeleteEvent = null) }
    }

    fun onPrepareDeleteNeedsWritePermission(event: CalendarEventUiModel) {
        localControls.update {
            it.copy(
                pendingDeleteEvent = null,
                pendingDeleteAfterWritePermission = event,
            )
        }
    }

    fun onConfirmDeleteEvent() {
        val target = localControls.value.pendingDeleteEvent ?: return
        localControls.update { it.copy(pendingDeleteEvent = null) }
        performDeleteEvent(target)
    }

    private fun performDeleteEvent(target: CalendarEventUiModel) {
        if (localControls.value.isMutatingEvent) return
        viewModelScope.launch {
            localControls.update { it.copy(isMutatingEvent = true, syncFeedbackMessage = null) }
            val result = deleteCalendarEvent(target.eventId)
            if (result is CalendarMutationResult.Deleted) {
                rescheduleWatchReminders()
            }
            localControls.update { state ->
                when (result) {
                    CalendarMutationResult.Deleted -> state.copy(
                        isMutatingEvent = false,
                        syncFeedbackMessage = "Deleted \"${target.title}\" from calendar.",
                    )

                    CalendarMutationResult.PermissionDenied -> state.copy(
                        isMutatingEvent = false,
                        hasWritePermission = false,
                        syncFeedbackMessage = "WRITE_CALENDAR permission is required to delete events.",
                    )

                    is CalendarMutationResult.Error -> state.copy(
                        isMutatingEvent = false,
                        syncFeedbackMessage = result.message,
                    )

                    is CalendarMutationResult.Created -> state.copy(isMutatingEvent = false)
                }
            }
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

    private fun CalendarEvent.toUiModel(zoneId: java.time.ZoneId): CalendarEventUiModel =
        CalendarEventUiModel(
            instanceKey = instanceKey,
            eventId = eventId,
            title = title,
            timeLabel = AgendaTimeWindows.formatTimeRange(this, zoneId),
            calendarName = calendarName,
            calendarColor = calendarColor,
            location = location,
            isAllDay = isAllDay,
        )

    private data class LocalCalendarControls(
        val hasPermission: Boolean = false,
        val hasWritePermission: Boolean = false,
        val permissionDeniedOnce: Boolean = false,
        val viewMode: CalendarViewMode = CalendarViewMode.MONTH_GRID,
        val selectedRange: AgendaRange = AgendaRange.WEEK,
        val displayedMonth: YearMonth = YearMonth.now(),
        val selectedDate: LocalDate = LocalDate.now(),
        val searchQuery: String = "",
        val isSyncing: Boolean = false,
        val isMutatingEvent: Boolean = false,
        val syncFeedbackMessage: String? = null,
        val showCalendarFilterSheet: Boolean = false,
        val showCreateEventSheet: Boolean = false,
        val createDraft: CreateEventDraft = CreateEventDraft(),
        val pendingCreateAfterWritePermission: Boolean = false,
        val pendingDeleteEvent: CalendarEventUiModel? = null,
        val pendingDeleteAfterWritePermission: CalendarEventUiModel? = null,
    )

    private companion object {
        val MONTH_HEADER_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    }
}
