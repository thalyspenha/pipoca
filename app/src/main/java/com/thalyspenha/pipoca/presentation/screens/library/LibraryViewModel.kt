package com.thalyspenha.pipoca.presentation.screens.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalyspenha.pipoca.domain.model.LibrarySort
import com.thalyspenha.pipoca.domain.model.MovieLibraryFilter
import com.thalyspenha.pipoca.domain.model.TvShowLibraryFilter
import com.thalyspenha.pipoca.domain.repository.LibraryViewMode
import com.thalyspenha.pipoca.domain.repository.PreferencesRepository
import com.thalyspenha.pipoca.domain.usecase.librarylist.ObserveLibraryMoviesUseCase
import com.thalyspenha.pipoca.domain.usecase.librarylist.ObserveLibraryTvShowsUseCase
import com.thalyspenha.pipoca.domain.usecase.librarylist.ObserveMovieLibraryCountsUseCase
import com.thalyspenha.pipoca.domain.usecase.librarylist.ObserveTvShowLibraryCountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * "Minha Biblioteca" (D-050, D-051): lista reativa do Room conforme aba, filtro, ordenação e
 * pesquisa; contagens do banco; modo Grid/Lista persistido. Seleção sobrevive à recriação do
 * processo (SavedStateHandle).
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
