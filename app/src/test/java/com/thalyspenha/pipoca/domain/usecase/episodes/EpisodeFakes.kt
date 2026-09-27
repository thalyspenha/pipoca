package com.thalyspenha.pipoca.domain.usecase.episodes

import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.TvShowDetails
import com.thalyspenha.pipoca.domain.repository.SeasonRepository
import com.thalyspenha.pipoca.domain.repository.TvShowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Série em memória; `onRefresh` simula a rede. */
class FakeTvShowRepository : TvShowRepository {
    val details = MutableStateFlow<TvShowDetails?>(null)
    var onRefresh: suspend () -> DataResult<Unit> = { DataResult.Success(Unit) }

    override fun observeTvShowDetails(id: Long): Flow<TvShowDetails?> = details
    override suspend fun refreshTvShowDetails(id: Long, force: Boolean): DataResult<Unit> = onRefresh()
}

/** Episódios em memória; registra temporadas pedidas e pode falhar numa temporada. */
class FakeSeasonRepository : SeasonRepository {
    val episodes = MutableStateFlow<List<Episode>>(emptyList())
    val refreshed = mutableListOf<Int>()
    var failOn: Int? = null

    override fun observeSeasonEpisodes(showId: Long, seasonNumber: Int): Flow<List<Episode>> =
        episodes.map { all -> all.filter { it.seasonNumber == seasonNumber } }

    override fun observeShowEpisodes(showId: Long): Flow<List<Episode>> = episodes

    override suspend fun refreshSeason(showId: Long, seasonNumber: Int, force: Boolean): DataResult<Unit> {
        if (seasonNumber == failOn) return DataResult.Failure(DataError.Network)
        refreshed += seasonNumber
        return DataResult.Success(Unit)
    }
}
