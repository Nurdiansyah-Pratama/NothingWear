package com.nothingwear.app.domain.model

data class CalendarSource(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val color: Int,
    val isSelected: Boolean,
    val isPrimary: Boolean,
) {
    val isGoogleCalendar: Boolean
        get() = accountType.equals("com.google", ignoreCase = true)
}

data class CalendarEvent(
    val instanceKey: String,
    val eventId: Long,
    val calendarId: Long,
    val calendarName: String,
    val calendarColor: Int,
    val title: String,
    val description: String?,
    val location: String?,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val isAllDay: Boolean,
    val timezone: String?,
    val lastSyncedAtMillis: Long,
)

enum class AgendaRange(val label: String) {
    TODAY("Today"),
    WEEK("7 Days"),
    UPCOMING("30 Days"),
}

data class CalendarSyncMetadata(
    val lastSyncedAtMillis: Long? = null,
    val selectedCalendarCount: Int = 0,
    val totalCalendarCount: Int = 0,
    val cachedEventCount: Int = 0,
)

sealed interface CalendarSyncResult {
    data class Success(
        val syncedCalendars: Int,
        val syncedEvents: Int,
        val syncedAtMillis: Long,
    ) : CalendarSyncResult

    data object PermissionDenied : CalendarSyncResult

    data class Error(val message: String) : CalendarSyncResult
}
