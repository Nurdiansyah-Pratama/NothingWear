package com.nothingwear.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.core.database.dao.CalendarDao
import com.nothingwear.app.core.database.entity.CalendarAccountEntity
import com.nothingwear.app.core.database.entity.CalendarEventEntity
import com.nothingwear.app.core.permissions.CalendarPermissionChecker
import com.nothingwear.app.data.calendar.DeviceCalendarDataSource
import com.nothingwear.app.domain.model.CalendarEvent
import com.nothingwear.app.domain.model.CalendarSource
import com.nothingwear.app.domain.model.CalendarSyncMetadata
import com.nothingwear.app.domain.model.CalendarSyncResult
import com.nothingwear.app.domain.repository.CalendarRepository
import com.nothingwear.app.domain.util.AgendaTimeWindows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstCalendarRepository @Inject constructor(
    private val calendarDao: CalendarDao,
    private val deviceCalendarDataSource: DeviceCalendarDataSource,
    private val dataStore: DataStore<Preferences>,
    private val permissionChecker: CalendarPermissionChecker,
    private val timeProvider: TimeProvider,
) : CalendarRepository {

    private val lastSyncMillisFlow: Flow<Long?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs -> prefs[KEY_LAST_SYNC_MILLIS] }

    override fun observeCalendars(): Flow<List<CalendarSource>> =
        calendarDao.observeCalendars().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeSelectedEvents(): Flow<List<CalendarEvent>> =
        calendarDao.observeSelectedEvents().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeEventDetail(instanceKey: String): Flow<CalendarEvent?> =
        calendarDao.observeEventByInstanceKey(instanceKey).map { it?.toDomain() }

    override fun observeSyncMetadata(): Flow<CalendarSyncMetadata> = combine(
        lastSyncMillisFlow,
        calendarDao.observeCalendars(),
        calendarDao.observeSelectedEvents(),
    ) { lastSync, calendars, selectedEvents ->
        CalendarSyncMetadata(
            lastSyncedAtMillis = lastSync,
            selectedCalendarCount = calendars.count { it.isSelected },
            totalCalendarCount = calendars.size,
            cachedEventCount = selectedEvents.size,
        )
    }

    override suspend fun syncFromDevice(): CalendarSyncResult {
        if (!permissionChecker.hasReadCalendarPermission()) {
            return CalendarSyncResult.PermissionDenied
        }

        return try {
            val nowMillis = timeProvider.nowMillis()
            val zoneId = timeProvider.zoneId()
            val window = AgendaTimeWindows.providerFetchWindow(nowMillis, zoneId)

            val rawCalendars = deviceCalendarDataSource.fetchCalendars()
            val existingSelectionById = calendarDao.getAllCalendars()
                .associate { it.calendarId to it.isSelected }

            val calendarEntities = rawCalendars.map { raw ->
                CalendarAccountEntity(
                    calendarId = raw.calendarId,
                    displayName = raw.displayName,
                    accountName = raw.accountName,
                    accountType = raw.accountType,
                    color = raw.color,
                    isSelected = existingSelectionById[raw.calendarId] ?: raw.isVisible,
                    isPrimary = raw.isPrimary,
                )
            }

            val rawInstances = deviceCalendarDataSource.fetchEventInstances(
                startMillis = window.startMillis,
                endMillis = window.endMillis,
            )

            val eventEntities = rawInstances
                .distinctBy { "${it.eventId}_${it.startTimeMillis}" }
                .map { raw ->
                    CalendarEventEntity(
                        instanceKey = "${raw.eventId}_${raw.startTimeMillis}",
                        eventId = raw.eventId,
                        calendarId = raw.calendarId,
                        calendarDisplayName = raw.calendarDisplayName,
                        calendarColor = raw.calendarColor,
                        title = raw.title,
                        description = raw.description,
                        location = raw.location,
                        startTimeMillis = raw.startTimeMillis,
                        endTimeMillis = raw.endTimeMillis,
                        isAllDay = raw.isAllDay,
                        timezone = raw.timezone,
                        lastSyncedAtMillis = nowMillis,
                    )
                }

            calendarDao.replaceCalendarCache(
                calendars = calendarEntities,
                events = eventEntities,
            )

            dataStore.edit { prefs ->
                prefs[KEY_LAST_SYNC_MILLIS] = nowMillis
            }

            CalendarSyncResult.Success(
                syncedCalendars = calendarEntities.size,
                syncedEvents = eventEntities.size,
                syncedAtMillis = nowMillis,
            )
        } catch (_: SecurityException) {
            CalendarSyncResult.PermissionDenied
        } catch (e: Exception) {
            CalendarSyncResult.Error(e.message ?: "Failed to read Android Calendar Provider.")
        }
    }

    override suspend fun setCalendarSelected(calendarId: Long, selected: Boolean) {
        calendarDao.setCalendarSelected(calendarId = calendarId, selected = selected)
    }

    private fun CalendarAccountEntity.toDomain() = CalendarSource(
        id = calendarId,
        displayName = displayName,
        accountName = accountName,
        accountType = accountType,
        color = color,
        isSelected = isSelected,
        isPrimary = isPrimary,
    )

    private fun CalendarEventEntity.toDomain() = CalendarEvent(
        instanceKey = instanceKey,
        eventId = eventId,
        calendarId = calendarId,
        calendarName = calendarDisplayName,
        calendarColor = calendarColor,
        title = title,
        description = description,
        location = location,
        startTimeMillis = startTimeMillis,
        endTimeMillis = endTimeMillis,
        isAllDay = isAllDay,
        timezone = timezone,
        lastSyncedAtMillis = lastSyncedAtMillis,
    )

    private companion object {
        val KEY_LAST_SYNC_MILLIS = longPreferencesKey("calendar_last_sync_millis")
    }
}
