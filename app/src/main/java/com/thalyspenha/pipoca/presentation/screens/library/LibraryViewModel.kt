package com.thalyspenha.pipoca.presentation.screens.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalyspenha.pipoca.domain.model.LibrarySort
import com.thalyspenha.pipoca.domain.model.MovieLibraryFilter
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.model.TvShowLibraryFilter
import com.thalyspenha.pipoca.domain.repository.LibraryViewMode
import com.thalyspenha.pipoca.domain.repository.PreferencesRepository
import com.thalyspenha.pipoca.domain.usecase.cache.FetchMissingDetailsUseCase
import com.thalyspenha.pipoca.domain.usecase.library.RemoveMovieFromLibraryUseCase
import com.thalyspenha.pipoca.domain.usecase.library.RemoveTvShowFromLibraryUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetMovieFavoriteUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetMovieStatusUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowFavoriteUseCase
import com.thalyspenha.pipoca.domain.usecase.librarylist.ObserveLibraryMoviesUseCase
import com.thalyspenha.pipoca.domain.usecase.librarylist.ObserveLibraryTvShowsUseCase
import com.thalyspenha.pipoca.domain.usecase.librarylist.ObserveMovieLibraryCountsUseCase
import com.thalyspenha.pipoca.domain.usecase.librarylist.ObserveTvShowLibraryCountsUseCase
import com.thalyspenha.pipoca.presentation.components.MediaCardData
import com.thalyspenha.pipoca.presentation.components.MediaStatusBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * "Minha Biblioteca" (D-050, D-051): lista reativa do Room conforme aba, filtro, ordenação e
 * pesquisa; contagens do banco; modo Grid/Lista persistido. Seleção sobrevive à recriação do
 * processo (SavedStateHandle). Ações rápidas (toque longo) usam os use cases da biblioteca;
 * a lista se atualiza sozinha pelo Flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val observeMovies: ObserveLibraryMoviesUseCase,
    private val observeTvShows: ObserveLibraryTvShowsUseCase,
    observeMovieCounts: ObserveMovieLibraryCountsUseCase,
    observeTvShowCounts: ObserveTvShowLibraryCountsUseCase,
    private val preferences: PreferencesRepository,
    private val setMovieStatus: SetMovieStatusUseCase,
    private val setMovieFavorite: SetMovieFavoriteUseCase,
    private val setTvShowFavorite: SetTvShowFavoriteUseCase,
    private val removeMovie: RemoveMovieFromLibraryUseCase,
    private val removeTvShow: RemoveTvShowFromLibraryUseCase,
    private val fetchMissingDetails: FetchMissingDetailsUseCase,
) : ViewModel() {

    private val selection = combine(
        savedStateHandle.getStateFlow(KEY_TAB, LibraryTab.MOVIES),
        savedStateHandle.getStateFlow(KEY_MOVIE_FILTER, MovieLibraryFilter.ALL),
        savedStateHandle.getStateFlow(KEY_TV_FILTER, TvShowLibraryFilter.ALL),
        combine(
            savedStateHandle.getStateFlow(KEY_MOVIE_SORT, LibrarySort.RECENTLY_ADDED),
            savedStateHandle.getStateFlow(KEY_TV_SORT, LibrarySort.RECENTLY_ADDED),
        ) { movieSort, tvShowSort -> movieSort to tvShowSort },
        savedStateHandle.getStateFlow(KEY_QUERY, ""),
    ) { tab, movieFilter, tvShowFilter, (movieSort, tvShowSort), query ->
        LibrarySelection(tab, movieFilter, tvShowFilter, movieSort, tvShowSort, query)
    }

    /** Lista junto com a seleção que a gerou: a tela nunca mostra itens de outra aba/filtro. */
    private val list = selection.flatMapLatest { s ->
        when (s.tab) {
            LibraryTab.MOVIES -> observeMovies(s.movieFilter, s.movieSort, s.query)
                .map { movies -> s to movies.map { it.toCardData() } }
            LibraryTab.TV_SHOWS -> observeTvShows(s.tvShowFilter, s.tvShowSort, s.query)
                .map { shows -> s to shows.map { it.toCardData() } }
        }
    }

    val uiState: StateFlow<LibraryUiState> =
        combine(list, observeMovieCounts(), observeTvShowCounts(), preferences.libraryViewMode) { (s, items), movieCounts, tvShowCounts, viewMode ->
            fetchMissing(items)
            LibraryUiState(
                selection = s,
                viewMode = viewMode,
                movieCounts = movieCounts,
                tvShowCounts = tvShowCounts,
                items = items,
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LibraryUiState())

    fun onTabChange(tab: LibraryTab) {
        savedStateHandle[KEY_TAB] = tab
    }

    fun onMovieFilterChange(filter: MovieLibraryFilter) {
        savedStateHandle[KEY_MOVIE_FILTER] = filter
    }

    fun onTvShowFilterChange(filter: TvShowLibraryFilter) {
        savedStateHandle[KEY_TV_FILTER] = filter
    }

    /** Ordenação vale para a aba atual; as só de séries são ignoradas em filmes. */
    fun onSortChange(sort: LibrarySort) {
        when (savedStateHandle.get<LibraryTab>(KEY_TAB) ?: LibraryTab.MOVIES) {
            LibraryTab.MOVIES -> if (sort.forMovies) savedStateHandle[KEY_MOVIE_SORT] = sort
            LibraryTab.TV_SHOWS -> savedStateHandle[KEY_TV_SORT] = sort
        }
    }

    fun onQueryChange(query: String) {
        savedStateHandle[KEY_QUERY] = query
    }

    fun onViewModeChange(mode: LibraryViewMode) {
        preferences.setLibraryViewMode(mode)
    }

    /** Filme: alterna assistido/quero assistir (assistido registra no histórico, D-029). */
    fun onToggleWatched(item: MediaCardData) {
        if (!item.isMovie) return
        val status = if (item.status == MediaStatusBadge.WATCHED) MovieStatus.WANT_TO_WATCH else MovieStatus.WATCHED
        viewModelScope.launch { setMovieStatus(item.id, status) }
    }

    fun onToggleFavorite(item: MediaCardData) {
        viewModelScope.launch {
            if (item.isMovie) setMovieFavorite(item.id, !item.isFavorite) else setTvShowFavorite(item.id, !item.isFavorite)
        }
    }

    /** Remove da biblioteca (série leva junto episódios assistidos e histórico, D-037); a tela confirma antes. */
    fun onRemove(item: MediaCardData) {
        viewModelScope.launch { if (item.isMovie) removeMovie(item.id) else removeTvShow(item.id) }
    }

    /** Item sem cache TMDB (ex.: depois de limpar o cache) busca os detalhes uma vez (D-055). */
    private fun fetchMissing(items: List<MediaCardData>) {
        val missing = items.filter { it.title == null }
        fetchMissingDetails.request(
            scope = viewModelScope,
            movieIds = missing.filter { it.isMovie }.map { it.id },
            tvShowIds = missing.filterNot { it.isMovie }.map { it.id },
        )
    }

    private companion object {
        const val KEY_TAB = "tab"
        const val KEY_MOVIE_FILTER = "movieFilter"
        const val KEY_TV_FILTER = "tvShowFilter"
        const val KEY_MOVIE_SORT = "movieSort"
        const val KEY_TV_SORT = "tvShowSort"
        const val KEY_QUERY = "query"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
