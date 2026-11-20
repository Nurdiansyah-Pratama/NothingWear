package com.nothingwear.protocol.watchs4

/**
 * Evidence-backed calendar event payload model aligned with the M3 structural findings
 * from `com.xiaomi.wearable 3.60.0i` (`BlueToothSender.handleCalendar` / `CalendarModel`).
 *
 * NOTE: Wire-level Protobuf field tags and `WearAuthV2` session cipher frames are
 * intentionally NOT fabricated here. This model provides clean validation and bounded
 * text sanitization for diagnostics and any future verified transport.
 */
data class WatchS4EventPayload(
    val title: String,
    val location: String?,
    val description: String?,
    val startEpochSeconds: Long,
    val endEpochSeconds: Long,
    val isAllDay: Boolean,
    val reminderMinutes: Int,
    val calendarName: String,
) {
    companion object {
        const val MAX_TITLE_CHARS = 80
        const val MAX_LOCATION_CHARS = 80
        const val MAX_DESCRIPTION_CHARS = 240

        fun sanitized(
            title: String,
            location: String?,
            description: String?,
            startEpochSeconds: Long,
            endEpochSeconds: Long,
            isAllDay: Boolean,
            reminderMinutes: Int,
            calendarName: String,
        ): WatchS4EventPayload {
            val cleanTitle = title.trim().ifBlank { "(Untitled Event)" }.take(MAX_TITLE_CHARS)
            val cleanLocation = location?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_LOCATION_CHARS)
            val cleanDescription = description?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_DESCRIPTION_CHARS)
            val normalizedEnd = maxOf(endEpochSeconds, startEpochSeconds)
            return WatchS4EventPayload(
                title = cleanTitle,
                location = cleanLocation,
                description = cleanDescription,
                startEpochSeconds = startEpochSeconds,
                endEpochSeconds = normalizedEnd,
                isAllDay = isAllDay,
                reminderMinutes = reminderMinutes.coerceAtLeast(0),
                calendarName = calendarName.trim().ifBlank { "Calendar" }.take(48),
            )
        }
    }
}

sealed interface WatchS4SyncOutcome {
    data class BlockedUnsupported(
        val state: WatchS4AdapterState,
        val reason: String,
        val validatedPayloadCount: Int,
    ) : WatchS4SyncOutcome
}
