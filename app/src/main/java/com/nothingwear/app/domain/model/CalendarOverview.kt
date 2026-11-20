package com.nothingwear.app.domain.model

enum class CalendarSyncState {
    NOT_CONFIGURED,
    PERMISSION_REQUIRED,
    LOCAL_ONLY,
    SYNCING,
    ERROR,
}

/**
 * Domain representation of a calendar agenda preview item for Dashboard cards.
 */
data class AgendaItem(
    val id: String,
    val title: String,
    val timeLabel: String,
    val dateLabel: String,
    val calendarName: String,
    val calendarColor: Int,
    val location: String? = null,
)

data class CalendarOverview(
    val localCalendarState: CalendarSyncState,
    val localStatusMessage: String,
    val nativeWatchSyncState: WatchConnectionState,
    val nativeWatchSyncMessage: String,
    val todayEvents: List<AgendaItem>,
    val upcomingEvents: List<AgendaItem>,
)
