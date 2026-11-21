package com.nothingwear.app.data.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.nothingwear.app.core.common.IoDispatcher
import com.nothingwear.app.domain.model.CreateCalendarEventInput
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class RawDeviceCalendar(
    val calendarId: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val color: Int,
    val isVisible: Boolean,
    val isPrimary: Boolean,
)

data class RawDeviceEventInstance(
    val eventId: Long,
    val calendarId: Long,
    val calendarDisplayName: String,
    val calendarColor: Int,
    val title: String,
    val description: String?,
    val location: String?,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val isAllDay: Boolean,
    val timezone: String?,
)

interface DeviceCalendarDataSource {
    suspend fun fetchCalendars(): List<RawDeviceCalendar>
    suspend fun fetchEventInstances(startMillis: Long, endMillis: Long): List<RawDeviceEventInstance>
    suspend fun createEvent(input: CreateCalendarEventInput): Long = -1L
    suspend fun deleteEvent(eventId: Long): Int = 0
}

@Singleton
class AndroidCalendarDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DeviceCalendarDataSource {

    override suspend fun fetchCalendars(): List<RawDeviceCalendar> = withContext(ioDispatcher) {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.IS_PRIMARY,
        )

        val results = mutableListOf<RawDeviceCalendar>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC",
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
            val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountNameIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
            val accountTypeIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
            val colorIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
            val visibleIdx = cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE)
            val primaryIdx = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)

            while (cursor.moveToNext()) {
                if (idIdx < 0) continue
                val id = cursor.getLong(idIdx)
                val displayName = nameIdx.takeIf { it >= 0 }?.let(cursor::getString).orEmpty()
                    .ifBlank { "Untitled Calendar" }
                val accountName = accountNameIdx.takeIf { it >= 0 }?.let(cursor::getString).orEmpty()
                val accountType = accountTypeIdx.takeIf { it >= 0 }?.let(cursor::getString).orEmpty()
                val color = colorIdx.takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let(cursor::getInt)
                    ?: DEFAULT_CALENDAR_COLOR
                val visible = visibleIdx.takeIf { it >= 0 }?.let(cursor::getInt) == 1
                val primary = primaryIdx.takeIf { it >= 0 }?.let(cursor::getInt) == 1

                results += RawDeviceCalendar(
                    calendarId = id,
                    displayName = displayName,
                    accountName = accountName,
                    accountType = accountType,
                    color = color,
                    isVisible = visible,
                    isPrimary = primary,
                )
            }
        }
        results
    }

    override suspend fun fetchEventInstances(
        startMillis: Long,
        endMillis: Long,
    ): List<RawDeviceEventInstance> = withContext(ioDispatcher) {
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_TIMEZONE,
        )

        val results = mutableListOf<RawDeviceEventInstance>()
        context.contentResolver.query(
            builder.build(),
            projection,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
            val eventIdIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
            val calendarIdIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)
            val calNameIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
            val colorIdx = cursor.getColumnIndex(CalendarContract.Instances.DISPLAY_COLOR)
            val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
            val descIdx = cursor.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
            val locationIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
            val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
            val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
            val tzIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_TIMEZONE)

            while (cursor.moveToNext()) {
                if (eventIdIdx < 0 || calendarIdIdx < 0 || beginIdx < 0) continue
                val eventId = cursor.getLong(eventIdIdx)
                val calendarId = cursor.getLong(calendarIdIdx)
                val begin = cursor.getLong(beginIdx)
                val end = endIdx.takeIf { it >= 0 && !cursor.isNull(it) }?.let(cursor::getLong) ?: begin
                val title = titleIdx.takeIf { it >= 0 }?.let(cursor::getString).orEmpty()
                    .ifBlank { "(No Title)" }
                val calName = calNameIdx.takeIf { it >= 0 }?.let(cursor::getString).orEmpty()
                    .ifBlank { "Calendar" }
                val color = colorIdx.takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let(cursor::getInt)
                    ?: DEFAULT_CALENDAR_COLOR
                val description = descIdx.takeIf { it >= 0 }?.let(cursor::getString)?.trim()
                    ?.takeIf { it.isNotEmpty() }
                val location = locationIdx.takeIf { it >= 0 }?.let(cursor::getString)?.trim()
                    ?.takeIf { it.isNotEmpty() }
                val isAllDay = allDayIdx.takeIf { it >= 0 }?.let(cursor::getInt) == 1
                val timezone = tzIdx.takeIf { it >= 0 }?.let(cursor::getString)?.takeIf { it.isNotBlank() }

                results += RawDeviceEventInstance(
                    eventId = eventId,
                    calendarId = calendarId,
                    calendarDisplayName = calName,
                    calendarColor = color,
                    title = title,
                    description = description,
                    location = location,
                    startTimeMillis = begin,
                    endTimeMillis = end,
                    isAllDay = isAllDay,
                    timezone = timezone,
                )
            }
        }
        results
    }

    override suspend fun createEvent(input: CreateCalendarEventInput): Long = withContext(ioDispatcher) {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, input.calendarId)
            put(CalendarContract.Events.TITLE, input.title.trim())
            put(CalendarContract.Events.DTSTART, input.startTimeMillis)
            put(CalendarContract.Events.DTEND, maxOf(input.endTimeMillis, input.startTimeMillis))
            put(CalendarContract.Events.ALL_DAY, if (input.isAllDay) 1 else 0)
            put(CalendarContract.Events.EVENT_TIMEZONE, if (input.isAllDay) "UTC" else input.timezone)
            if (!input.location.isNullOrBlank()) {
                put(CalendarContract.Events.EVENT_LOCATION, input.location.trim())
            }
            if (!input.description.isNullOrBlank()) {
                put(CalendarContract.Events.DESCRIPTION, input.description.trim())
            }
        }
        val insertedUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            ?: throw IllegalStateException("Android Calendar Provider rejected event insert.")
        ContentUris.parseId(insertedUri)
    }

    override suspend fun deleteEvent(eventId: Long): Int = withContext(ioDispatcher) {
        val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        context.contentResolver.delete(deleteUri, null, null)
    }

    private companion object {
        const val DEFAULT_CALENDAR_COLOR = 0xFFD71921.toInt()
    }
}
