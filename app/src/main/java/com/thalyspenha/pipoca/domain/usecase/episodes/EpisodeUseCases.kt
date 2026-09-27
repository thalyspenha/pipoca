package com.thalyspenha.pipoca.domain.usecase.episodes

import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.progress.ShowProgressCalculator
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import com.thalyspenha.pipoca.domain.repository.SeasonRepository
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowStatusUseCase
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Status automático da série depois de marcar/desmarcar episódios (D-035):
 * - marcou: fora da biblioteca ou `WANT_TO_WATCH` vira `WATCHING`; se o progresso ficou concluído
 *   (tudo exibido assistido e série finalizada), vira `COMPLETED`;
 * - desmarcou: `COMPLETED` que deixou de estar concluída volta a `WATCHING`.
 * `PAUSED`/`DROPPED` escolhidos pelo usuário só mudam se a série ficar concluída.
 */
class SyncShowStatusUseCase @Inject constructor(
    private val library: LibraryRepository,
    private val observeProgress: ObserveShowProgressUseCase,
    private val setStatus: SetTvShowStatusUseCase,
) {
    suspend fun afterMark(showId: Long) {
        val status = library.getTvShow(showId)?.status
        if (observeProgress.current(showId)?.isCompleted == true) {
            setStatus(showId, TvShowStatus.COMPLETED)
        } else if (status == null || status == TvShowStatus.WANT_TO_WATCH) {
            setStatus(showId, TvShowStatus.WATCHING)
        }
    }

    suspend fun afterUnmark(showId: Long) {
        val status = library.getTvShow(showId)?.status ?: return
        if (status == TvShowStatus.COMPLETED && observeProgress.current(showId)?.isCompleted != true) {
            setStatus(showId, TvShowStatus.WATCHING)
        }
    }
}

/** Marca um episódio já exibido. Episódio futuro ou sem data é ignorado (não dá para ter assistido). */
class MarkEpisodeWatchedUseCase @Inject constructor(
    private val library: LibraryRepository,
    private val syncStatus: SyncShowStatusUseCase,
    private val clock: Clock,
) {
    suspend operator fun invoke(episode: Episode) {
        if (!episode.isAired(LocalDate.now(clock))) return
        if (library.observeWatchedEpisodes(episode.showId).first().any { it.episodeId == episode.id }) return
        library.markEpisodesWatched(listOf(episode), watchedAt = clock.instant())
        syncStatus.afterMark(episode.showId)
    }
}

class UnmarkEpisodeUseCase @Inject constructor(
    private val library: LibraryRepository,
    private val syncStatus: SyncShowStatusUseCase,
) {
    suspend operator fun invoke(episode: Episode) {
        library.unmarkEpisodes(listOf(episode.id))
        syncStatus.afterUnmark(episode.showId)
    }
}

/**
 * Marca todos os episódios já exibidos da temporada que ainda não estavam marcados (mantém a data
 * dos que já estavam). Vale também para a temporada 0, que não entra no progresso.
 */
class MarkSeasonWatchedUseCase @Inject constructor(
    private val library: LibraryRepository,
    private val seasonRepository: SeasonRepository,
    private val syncStatus: SyncShowStatusUseCase,
    private val clock: Clock,
) {
    suspend operator fun invoke(showId: Long, seasonNumber: Int) {
        val today = LocalDate.now(clock)
        val watched = library.observeWatchedEpisodes(showId).first().map { it.episodeId }.toSet()
        val toMark = seasonRepository.observeSeasonEpisodes(showId, seasonNumber).first()
            .filter { it.isAired(today) && it.id !in watched }
        if (toMark.isEmpty()) return
        library.markEpisodesWatched(toMark, watchedAt = clock.instant())
        syncStatus.afterMark(showId)
    }
}

/** Desmarca todos os episódios assistidos da temporada (inclusive os que não estão mais no cache). */
class UnmarkSeasonUseCase @Inject constructor(
    private val library: LibraryRepository,
    private val syncStatus: SyncShowStatusUseCase,
) {
    suspend operator fun invoke(showId: Long, seasonNumber: Int) {
        val ids = library.observeWatchedEpisodes(showId).first()
            .filter { it.seasonNumber == seasonNumber }
            .map { it.episodeId }
        if (ids.isEmpty()) return
        library.unmarkEpisodes(ids)
        syncStatus.afterUnmark(showId)
    }
}

/** Especiais também podem ser marcados; só precisam já ter sido exibidos. */
private fun Episode.isAired(today: LocalDate): Boolean = ShowProgressCalculator.isAired(this, today)
