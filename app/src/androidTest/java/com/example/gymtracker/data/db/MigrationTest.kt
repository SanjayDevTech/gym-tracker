package com.example.gymtracker.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Room v4 → v5: `sessions.started_at_millis`, nullable, existing rows NULL. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate4To5_keepsSessionsAndAddsNullStartTime() {
        helper.createDatabase(dbName, 4).use { db ->
            db.execSQL(
                "INSERT INTO sessions (id, client_uuid, plan_id, date_epoch_day, logged_at_millis, notes) " +
                    "VALUES (1, 'uuid-1', NULL, 20000, 123456, 'old note')"
            )
        }

        helper.runMigrationsAndValidate(dbName, 5, true, AppDatabase.MIGRATION_4_5).use { db ->
            db.query("SELECT client_uuid, notes, started_at_millis FROM sessions WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("uuid-1", c.getString(0))
                assertEquals("old note", c.getString(1))
                assertTrue(c.isNull(2))
            }
            db.execSQL(
                "INSERT INTO sessions (id, client_uuid, date_epoch_day, logged_at_millis, notes, started_at_millis) " +
                    "VALUES (2, 'uuid-2', 20001, 2000, '', 1000)"
            )
            db.query("SELECT started_at_millis FROM sessions WHERE id = 2").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1000L, c.getLong(0))
            }
        }
    }
}
