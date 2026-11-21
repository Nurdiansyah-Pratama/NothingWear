package com.nothingwear.app.features.calendar

import com.nothingwear.app.domain.model.AgendaRange
import com.nothingwear.app.domain.model.CalendarSource
import com.nothingwear.app.domain.model.CalendarSyncMetadata
import java.time.LocalDate
import java.time.YearMonth

enum class CalendarViewMode(val label: String) {
    MONTH_GRID("Month"),
    AGENDA_LIST("List"),
}

data class CalendarEventUiModel(
    val instanceKey: String,
    val eventId: Long,
    val title: String,
    val timeLabel: String,
    val calendarName: String,
    val calendarColor: Int,
    val location: String?,
    val isAllDay: Boolean,
)

data class CalendarMonthCell(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val events: List<CalendarEventUiModel>,
)

data class AgendaDayGroup(
    val dateHeader: String,
    val events: List<CalendarEventUiModel>,
)

data class CreateEventDraft(
    val title: String = "",
    val calendarId: Long? = null,
    val date: LocalDate = LocalDate.now(),
    val isAllDay: Boolean = false,
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val durationMinutes: Int = 60,
    val location: String = "",
    val description: String = "",
)

data class CalendarUiState(
    val hasPermission: Boolean = false,
    val hasWritePermission: Boolean = false,
    val permissionDeniedOnce: Boolean = false,
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH_GRID,
    val selectedRange: AgendaRange = AgendaRange.WEEK,
    val displayedMonth: YearMonth = YearMonth.now(),
    val displayedMonthLabel: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDateHeader: String = "",
    val monthCells: List<CalendarMonthCell> = emptyList(),
    val selectedDateEvents: List<CalendarEventUiModel> = emptyList(),
    val searchQuery: String = "",
    val isSyncing: Boolean = false,
    val isMutatingEvent: Boolean = false,
    val syncFeedbackMessage: String? = null,
    val lastSyncedLabel: String? = null,
    val syncMetadata: CalendarSyncMetadata = CalendarSyncMetadata(),
    val calendars: List<CalendarSource> = emptyList(),
    val groupedEvents: List<AgendaDayGroup> = emptyList(),
    val totalFilteredEvents: Int = 0,
    val showCalendarFilterSheet: Boolean = false,
    val showCreateEventSheet: Boolean = false,
    val createDraft: CreateEventDraft = CreateEventDraft(),
    val pendingDeleteEvent: CalendarEventUiModel? = null,
)
