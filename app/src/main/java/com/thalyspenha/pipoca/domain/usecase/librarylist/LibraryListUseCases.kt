package com.thalyspenha.pipoca.domain.usecase.librarylist

import com.thalyspenha.pipoca.domain.model.LibraryMovieItem
import com.thalyspenha.pipoca.domain.model.LibrarySort
import com.thalyspenha.pipoca.domain.model.LibraryTvShowItem
import com.thalyspenha.pipoca.domain.model.MovieLibraryCounts
import com.thalyspenha.pipoca.domain.model.MovieLibraryFilter
import com.thalyspenha.pipoca.domain.model.TvShowLibraryCounts
import com.thalyspenha.pipoca.domain.model.TvShowLibraryFilter
import com.thalyspenha.pipoca.domain.model.matchesSearch
import com.thalyspenha.pipoca.domain.progress.ShowProgress
import com.thalyspenha.pipoca.domain.progress.ShowProgressCalculator
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import com.thalyspenha.pipoca.domain.repository.SeasonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Série da biblioteca com progresso (nulo sem episódios em cache e nada assistido). */
data class LibraryTvShowEntry(val item: LibraryTvShowItem, val progress: ShowProgress?)

/** Filmes da Biblioteca: filtro/ordem no banco, pesquisa local em memória ignorando acento (D-050). */
class ObserveLibraryMoviesUseCase @Inject constructor(
    private val library: LibraryRepository,
) {
    operator fun invoke(filter: MovieLibraryFilter, sort: LibrarySort, query: String): Flow<List<LibraryMovieItem>> =
        library.observeMovieList(filter, sort).map { list -> list.filter { matchesSearch(it.title, query) } }
}

/**
 * Séries da Biblioteca com progresso. Progresso de todas as séries sai de três consultas fixas
 * (episódios, temporadas, assistidos da biblioteca), sem consulta por série. Ordenar por progresso
 * é feito aqui (depende do cálculo de episódios exibidos): maior percentual primeiro, sem
 * progresso no fim.
 */
class ObserveLibraryTvShowsUseCase @Inject constructor(
    private val library: LibraryRepository,
    private val seasonRepository: SeasonRepository,
    private val clock: Clock,
) {
    operator fun invoke(filter: TvShowLibraryFilter, sort: LibrarySort, query: String): Flow<List<LibraryTvShowEntry>> =
        combine(
            library.observeTvShowList(filter, sort),
            seasonRepository.observeLibraryShowsEpisodes(),
            seasonRepository.observeLibraryShowsSeasons(),
            library.observeLibraryShowsWatchedEpisodes(),
        ) { shows, episodes, seasons, watched ->
            val today = LocalDate.now(clock)
            val episodesByShow = episodes.groupBy { it.showId }
            val watchedByShow = watched.groupBy({ it.showId }, { it.episodeId })
            val entries = shows
                .filter { matchesSearch(it.name, query) }
                .map { item ->
                    val showEpisodes = episodesByShow[item.show.showId].orEmpty()
                    val showWatched = watchedByShow[item.show.showId].orEmpty().toSet()
                    val progress = if (showEpisodes.isEmpty() && showWatched.isEmpty()) {
                        null
                    } else {
                        ShowProgressCalculator.calculate(
                            episodes = showEpisodes,
                            seasons = seasons[item.show.showId].orEmpty(),
                            watchedEpisodeIds = showWatched,
                            tmdbStatus = item.tmdbStatus,
                            today = today,
                        )
                    }
                    LibraryTvShowEntry(item, progress)
                }
            if (sort == LibrarySort.PROGRESS) {
                entries.sortedWith(
                    compareBy<LibraryTvShowEntry> { it.progress == null }
                        .thenByDescending { it.progress?.percent }
                        .thenByDescending { it.progress?.watched },
                )
            } else {
                entries
            }
        }
}

class ObserveMovieLibraryCountsUseCase @Inject constructor(
    private val library: LibraryRepository,
) {
    operator fun invoke(): Flow<MovieLibraryCounts> = library.observeMovieLibraryCounts()
}

class ObserveTvShowLibraryCountsUseCase @Inject constructor(
    private val library: LibraryRepository,
) {
    operator fun invoke(): Flow<TvShowLibraryCounts> = library.observeTvShowLibraryCounts()
}
