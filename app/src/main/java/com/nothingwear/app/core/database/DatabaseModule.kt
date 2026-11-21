package com.nothingwear.app.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nothingwear.app.core.database.dao.CalendarDao
import com.nothingwear.app.core.database.dao.WatchFaceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `watch_face_projects` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `dial_style` TEXT NOT NULL,
                    `ring_style` TEXT NOT NULL,
                    `font_style` TEXT NOT NULL,
                    `background_color_argb` INTEGER NOT NULL,
                    `accent_color_argb` INTEGER NOT NULL,
                    `text_color_argb` INTEGER NOT NULL,
                    `is_24_hour` INTEGER NOT NULL,
                    `show_seconds` INTEGER NOT NULL,
                    `time_scale` REAL NOT NULL,
                    `top_complication` TEXT NOT NULL,
                    `bottom_complication` TEXT NOT NULL,
                    `left_complication` TEXT NOT NULL,
                    `right_complication` TEXT NOT NULL,
                    `updated_at_millis` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

    @Provides
    @Singleton
    fun provideNothingWearDatabase(
        @ApplicationContext context: Context,
    ): NothingWearDatabase = Room.databaseBuilder(
        context,
        NothingWearDatabase::class.java,
        "nothingwear.db",
    )
        .addMigrations(MIGRATION_1_2)
        .build()

    @Provides
    fun provideCalendarDao(
        database: NothingWearDatabase,
    ): CalendarDao = database.calendarDao()

    @Provides
    fun provideWatchFaceDao(
        database: NothingWearDatabase,
    ): WatchFaceDao = database.watchFaceDao()
}
