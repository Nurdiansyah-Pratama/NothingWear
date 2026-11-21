package com.nothingwear.app.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * JVM integrity test verifying exported Room schema snapshots (v1 & v2) and MIGRATION_1_2 contract.
 */
class RoomSchemaIntegrityTest {

    @Test
    fun exportedSchemasV1AndV2ExistAndMatchMigration1To2() {
        val schemaDir = File("schemas/com.nothingwear.app.core.database.NothingWearDatabase")
        val v1File = File(schemaDir, "1.json")
        val v2File = File(schemaDir, "2.json")

        assertTrue("Room schema 1.json must be exported and committed", v1File.exists())
        assertTrue("Room schema 2.json must be exported and committed", v2File.exists())

        val v1Json = v1File.readText()
        val v2Json = v2File.readText()

        assertTrue(v1Json.contains("\"tableName\": \"calendar_accounts\""))
        assertTrue(v1Json.contains("\"tableName\": \"calendar_events\""))
        assertTrue(v2Json.contains("\"tableName\": \"calendar_accounts\""))
        assertTrue(v2Json.contains("\"tableName\": \"calendar_events\""))
        assertTrue(v2Json.contains("\"tableName\": \"watch_face_projects\""))

        assertEquals(1, DatabaseModule.MIGRATION_1_2.startVersion)
        assertEquals(2, DatabaseModule.MIGRATION_1_2.endVersion)
    }
}
