package com.thalyspenha.pipoca.data.repository

import com.thalyspenha.pipoca.data.cache.CachePolicy
import com.thalyspenha.pipoca.data.local.dao.TmdbCacheDao
import com.thalyspenha.pipoca.data.local.entity.CreditMediaType
import com.thalyspenha.pipoca.data.mapper.toCacheBundle
import com.thalyspenha.pipoca.data.mapper.toDomain
import com.thalyspenha.pipoca.data.remote.TmdbRemoteDataSource
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.MovieDetails
import com.thalyspenha.pipoca.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

class MovieRepositoryImpl @Inject constructor(
    private val remote: TmdbRemoteDataSource,
    private val dao: TmdbCacheDao,
    private val clock: Clock,
) : MovieRepository {

    override fun observeMovieDetails(id: Long): Flow<MovieDetails?> =
        dao.observeMovie(id).map { entity ->
            entity?.toDomain(
                genres = dao.getMovieGenres(id),
                cast = dao.getCast(CreditMediaType.MOVIE, id),
            )
        }

    override suspend fun refreshMovieDetails(id: Long, force: Boolean): DataResult<Unit> {
        val now = clock.instant()
        if (!force) {
            val fetchedAt = dao.getMovieFetchedAt(id)
            if (fetchedAt != null && CachePolicy.isFresh(fetchedAt, CachePolicy.MOVIE, now)) {
                return DataResult.Success(Unit)
            }
        }
        return when (val result = remote.getMovieDetails(id)) {
            is DataResult.Success -> {
                dao.saveMovie(result.data.toCacheBundle(fetchedAt = now.toEpochMilli()))
                DataResult.Success(Unit)
            }
            is DataResult.Failure -> result
        }
    }
}
