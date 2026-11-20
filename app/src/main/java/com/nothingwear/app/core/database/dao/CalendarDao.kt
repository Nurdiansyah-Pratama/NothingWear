package com.nothingwear.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.nothingwear.app.core.database.entity.CalendarAccountEntity
import com.nothingwear.app.core.database.entity.CalendarEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDao {

    @Query(
        """
        SELECT * FROM calendar_accounts
        ORDER BY is_primary DESC, display_name COLLATE NOCASE ASC
        """,
    )
    fun observeCalendars(): Flow<List<CalendarAccountEntity>>

    @Query("SELECT * FROM calendar_accounts")
    suspend fun getAllCalendars(): List<CalendarAccountEntity>

    @Upsert
    suspend fun upsertCalendars(calendars: List<CalendarAccountEntity>)

    @Query("DELETE FROM calendar_accounts WHERE calendar_id NOT IN (:activeIds)")
    suspend fun deleteCalendarsNotIn(activeIds: List<Long>)

    @Query("DELETE FROM calendar_accounts")
    suspend fun deleteAllCalendars()

    @Query("UPDATE calendar_accounts SET is_selected = :selected WHERE calendar_id = :calendarId")
    suspend fun setCalendarSelected(calendarId: Long, selected: Boolean)

    @Query(
        """
        SELECT e.* FROM calendar_events e
        INNER JOIN calendar_accounts c ON e.calendar_id = c.calendar_id
        WHERE c.is_selected = 1
        ORDER BY e.start_time_millis ASC, e.title COLLATE NOCASE ASC
        """,
    )
    fun observeSelectedEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE instance_key = :instanceKey LIMIT 1")
    fun observeEventByInstanceKey(instanceKey: String): Flow<CalendarEventEntity?>

    @Query("SELECT COUNT(*) FROM calendar_events")
    fun observeCachedEventCount(): Flow<Int>

    @Query("DELETE FROM calendar_events")
    suspend fun deleteAllEvents()

    @Upsert
    suspend fun upsertEvents(events: List<CalendarEventEntity>)

    @Transaction
    suspend fun replaceCalendarCache(
        calendars: List<CalendarAccountEntity>,
        events: List<CalendarEventEntity>,
    ) {
        if (calendars.isEmpty()) {
            deleteAllCalendars()
        } else {
            upsertCalendars(calendars)
            deleteCalendarsNotIn(calendars.map { it.calendarId })
        }
        deleteAllEvents()
        if (events.isNotEmpty()) {
            upsertEvents(events)
        }
    }
}
