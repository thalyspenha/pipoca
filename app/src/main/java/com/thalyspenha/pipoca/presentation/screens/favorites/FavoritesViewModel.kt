package com.thalyspenha.pipoca.presentation.screens.favorites

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalyspenha.pipoca.domain.model.LibraryMovieItem
import com.thalyspenha.pipoca.domain.model.LibraryTvShowItem
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import com.thalyspenha.pipoca.domain.usecase.library.SetMovieFavoriteUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FavoritesTab { MOVIES, TV_SHOWS }

data class FavoritesUiState(
    val tab: FavoritesTab = FavoritesTab.MOVIES,
    val movies: List<FavoriteItem> = emptyList(),
    val tvShows: List<FavoriteItem> = emptyList(),
    val isLoading: Boolean = true,
) {
    val items: List<FavoriteItem> get() = if (tab == FavoritesTab.MOVIES) movies else tvShows
}

/** Linha da lista. `title` nulo = ainda sem cache TMDB. */
data class FavoriteItem(
    val id: Long,
    val tab: FavoritesTab,
    val title: String?,
    val posterPath: String?,
    val year: Int?,
    val statusLabel: String,
)

/**
 * Favoritos (D-045): filmes e séries com `isFavorite`, mais recentes primeiro (ordem do Room).
 * Remover desfavorita sem tirar da biblioteca; `undoRemove` refaz.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    library: LibraryRepository,
    private val setMovieFavorite: SetMovieFavoriteUseCase,
    private val setTvShowFavorite: SetTvShowFavoriteUseCase,
) : ViewModel() {

    private val tab = savedStateHandle.getStateFlow(KEY_TAB, FavoritesTab.MOVIES)

    val uiState: StateFlow<FavoritesUiState> =
        combine(library.observeMovieItems(), library.observeTvShowItems(), tab) { movies, tvShows, tab ->
            FavoritesUiState(
                tab = tab,
                movies = movies.filter { it.movie.isFavorite }.map { it.toFavoriteItem() },
                tvShows = tvShows.filter { it.show.isFavorite }.map { it.toFavoriteItem() },
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), FavoritesUiState())

    fun onTabChange(tab: FavoritesTab) {
        savedStateHandle[KEY_TAB] = tab
    }

    fun onRemove(item: FavoriteItem) = setFavorite(item, false)

    fun undoRemove(item: FavoriteItem) = setFavorite(item, true)

    private fun setFavorite(item: FavoriteItem, favorite: Boolean) {
        viewModelScope.launch {
            when (item.tab) {
                FavoritesTab.MOVIES -> setMovieFavorite(item.id, favorite)
                FavoritesTab.TV_SHOWS -> setTvShowFavorite(item.id, favorite)
            }
        }
    }

    private companion object {
        const val KEY_TAB = "tab"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun LibraryMovieItem.toFavoriteItem() = FavoriteItem(
    id = movie.movieId,
    tab = FavoritesTab.MOVIES,
    title = title,
    posterPath = posterPath,
    year = year,
    statusLabel = when (movie.status) {
        MovieStatus.WANT_TO_WATCH -> "Quero assistir"
        MovieStatus.WATCHED -> "Assistido"
    },
)

private fun LibraryTvShowItem.toFavoriteItem() = FavoriteItem(
    id = show.showId,
    tab = FavoritesTab.TV_SHOWS,
    title = name,
    posterPath = posterPath,
    year = year,
    statusLabel = when (show.status) {
        TvShowStatus.WANT_TO_WATCH -> "Quero assistir"
        TvShowStatus.WATCHING -> "Assistindo"
        TvShowStatus.COMPLETED -> "Concluída"
        TvShowStatus.PAUSED -> "Pausada"
        TvShowStatus.DROPPED -> "Abandonada"
    },
)
