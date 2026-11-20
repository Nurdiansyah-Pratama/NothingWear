package com.nothingwear.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calendar_events",
    indices = [
        Index(value = ["calendar_id"]),
        Index(value = ["start_time_millis"]),
    ],
)
data class CalendarEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "instance_key")
    val instanceKey: String,
    @ColumnInfo(name = "event_id")
    val eventId: Long,
    @ColumnInfo(name = "calendar_id")
    val calendarId: Long,
    @ColumnInfo(name = "calendar_display_name")
    val calendarDisplayName: String,
    @ColumnInfo(name = "calendar_color")
    val calendarColor: Int,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String?,
    @ColumnInfo(name = "location")
    val location: String?,
    @ColumnInfo(name = "start_time_millis")
    val startTimeMillis: Long,
    @ColumnInfo(name = "end_time_millis")
    val endTimeMillis: Long,
    @ColumnInfo(name = "is_all_day")
    val isAllDay: Boolean,
    @ColumnInfo(name = "timezone")
    val timezone: String?,
    @ColumnInfo(name = "last_synced_at_millis")
    val lastSyncedAtMillis: Long,
)
