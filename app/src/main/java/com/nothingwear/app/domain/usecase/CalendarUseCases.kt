package com.nothingwear.app.domain.usecase

import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.domain.model.AgendaRange
import com.nothingwear.app.domain.model.CalendarEvent
import com.nothingwear.app.domain.model.CalendarMutationResult
import com.nothingwear.app.domain.model.CalendarSource
import com.nothingwear.app.domain.model.CalendarSyncMetadata
import com.nothingwear.app.domain.model.CalendarSyncResult
import com.nothingwear.app.domain.model.CreateCalendarEventInput
import com.nothingwear.app.domain.repository.CalendarRepository
import com.nothingwear.app.domain.util.AgendaTimeWindows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveCalendarsUseCase @Inject constructor(
    private val repository: CalendarRepository,
) {
    operator fun invoke(): Flow<List<CalendarSource>> = repository.observeCalendars()
}

class ObserveAgendaEventsUseCase @Inject constructor(
    private val repository: CalendarRepository,
    private val timeProvider: TimeProvider,
) {
    operator fun invoke(
        range: AgendaRange,
        query: String = "",
    ): Flow<List<CalendarEvent>> = repository.observeSelectedEvents().map { events ->
        val zoneId = timeProvider.zoneId()
        val window = AgendaTimeWindows.windowFor(
            range = range,
            nowMillis = timeProvider.nowMillis(),
            zoneId = zoneId,
        )
        events
            .asSequence()
            .filter { AgendaTimeWindows.overlaps(it, window, zoneId) }
            .filter { AgendaTimeWindows.matchesQuery(it, query) }
            .sortedWith(
                compareBy<CalendarEvent> { AgendaTimeWindows.normalizedInterval(it, zoneId).first }
                    .thenByDescending { it.isAllDay }
                    .thenBy { it.title.lowercase() },
            )
            .toList()
    }
}

class ObserveEventDetailUseCase @Inject constructor(
    private val repository: CalendarRepository,
) {
    operator fun invoke(instanceKey: String): Flow<CalendarEvent?> =
        repository.observeEventDetail(instanceKey)
}

class ObserveCalendarSyncMetadataUseCase @Inject constructor(
    private val repository: CalendarRepository,
) {
    operator fun invoke(): Flow<CalendarSyncMetadata> = repository.observeSyncMetadata()
}

class SyncDeviceCalendarsUseCase @Inject constructor(
    private val repository: CalendarRepository,
) {
    suspend operator fun invoke(): CalendarSyncResult = repository.syncFromDevice()
}

class ToggleCalendarSelectionUseCase @Inject constructor(
    private val repository: CalendarRepository,
) {
    suspend operator fun invoke(calendarId: Long, selected: Boolean) {
        repository.setCalendarSelected(calendarId, selected)
    }
}

class CreateCalendarEventUseCase @Inject constructor(
    private val repository: CalendarRepository,
) {
    suspend operator fun invoke(input: CreateCalendarEventInput): CalendarMutationResult =
        repository.createEvent(input)
}

class DeleteCalendarEventUseCase @Inject constructor(
    private val repository: CalendarRepository,
) {
    suspend operator fun invoke(eventId: Long): CalendarMutationResult =
        repository.deleteEvent(eventId)
}
