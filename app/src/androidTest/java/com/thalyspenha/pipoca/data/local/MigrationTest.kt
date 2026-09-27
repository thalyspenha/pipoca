package com.thalyspenha.pipoca.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thalyspenha.pipoca.data.local.migration.MIGRATION_1_2
import com.thalyspenha.pipoca.data.local.migration.MIGRATION_2_3
import com.thalyspenha.pipoca.data.local.migration.MIGRATION_3_4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
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

    /** Cada teste cria o banco do zero; sem isso o arquivo do teste anterior (já na versão 3) sobra. */
    @Before
    fun deleteLeftoverDatabase() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(DB_NAME)
    }

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

    @Test
    fun migra2Para3MantendoBibliotecaEHistorico() {
        helper.createDatabase(2).use { connection ->
            connection.execSQL(
                "INSERT INTO user_tv_show (show_id, status, is_favorite, added_at, updated_at) " +
                    "VALUES (1396, 'WATCHING', 1, 1, 2)",
            )
            connection.execSQL("INSERT INTO watch_history (media_type, movie_id, watched_at) VALUES ('MOVIE', 603, 100)")
        }

        helper.runMigrationsAndValidate(3, listOf(MIGRATION_2_3)).use { connection ->
            connection.prepare("SELECT status, is_favorite FROM user_tv_show WHERE show_id = 1396").use { statement ->
                assertTrue(statement.step())
                assertEquals("WATCHING", statement.getText(0))
                assertEquals(1L, statement.getLong(1))
            }
            connection.prepare("SELECT COUNT(*) FROM watch_history").use { statement ->
                assertTrue(statement.step())
                assertEquals(1L, statement.getLong(0))
            }
            connection.execSQL(
                "INSERT INTO user_episode (episode_id, show_id, season_number, episode_number, watched_at) " +
                    "VALUES (62085, 1396, 1, 1, 100)",
            )
        }
    }

    @Test
    fun migra3Para4MantendoEpisodiosAssistidos() {
        helper.createDatabase(3).use { connection ->
            connection.execSQL(
                "INSERT INTO user_episode (episode_id, show_id, season_number, episode_number, watched_at) " +
                    "VALUES (62085, 1396, 1, 1, 100)",
            )
        }

        helper.runMigrationsAndValidate(4, listOf(MIGRATION_3_4)).use { connection ->
            connection.prepare("SELECT COUNT(*) FROM user_episode").use { statement ->
                assertTrue(statement.step())
                assertEquals(1L, statement.getLong(0))
            }
            connection.execSQL(
                "INSERT INTO collection_item (media_type, tmdb_id, format, quantity, added_at, updated_at) " +
                    "VALUES ('MOVIE', 603, 'UHD_4K_BLURAY', 1, 1, 1)",
            )
        }
    }

    @Test
    fun migra1Para4EmSequencia() {
        helper.createDatabase(1).close()

        helper.runMigrationsAndValidate(4, listOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)).close()
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
