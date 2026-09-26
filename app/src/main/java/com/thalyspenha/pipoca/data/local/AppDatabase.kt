package com.thalyspenha.pipoca.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.thalyspenha.pipoca.data.local.converter.Converters
import com.thalyspenha.pipoca.data.local.entity.TmdbGenreEntity

@Database(
    entities = [TmdbGenreEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        const val NAME = "pipoca.db"
    }
}
