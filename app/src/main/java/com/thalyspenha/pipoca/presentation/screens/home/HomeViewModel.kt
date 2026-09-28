package com.thalyspenha.pipoca.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalyspenha.pipoca.domain.model.CollectionEntry
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.LibraryMovieItem
import com.thalyspenha.pipoca.domain.model.LibraryTvShowItem
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import com.thalyspenha.pipoca.domain.repository.CollectionRepository
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import com.thalyspenha.pipoca.domain.usecase.cache.FetchMissingDetailsUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.MarkEpisodeWatchedUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.ObserveWatchingShowsUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.WatchingShow
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
 * Painel da Home (D-030, D-044). Tudo vem de Flows do Room: marcar episódio, favoritar ou
 * mexer na coleção em qualquer tela reflete aqui sozinho.
 * Com token: busca uma vez por item o que falta no cache (detalhes sem título; temporadas das
 * séries assistindo com progresso incompleto).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tmdbConfig: TmdbConfig,
    library: LibraryRepository,
    collection: CollectionRepository,
    observeWatchingShows: ObserveWatchingShowsUseCase,
    private val fetchMissingDetails: FetchMissingDetailsUseCase,
    private val markEpisode: MarkEpisodeWatchedUseCase,
) : ViewModel() {

    val uiState: StateFlow<UiState<HomeContent>> =
        combine(
            library.observeMovieItems(),
            library.observeTvShowItems(),
            observeWatchingShows(),
            collection.observeEntries(),
        ) { movies, tvShows, watching, collectionEntries ->
            fetchMissing(movies, tvShows, watching, collectionEntries)
            buildHomeContent(tmdbConfig.isConfigured, movies, tvShows, watching, collectionEntries)
        }
            .map<HomeContent, UiState<HomeContent>> { UiState.Success(it) }
            .catch { emit(UiState.Error("Não foi possível carregar sua biblioteca.", it)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    /** Botão "Assisti" de Continuar assistindo. */
    fun onMarkWatched(episode: Episode) {
        viewModelScope.launch { markEpisode(episode) }
    }

    private fun fetchMissing(
        movies: List<LibraryMovieItem>,
        tvShows: List<LibraryTvShowItem>,
        watching: List<WatchingShow>,
        collectionEntries: List<CollectionEntry>,
    ) {
        val movieIds = movies.filter { it.title == null }.map { it.movie.movieId } +
            collectionEntries.filter { it.title == null && it.item.mediaType == CollectionMediaType.MOVIE }.map { it.item.tmdbId }
        val showIds = tvShows.filter { it.name == null }.map { it.show.showId } +
            collectionEntries.filter { it.title == null && it.item.mediaType == CollectionMediaType.TV_SHOW }.map { it.item.tmdbId }
        fetchMissingDetails.request(
            scope = viewModelScope,
            movieIds = movieIds,
            tvShowIds = showIds,
            episodesOfShowIds = watching.filter { !it.progress.isComplete }.map { it.item.show.showId },
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
