package com.thalyspenha.pipoca.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.thalyspenha.pipoca.data.local.converter.Converters
import com.thalyspenha.pipoca.data.local.dao.TmdbCacheDao
import com.thalyspenha.pipoca.data.local.dao.TmdbEpisodeDao
import com.thalyspenha.pipoca.data.local.dao.UserLibraryDao
import com.thalyspenha.pipoca.data.local.entity.TmdbCreditEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbEpisodeEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbGenreEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbMovieEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbMovieGenreCrossRef
import com.thalyspenha.pipoca.data.local.entity.TmdbPersonEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbSeasonEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbTvShowEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbTvShowGenreCrossRef
import com.thalyspenha.pipoca.data.local.entity.UserEpisodeEntity
import com.thalyspenha.pipoca.data.local.entity.UserMovieEntity
import com.thalyspenha.pipoca.data.local.entity.UserTvShowEntity
import com.thalyspenha.pipoca.data.local.entity.WatchHistoryEntity

@Database(
    entities = [
        TmdbGenreEntity::class,
        TmdbMovieEntity::class,
        TmdbMovieGenreCrossRef::class,
        TmdbTvShowEntity::class,
        TmdbTvShowGenreCrossRef::class,
        TmdbSeasonEntity::class,
        TmdbPersonEntity::class,
        TmdbCreditEntity::class,
        UserMovieEntity::class,
        UserTvShowEntity::class,
        WatchHistoryEntity::class,
        TmdbEpisodeEntity::class,
        UserEpisodeEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tmdbCacheDao(): TmdbCacheDao

    abstract fun userLibraryDao(): UserLibraryDao

    abstract fun tmdbEpisodeDao(): TmdbEpisodeDao

    companion object {
        const val NAME = "pipoca.db"
    }
}
