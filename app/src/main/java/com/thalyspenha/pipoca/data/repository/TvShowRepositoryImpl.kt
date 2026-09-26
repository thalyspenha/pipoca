package com.thalyspenha.pipoca.data.repository

import com.thalyspenha.pipoca.data.cache.CachePolicy
import com.thalyspenha.pipoca.data.local.dao.TmdbCacheDao
import com.thalyspenha.pipoca.data.local.entity.CreditMediaType
import com.thalyspenha.pipoca.data.mapper.toCacheBundle
import com.thalyspenha.pipoca.data.mapper.toDomain
import com.thalyspenha.pipoca.data.remote.TmdbRemoteDataSource
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.TvShowDetails
import com.thalyspenha.pipoca.domain.repository.TvShowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

class TvShowRepositoryImpl @Inject constructor(
    private val remote: TmdbRemoteDataSource,
    private val dao: TmdbCacheDao,
    private val clock: Clock,
) : TvShowRepository {

    override fun observeTvShowDetails(id: Long): Flow<TvShowDetails?> =
        dao.observeTvShow(id).map { entity ->
            entity?.toDomain(
                genres = dao.getTvShowGenres(id),
                seasons = dao.getSeasons(id),
                cast = dao.getCast(CreditMediaType.TV, id),
            )
        }

    override suspend fun refreshTvShowDetails(id: Long, force: Boolean): DataResult<Unit> {
        val now = clock.instant()
        if (!force) {
            val info = dao.getTvShowCacheInfo(id)
            if (info != null && CachePolicy.isFresh(info.fetchedAt, CachePolicy.tvShowTtl(info.tmdbStatus), now)) {
                return DataResult.Success(Unit)
            }
        }
        return when (val result = remote.getTvShowDetails(id)) {
            is DataResult.Success -> {
                dao.saveTvShow(result.data.toCacheBundle(fetchedAt = now.toEpochMilli()))
                DataResult.Success(Unit)
            }
            is DataResult.Failure -> result
        }
    }
}
