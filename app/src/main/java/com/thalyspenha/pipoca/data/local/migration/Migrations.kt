package com.thalyspenha.pipoca.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Migrações explícitas (DATABASE.md, D-028). SQL copiado do schema exportado (`app/schemas/`).
 * Nunca usar `fallbackToDestructiveMigration`: apagaria os dados pessoais.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `user_movie` (`movie_id` INTEGER NOT NULL, `status` TEXT NOT NULL, " +
                "`is_favorite` INTEGER NOT NULL, `rating` INTEGER, `notes` TEXT, `added_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL, PRIMARY KEY(`movie_id`))",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `user_tv_show` (`show_id` INTEGER NOT NULL, `status` TEXT NOT NULL, " +
                "`is_favorite` INTEGER NOT NULL, `rating` INTEGER, `notes` TEXT, `added_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL, PRIMARY KEY(`show_id`))",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `watch_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`media_type` TEXT NOT NULL, `movie_id` INTEGER, `show_id` INTEGER, `episode_id` INTEGER, " +
                "`watched_at` INTEGER NOT NULL)",
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_history_watched_at` ON `watch_history` (`watched_at`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_history_movie_id` ON `watch_history` (`movie_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_history_show_id` ON `watch_history` (`show_id`)")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
