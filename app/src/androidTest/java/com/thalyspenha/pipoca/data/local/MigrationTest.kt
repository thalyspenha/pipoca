package com.thalyspenha.pipoca.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thalyspenha.pipoca.data.local.migration.MIGRATION_1_2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Migrações preservam o cache existente e produzem o schema exportado (validado pelo helper). */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath(DB_NAME),
        driver = AndroidSQLiteDriver(),
        databaseClass = AppDatabase::class,
    )

    @Test
    fun migra1Para2MantendoCache() {
        helper.createDatabase(1).use { connection ->
            connection.execSQL("INSERT INTO tmdb_genre (id, name) VALUES (28, 'Ação')")
        }

        helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2)).use { connection ->
            connection.prepare("SELECT name FROM tmdb_genre WHERE id = 28").use { statement ->
                assertTrue(statement.step())
                assertEquals("Ação", statement.getText(0))
            }
            connection.execSQL(
                "INSERT INTO user_movie (movie_id, status, is_favorite, added_at, updated_at) " +
                    "VALUES (603, 'WATCHED', 1, 1, 1)",
            )
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
