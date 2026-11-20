package com.nothingwear.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nothingwear.app.core.database.dao.CalendarDao
import com.nothingwear.app.core.database.entity.CalendarAccountEntity
import com.nothingwear.app.core.database.entity.CalendarEventEntity

@Database(
    entities = [
        CalendarAccountEntity::class,
        CalendarEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NothingWearDatabase : RoomDatabase() {
    abstract fun calendarDao(): CalendarDao
}
