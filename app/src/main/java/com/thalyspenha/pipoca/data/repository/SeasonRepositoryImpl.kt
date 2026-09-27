package com.thalyspenha.pipoca.data.repository

import com.thalyspenha.pipoca.data.cache.CachePolicy
import com.thalyspenha.pipoca.data.local.dao.TmdbCacheDao
import com.thalyspenha.pipoca.data.local.dao.TmdbEpisodeDao
import com.thalyspenha.pipoca.data.mapper.toCacheBundle
import com.thalyspenha.pipoca.data.mapper.toDomain
import com.thalyspenha.pipoca.data.remote.TmdbRemoteDataSource
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.SeasonSummary
import com.thalyspenha.pipoca.domain.repository.SeasonRepository
import com.thalyspenha.pipoca.domain.repository.TvShowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/**
 * Episódios com a mesma validade da série (1 dia no ar, 30 dias finalizada).
 * A temporada tem FK para a série: sem a série em cache, busca os detalhes dela antes.
 */
class SeasonRepositoryImpl @Inject constructor(
    private val remote: TmdbRemoteDataSource,
    private val cacheDao: TmdbCacheDao,
    private val episodeDao: TmdbEpisodeDao,
    private val tvShowRepository: TvShowRepository,
    private val clock: Clock,
) : SeasonRepository {

    override fun observeSeasonEpisodes(showId: Long, seasonNumber: Int): Flow<List<Episode>> =
        episodeDao.observeSeasonEpisodes(showId, seasonNumber).map { list -> list.map { it.toDomain() } }

    override fun observeShowEpisodes(showId: Long): Flow<List<Episode>> =
        episodeDao.observeShowEpisodes(showId).map { list -> list.map { it.toDomain() } }

    override fun observeWatchingShowsEpisodes(): Flow<List<Episode>> =
        episodeDao.observeWatchingShowsEpisodes().map { list -> list.map { it.toDomain() } }

    override fun observeWatchingShowsSeasons(): Flow<Map<Long, List<SeasonSummary>>> =
        episodeDao.observeWatchingShowsSeasons().map { list ->
            list.groupBy({ it.showId }, { it.toDomain() })
        }

    override fun observeLibraryShowsEpisodes(): Flow<List<Episode>> =
        episodeDao.observeLibraryShowsEpisodes().map { list -> list.map { it.toDomain() } }

    override fun observeLibraryShowsSeasons(): Flow<Map<Long, List<SeasonSummary>>> =
        episodeDao.observeLibraryShowsSeasons().map { list -> list.groupBy({ it.showId }, { it.toDomain() }) }

    override suspend fun refreshSeason(showId: Long, seasonNumber: Int, force: Boolean): DataResult<Unit> {
        val now = clock.instant()
        var info = cacheDao.getTvShowCacheInfo(showId)
        if (info == null) {
            val showResult = tvShowRepository.refreshTvShowDetails(showId)
            if (showResult is DataResult.Failure) return showResult
            info = cacheDao.getTvShowCacheInfo(showId)
        }
        if (!force) {
            val fetchedAt = episodeDao.getSeasonEpisodesFetchedAt(showId, seasonNumber)
            if (fetchedAt != null && CachePolicy.isFresh(fetchedAt, CachePolicy.tvShowTtl(info?.tmdbStatus), now)) {
                return DataResult.Success(Unit)
            }
        }
        return when (val result = remote.getSeasonDetails(showId, seasonNumber)) {
            is DataResult.Success -> {
                val bundle = result.data.toCacheBundle(showId, fetchedAt = now.toEpochMilli())
                // O resumo vindo de tv/{id} manda: o endpoint da temporada pode trazer pôster nulo
                // no idioma pedido. Só usa o da temporada se a série ainda não tiver essa temporada.
                val season = cacheDao.getSeasons(showId).firstOrNull { it.id == bundle.season.id } ?: bundle.season
                episodeDao.saveSeason(season, bundle.episodes)
                DataResult.Success(Unit)
            }
            is DataResult.Failure -> result
        }
    }
}
