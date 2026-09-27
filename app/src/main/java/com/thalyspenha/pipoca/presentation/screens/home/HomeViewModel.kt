package com.thalyspenha.pipoca.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalyspenha.pipoca.domain.model.LibraryMovieItem
import com.thalyspenha.pipoca.domain.model.LibraryTvShowItem
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import com.thalyspenha.pipoca.domain.repository.MovieRepository
import com.thalyspenha.pipoca.domain.repository.TvShowRepository
import com.thalyspenha.pipoca.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Observa a biblioteca (Room) e monta a Home. Itens sem cache TMDB (ex.: cache limpo)
 * disparam uma busca de detalhes, uma vez por item; o Room reemite com título e pôster.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tmdbConfig: TmdbConfig,
    library: LibraryRepository,
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository,
) : ViewModel() {

    private val requestedMovies = mutableSetOf<Long>()
    private val requestedTvShows = mutableSetOf<Long>()

    val uiState: StateFlow<UiState<HomeContent>> =
        combine(library.observeMovieItems(), library.observeTvShowItems()) { movies, tvShows ->
            fetchMissingDetails(movies, tvShows)
            buildHomeContent(tmdbConfig.isConfigured, movies, tvShows)
        }
            .map<HomeContent, UiState<HomeContent>> { UiState.Success(it) }
            .catch { emit(UiState.Error("Não foi possível carregar sua biblioteca.", it)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    private fun fetchMissingDetails(movies: List<LibraryMovieItem>, tvShows: List<LibraryTvShowItem>) {
        if (!tmdbConfig.isConfigured) return
        movies.filter { it.title == null && requestedMovies.add(it.movie.movieId) }.forEach { item ->
            viewModelScope.launch { movieRepository.refreshMovieDetails(item.movie.movieId) }
        }
        tvShows.filter { it.name == null && requestedTvShows.add(it.show.showId) }.forEach { item ->
            viewModelScope.launch { tvShowRepository.refreshTvShowDetails(item.show.showId) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
