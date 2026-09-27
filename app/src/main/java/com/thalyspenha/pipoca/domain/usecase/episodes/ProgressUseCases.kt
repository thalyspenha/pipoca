package com.thalyspenha.pipoca.domain.usecase.episodes

import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.progress.SPECIALS_SEASON
import com.thalyspenha.pipoca.domain.progress.ShowProgress
import com.thalyspenha.pipoca.domain.progress.ShowProgressCalculator
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import com.thalyspenha.pipoca.domain.repository.SeasonRepository
import com.thalyspenha.pipoca.domain.repository.TvShowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Progresso da série a partir do cache (série + episódios) e dos episódios assistidos.
 * Emite `null` enquanto a série não está em cache. "Hoje" vem do [Clock] a cada emissão.
 */
class ObserveShowProgressUseCase @Inject constructor(
    private val tvShowRepository: TvShowRepository,
    private val seasonRepository: SeasonRepository,
    private val library: LibraryRepository,
    private val clock: Clock,
) {
    operator fun invoke(showId: Long): Flow<ShowProgress?> = combine(
        tvShowRepository.observeTvShowDetails(showId),
        seasonRepository.observeShowEpisodes(showId),
        library.observeWatchedEpisodes(showId),
    ) { show, episodes, watched ->
        show?.let {
            ShowProgressCalculator.calculate(
                episodes = episodes,
                seasons = it.seasons,
                watchedEpisodeIds = watched.map { w -> w.episodeId }.toSet(),
                tmdbStatus = it.tmdbStatus,
                today = LocalDate.now(clock),
            )
        }
    }

    /** Valor atual, para decisões após gravar (status automático). */
    suspend fun current(showId: Long): ShowProgress? = invoke(showId).first()
}

/**
 * Garante série e todas as temporadas regulares no cache (base do progresso completo).
 * Para na primeira falha; temporadas já baixadas e válidas não vão à rede.
 */
class RefreshShowEpisodesUseCase @Inject constructor(
    private val tvShowRepository: TvShowRepository,
    private val seasonRepository: SeasonRepository,
) {
    suspend operator fun invoke(showId: Long, force: Boolean = false): DataResult<Unit> {
        val showResult = tvShowRepository.refreshTvShowDetails(showId, force)
        if (showResult is DataResult.Failure) return showResult
        val seasons = tvShowRepository.observeTvShowDetails(showId).first()?.seasons.orEmpty()
            .filter { it.seasonNumber != SPECIALS_SEASON && it.episodeCount > 0 }
        for (season in seasons) {
            val result = seasonRepository.refreshSeason(showId, season.seasonNumber, force)
            if (result is DataResult.Failure) return result
        }
        return DataResult.Success(Unit)
    }
}
