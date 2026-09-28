package com.thalyspenha.pipoca.data.repository

import android.content.Context
import coil3.ImageLoader
import com.thalyspenha.pipoca.data.local.AppDatabase
import com.thalyspenha.pipoca.data.local.dao.MaintenanceDao
import com.thalyspenha.pipoca.domain.model.CacheClearResult
import com.thalyspenha.pipoca.domain.model.DatabaseInfo
import com.thalyspenha.pipoca.domain.repository.MaintenanceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MaintenanceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: MaintenanceDao,
    private val imageLoader: ImageLoader,
) : MaintenanceRepository {

    override fun observeDatabaseInfo(): Flow<DatabaseInfo> = dao.observeCounts()
        .map { counts ->
            DatabaseInfo(
                schemaVersion = AppDatabase.VERSION,
                movies = counts.movies,
                tvShows = counts.tvShows,
                watchedEpisodes = counts.watchedEpisodes,
                collectionItems = counts.collectionItems,
                historyEntries = counts.historyEntries,
                cachedMovies = counts.cachedMovies,
                cachedTvShows = counts.cachedTvShows,
                cachedEpisodes = counts.cachedEpisodes,
                fileSizeBytes = databaseFileSize(),
            )
        }
        .flowOn(Dispatchers.IO)

    override suspend fun clearCache(): CacheClearResult {
        val (movies, shows) = dao.clearUnusedCache()
        withContext(Dispatchers.IO) {
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
        }
        return CacheClearResult(movies = movies, tvShows = shows)
    }

    private fun databaseFileSize(): Long {
        val file = context.getDatabasePath(AppDatabase.NAME)
        return listOf(file, file.resolveSibling("${file.name}-wal"))
            .sumOf { if (it.exists()) it.length() else 0L }
    }
}
