package com.nothingwear.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nothingwear.app.core.database.dao.CalendarDao
import com.nothingwear.app.core.database.dao.WatchFaceDao
import com.nothingwear.app.core.database.entity.CalendarAccountEntity
import com.nothingwear.app.core.database.entity.CalendarEventEntity
import com.nothingwear.app.core.database.entity.WatchFaceProjectEntity

@Database(
    entities = [
        CalendarAccountEntity::class,
        CalendarEventEntity::class,
        WatchFaceProjectEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class NothingWearDatabase : RoomDatabase() {
    abstract fun calendarDao(): CalendarDao
    abstract fun watchFaceDao(): WatchFaceDao
}
