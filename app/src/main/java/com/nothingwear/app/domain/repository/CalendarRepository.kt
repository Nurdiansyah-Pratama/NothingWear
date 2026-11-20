package com.nothingwear.app.domain.repository

import com.nothingwear.app.domain.model.CalendarEvent
import com.nothingwear.app.domain.model.CalendarSource
import com.nothingwear.app.domain.model.CalendarSyncMetadata
import com.nothingwear.app.domain.model.CalendarSyncResult
import kotlinx.coroutines.flow.Flow

interface CalendarRepository {
    fun observeCalendars(): Flow<List<CalendarSource>>

    fun observeSelectedEvents(): Flow<List<CalendarEvent>>

    fun observeEventDetail(instanceKey: String): Flow<CalendarEvent?>

    fun observeSyncMetadata(): Flow<CalendarSyncMetadata>

    suspend fun syncFromDevice(): CalendarSyncResult

    suspend fun setCalendarSelected(calendarId: Long, selected: Boolean)
}
