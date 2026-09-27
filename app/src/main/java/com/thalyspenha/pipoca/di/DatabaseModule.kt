package com.thalyspenha.pipoca.di

import android.content.Context
import androidx.room.Room
import com.thalyspenha.pipoca.data.local.AppDatabase
import com.thalyspenha.pipoca.data.local.dao.TmdbCacheDao
import com.thalyspenha.pipoca.data.local.dao.UserLibraryDao
import com.thalyspenha.pipoca.data.local.migration.ALL_MIGRATIONS
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    @Provides
    fun provideTmdbCacheDao(database: AppDatabase): TmdbCacheDao = database.tmdbCacheDao()

    @Provides
    fun provideUserLibraryDao(database: AppDatabase): UserLibraryDao = database.userLibraryDao()
}
