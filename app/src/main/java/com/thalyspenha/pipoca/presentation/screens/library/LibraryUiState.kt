package com.thalyspenha.pipoca.presentation.screens.library

import com.thalyspenha.pipoca.domain.model.LibraryMovieItem
import com.thalyspenha.pipoca.domain.model.LibrarySort
import com.thalyspenha.pipoca.domain.model.MovieLibraryCounts
import com.thalyspenha.pipoca.domain.model.MovieLibraryFilter
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.model.TvShowLibraryCounts
import com.thalyspenha.pipoca.domain.model.TvShowLibraryFilter
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.repository.LibraryViewMode
import com.thalyspenha.pipoca.domain.usecase.librarylist.LibraryTvShowEntry
import com.thalyspenha.pipoca.presentation.components.MediaCardData
import com.thalyspenha.pipoca.presentation.components.MediaCardProgress
import com.thalyspenha.pipoca.presentation.components.MediaStatusBadge

enum class LibraryTab { MOVIES, TV_SHOWS }

/** Filtro e ordenação de cada aba ficam separados: trocar de aba não perde a escolha. */
data class LibrarySelection(
    val tab: LibraryTab = LibraryTab.MOVIES,
    val movieFilter: MovieLibraryFilter = MovieLibraryFilter.ALL,
    val tvShowFilter: TvShowLibraryFilter = TvShowLibraryFilter.ALL,
    val movieSort: LibrarySort = LibrarySort.RECENTLY_ADDED,
    val tvShowSort: LibrarySort = LibrarySort.RECENTLY_ADDED,
    val query: String = "",
) {
    val sort: LibrarySort get() = if (tab == LibraryTab.MOVIES) movieSort else tvShowSort
}

data class LibraryUiState(
    val selection: LibrarySelection = LibrarySelection(),
    val viewMode: LibraryViewMode = LibraryViewMode.GRID,
    val movieCounts: MovieLibraryCounts = MovieLibraryCounts(0, 0, 0, 0),
    val tvShowCounts: TvShowLibraryCounts = TvShowLibraryCounts(0, 0, 0, 0, 0, 0, 0),
    val items: List<MediaCardData> = emptyList(),
    val isLoading: Boolean = true,
) {
    /** Nada na biblioteca (nem filmes nem séries): estado vazio com atalho para a Busca. */
    val isLibraryEmpty: Boolean get() = !isLoading && movieCounts.all == 0 && tvShowCounts.all == 0

    /** Ordenações da aba atual (progresso e último episódio só para séries). */
    val sortOptions: List<LibrarySort>
        get() = if (selection.tab == LibraryTab.MOVIES) LibrarySort.entries.filter { it.forMovies } else LibrarySort.entries

    /** Mensagem quando a lista filtrada está vazia (a biblioteca não). */
    val emptyMessage: String
        get() = when {
            selection.query.isNotBlank() -> "Nenhum título encontrado para \"${selection.query.trim()}\"."
            selection.tab == LibraryTab.MOVIES -> when (selection.movieFilter) {
                MovieLibraryFilter.ALL -> "Você ainda não adicionou filmes."
                MovieLibraryFilter.WANT_TO_WATCH -> "Você ainda não adicionou títulos para assistir."
                MovieLibraryFilter.WATCHED -> "Você ainda não marcou nenhum título como assistido."
                MovieLibraryFilter.FAVORITES -> "Nenhum filme favorito."
            }
            else -> when (selection.tvShowFilter) {
                TvShowLibraryFilter.ALL -> "Você ainda não adicionou séries."
                TvShowLibraryFilter.WANT_TO_WATCH -> "Você ainda não adicionou títulos para assistir."
                TvShowLibraryFilter.WATCHING -> "Nenhuma série em andamento."
                TvShowLibraryFilter.COMPLETED -> "Nenhuma série concluída."
                TvShowLibraryFilter.PAUSED -> "Nenhuma série pausada."
                TvShowLibraryFilter.DROPPED -> "Nenhuma série abandonada."
                TvShowLibraryFilter.FAVORITES -> "Nenhuma série favorita."
            }
        }
}

val MovieLibraryFilter.label: String
    get() = when (this) {
        MovieLibraryFilter.ALL -> "Todos"
        MovieLibraryFilter.WANT_TO_WATCH -> "Quero assistir"
        MovieLibraryFilter.WATCHED -> "Assistidos"
        MovieLibraryFilter.FAVORITES -> "Favoritos"
    }

val TvShowLibraryFilter.label: String
    get() = when (this) {
        TvShowLibraryFilter.ALL -> "Todos"
        TvShowLibraryFilter.WANT_TO_WATCH -> "Quero assistir"
        TvShowLibraryFilter.WATCHING -> "Assistindo"
        TvShowLibraryFilter.COMPLETED -> "Concluídas"
        TvShowLibraryFilter.PAUSED -> "Pausadas"
        TvShowLibraryFilter.DROPPED -> "Abandonadas"
        TvShowLibraryFilter.FAVORITES -> "Favoritos"
    }

val LibrarySort.label: String
    get() = when (this) {
        LibrarySort.RECENTLY_ADDED -> "Adicionados recentemente"
        LibrarySort.TITLE_ASC -> "Título A–Z"
        LibrarySort.TITLE_DESC -> "Título Z–A"
        LibrarySort.RELEASE_YEAR -> "Ano de lançamento"
        LibrarySort.LAST_ACTIVITY -> "Última atividade"
        LibrarySort.RATING -> "Nota pessoal"
        LibrarySort.PROGRESS -> "Progresso"
        LibrarySort.LAST_EPISODE -> "Último episódio assistido"
    }

fun MovieStatus.badge(): MediaStatusBadge = when (this) {
    MovieStatus.WANT_TO_WATCH -> MediaStatusBadge.WANT_TO_WATCH
    MovieStatus.WATCHED -> MediaStatusBadge.WATCHED
}

fun TvShowStatus.badge(): MediaStatusBadge = when (this) {
    TvShowStatus.WANT_TO_WATCH -> MediaStatusBadge.WANT_TO_WATCH
    TvShowStatus.WATCHING -> MediaStatusBadge.WATCHING
    TvShowStatus.COMPLETED -> MediaStatusBadge.COMPLETED
    TvShowStatus.PAUSED -> MediaStatusBadge.PAUSED
    TvShowStatus.DROPPED -> MediaStatusBadge.DROPPED
}

fun LibraryMovieItem.toCardData() = MediaCardData(
    id = movie.movieId,
    isMovie = true,
    title = title,
    posterPath = posterPath,
    year = year,
    status = movie.status.badge(),
    isFavorite = movie.isFavorite,
)

/** Barra só quando há episódios disponíveis para contar (série não lançada fica sem barra). */
fun LibraryTvShowEntry.toCardData() = MediaCardData(
    id = item.show.showId,
    isMovie = false,
    title = item.name,
    posterPath = item.posterPath,
    year = item.year,
    status = item.show.status.badge(),
    isFavorite = item.show.isFavorite,
    progress = progress?.takeIf { it.available > 0 }?.let { MediaCardProgress(it.watched, it.available, it.percent) },
)
