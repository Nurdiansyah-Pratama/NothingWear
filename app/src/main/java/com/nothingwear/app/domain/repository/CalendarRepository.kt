package com.nothingwear.app.domain.repository

import com.nothingwear.app.domain.model.CalendarEvent
import com.nothingwear.app.domain.model.CalendarMutationResult
import com.nothingwear.app.domain.model.CalendarSource
import com.nothingwear.app.domain.model.CalendarSyncMetadata
import com.nothingwear.app.domain.model.CalendarSyncResult
import com.nothingwear.app.domain.model.CreateCalendarEventInput
import kotlinx.coroutines.flow.Flow

interface CalendarRepository {
    fun observeCalendars(): Flow<List<CalendarSource>>

    fun observeSelectedEvents(): Flow<List<CalendarEvent>>

    fun observeEventDetail(instanceKey: String): Flow<CalendarEvent?>

    fun observeSyncMetadata(): Flow<CalendarSyncMetadata>

    suspend fun syncFromDevice(): CalendarSyncResult

    suspend fun setCalendarSelected(calendarId: Long, selected: Boolean)

    suspend fun createEvent(input: CreateCalendarEventInput): CalendarMutationResult =
        CalendarMutationResult.PermissionDenied

    suspend fun deleteEvent(eventId: Long): CalendarMutationResult =
        CalendarMutationResult.PermissionDenied
}
