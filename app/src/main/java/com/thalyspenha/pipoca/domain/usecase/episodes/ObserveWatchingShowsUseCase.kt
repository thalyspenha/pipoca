package com.thalyspenha.pipoca.domain.usecase.episodes

import com.thalyspenha.pipoca.domain.model.LibraryTvShowItem
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.progress.ShowProgress
import com.thalyspenha.pipoca.domain.progress.ShowProgressCalculator
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import com.thalyspenha.pipoca.domain.repository.SeasonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** Série `WATCHING` com progresso calculado e o momento do último episódio assistido. */
data class WatchingShow(
    val item: LibraryTvShowItem,
    val progress: ShowProgress,
    val lastWatchedAt: Instant?,
)

/**
 * Progresso de todas as séries `WATCHING` a partir de quatro consultas fixas (séries, episódios,
 * temporadas e assistidos), sem consulta por série (D-044). Ordem: última vista primeiro; nunca
 * vistas por fim, pela atualização mais recente.
 */
class ObserveWatchingShowsUseCase @Inject constructor(
    private val library: LibraryRepository,
    private val seasonRepository: SeasonRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<WatchingShow>> = combine(
        library.observeTvShowItems(),
        seasonRepository.observeWatchingShowsEpisodes(),
        seasonRepository.observeWatchingShowsSeasons(),
        library.observeWatchingShowsWatchedEpisodes(),
    ) { shows, episodes, seasons, watched ->
        val today = LocalDate.now(clock)
        val episodesByShow = episodes.groupBy { it.showId }
        val watchedByShow = watched.groupBy { it.showId }
        shows
            .filter { it.show.status == TvShowStatus.WATCHING }
            .map { item ->
                val showWatched = watchedByShow[item.show.showId].orEmpty()
                WatchingShow(
                    item = item,
                    progress = ShowProgressCalculator.calculate(
                        episodes = episodesByShow[item.show.showId].orEmpty(),
                        seasons = seasons[item.show.showId].orEmpty(),
                        watchedEpisodeIds = showWatched.map { it.episodeId }.toSet(),
                        tmdbStatus = item.tmdbStatus,
                        today = today,
                    ),
                    lastWatchedAt = showWatched.maxOfOrNull { it.watchedAt },
                )
            }
            .sortedWith(
                compareBy<WatchingShow> { it.lastWatchedAt == null }
                    .thenByDescending { it.lastWatchedAt }
                    .thenByDescending { it.item.show.updatedAt },
            )
    }
}
