package com.nothingwear.app.core.database

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nothingwear.app.core.database.entity.WatchFaceProjectEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * M7 Instrumented Room migration verification on real Android SQLite runtime.
 *
 * Creates a genuine schema v1 SQLite database (exact DDL from exported 1.json), opens
 * [NothingWearDatabase] with [DatabaseModule.MIGRATION_1_2] (which triggers Room's
 * full runtime schema validator), and verifies v1 calendar data is preserved while
 * v2 Watch Face Studio CRUD succeeds.
 */
@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {

    @Test
    fun migrate1To2PreservesCalendarDataAndValidatesRoomV2Schema() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbName = "nothingwear-migration-v1-v2-test.db"
        context.deleteDatabase(dbName)

        try {
            val dbFile = context.getDatabasePath(dbName)
            dbFile.parentFile?.mkdirs()

            // 1. Seed a genuine Room schema v1 SQLite file (exact DDL from schemas/.../1.json)
            SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { v1Db ->
                v1Db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `calendar_accounts` (`calendar_id` INTEGER NOT NULL, `display_name` TEXT NOT NULL, `account_name` TEXT NOT NULL, `account_type` TEXT NOT NULL, `color` INTEGER NOT NULL, `is_selected` INTEGER NOT NULL, `is_primary` INTEGER NOT NULL, PRIMARY KEY(`calendar_id`))",
                )
                v1Db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `calendar_events` (`instance_key` TEXT NOT NULL, `event_id` INTEGER NOT NULL, `calendar_id` INTEGER NOT NULL, `calendar_display_name` TEXT NOT NULL, `calendar_color` INTEGER NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `location` TEXT, `start_time_millis` INTEGER NOT NULL, `end_time_millis` INTEGER NOT NULL, `is_all_day` INTEGER NOT NULL, `timezone` TEXT, `last_synced_at_millis` INTEGER NOT NULL, PRIMARY KEY(`instance_key`))",
                )
                v1Db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_calendar_events_calendar_id` ON `calendar_events` (`calendar_id`)",
                )
                v1Db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_calendar_events_start_time_millis` ON `calendar_events` (`start_time_millis`)",
                )
                v1Db.execSQL(
                    "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
                )
                v1Db.execSQL(
                    "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '8f46a624aaf23a75bb13ef5b079d7632')",
                )
                v1Db.execSQL(
                    """
                    INSERT INTO calendar_accounts (
                        calendar_id, display_name, account_name, account_type, color, is_selected, is_primary
                    ) VALUES (10, 'Personal', 'user@example.com', 'com.google', -16711936, 1, 1)
                    """.trimIndent(),
                )
                v1Db.execSQL(
                    """
                    INSERT INTO calendar_events (
                        instance_key, event_id, calendar_id, calendar_display_name, calendar_color,
                        title, description, location, start_time_millis, end_time_millis,
                        is_all_day, timezone, last_synced_at_millis
                    ) VALUES (
                        '10_100', 100, 10, 'Personal', -16711936,
                        'Migration Check Event', NULL, 'Lab', 1763690000000, 1763693600000,
                        0, 'Asia/Jakarta', 1763689000000
                    )
                    """.trimIndent(),
                )
                v1Db.version = 1
            }

            // 2. Open actual NothingWearDatabase (v2) with MIGRATION_1_2 -> triggers Room schema validation
            val roomDb = Room.databaseBuilder(
                context,
                NothingWearDatabase::class.java,
                dbName,
            )
                .addMigrations(DatabaseModule.MIGRATION_1_2)
                .build()

            try {
                // 3. Confirm v1 calendar account & event survived migration intact
                val accounts = roomDb.calendarDao().observeCalendars().first()
                assertEquals(1, accounts.size)
                assertEquals("Personal", accounts.first().displayName)

                val events = roomDb.calendarDao().observeSelectedEvents().first()
                assertEquals(1, events.size)
                assertEquals("Migration Check Event", events.first().title)

                // 4. Confirm new v2 watch_face_projects table works through WatchFaceDao
                val insertedId = roomDb.watchFaceDao().upsertProject(
                    WatchFaceProjectEntity(
                        name = "Migrated Watch Face",
                        category = "NOTHING",
                        dialStyle = "DIGITAL_STACKED",
                        ringStyle = "DOTS_12",
                        fontStyle = "DOT_INDUSTRIAL",
                        backgroundColorArgb = 0xFF000000,
                        accentColorArgb = 0xFFD71921,
                        textColorArgb = 0xFFF5F5F5,
                        is24Hour = true,
                        showSeconds = true,
                        timeScale = 1.0f,
                        topComplication = "DATE",
                        bottomComplication = "NEXT_EVENT",
                        leftComplication = "NONE",
                        rightComplication = "NONE",
                        updatedAtMillis = 1_763_690_000_000L,
                    ),
                )
                assertTrue(insertedId > 0L)

                val projects = roomDb.watchFaceDao().observeAllProjects().first()
                assertEquals(1, projects.size)
                assertEquals("Migrated Watch Face", projects.first().name)
            } finally {
                roomDb.close()
            }
        } finally {
            context.deleteDatabase(dbName)
        }
    }
}
