package com.nothingwear.app.features.calendar

import com.nothingwear.app.domain.model.AgendaRange
import com.nothingwear.app.domain.model.CalendarSource
import com.nothingwear.app.domain.model.CalendarSyncMetadata

data class CalendarEventUiModel(
    val instanceKey: String,
    val title: String,
    val timeLabel: String,
    val calendarName: String,
    val calendarColor: Int,
    val location: String?,
    val isAllDay: Boolean,
)

data class AgendaDayGroup(
    val dateHeader: String,
    val events: List<CalendarEventUiModel>,
)

data class CalendarUiState(
    val hasPermission: Boolean = false,
    val permissionDeniedOnce: Boolean = false,
    val selectedRange: AgendaRange = AgendaRange.WEEK,
    val searchQuery: String = "",
    val isSyncing: Boolean = false,
    val syncFeedbackMessage: String? = null,
    val lastSyncedLabel: String? = null,
    val syncMetadata: CalendarSyncMetadata = CalendarSyncMetadata(),
    val calendars: List<CalendarSource> = emptyList(),
    val groupedEvents: List<AgendaDayGroup> = emptyList(),
    val totalFilteredEvents: Int = 0,
    val showCalendarFilterSheet: Boolean = false,
)
