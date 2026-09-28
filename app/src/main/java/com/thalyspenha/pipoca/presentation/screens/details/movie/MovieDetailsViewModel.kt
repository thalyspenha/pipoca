package com.thalyspenha.pipoca.presentation.screens.details.movie

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.repository.CollectionRepository
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import com.thalyspenha.pipoca.domain.repository.MovieRepository
import com.thalyspenha.pipoca.domain.usecase.library.RemoveMovieFromLibraryUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetMovieFavoriteUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetMovieRatingUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetMovieStatusUseCase
import com.thalyspenha.pipoca.presentation.components.shownFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Detalhes do filme: observa cache TMDB e biblioteca (Room) e faz refresh ao abrir (D-003).
 * Ações gravam no Room pelos use cases; a tela atualiza pela reemissão do Flow.
 */
@HiltViewModel
class MovieDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val movieRepository: MovieRepository,
    library: LibraryRepository,
    collection: CollectionRepository,
    private val setStatus: SetMovieStatusUseCase,
    private val removeFromLibrary: RemoveMovieFromLibraryUseCase,
    private val setFavorite: SetMovieFavoriteUseCase,
    private val setRating: SetMovieRatingUseCase,
) : ViewModel() {

    /** Argumento `id` de `MovieDetailsRoute` (nome da propriedade da rota). */
    private val movieId: Long = checkNotNull(savedStateHandle.get<Long>(ARG_ID)) { "MovieDetailsRoute sem id" }

    private data class RefreshState(val isRefreshing: Boolean = false, val error: DataError? = null)

    private val refreshState = MutableStateFlow(RefreshState())

    val uiState: StateFlow<MovieDetailsUiState> =
        combine(
            movieRepository.observeMovieDetails(movieId),
            library.observeMovie(movieId),
            refreshState,
            collection.observeItemsFor(movieId, CollectionMediaType.MOVIE),
        ) { movie, personal, refresh, items ->
            when {
                movie != null -> MovieDetailsUiState.Success(
                    movie = movie,
                    personal = personal.toPersonalMovie(),
                    isRefreshing = refresh.isRefreshing,
                    refreshError = refresh.error,
                    collection = items,
                )
                refresh.error != null -> MovieDetailsUiState.Error(refresh.error)
                else -> MovieDetailsUiState.Loading
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MovieDetailsUiState.Loading)

    init {
        refresh(userInitiated = false)
    }

    /** Busca no TMDB se o cache estiver ausente ou vencido; [force] ignora a validade. */
    /** [userInitiated] falso = refresh ao abrir: sem internet e com cache, não avisa (D-055). */
    fun refresh(force: Boolean = false, userInitiated: Boolean = true) {
        if (refreshState.value.isRefreshing) return
        refreshState.value = RefreshState(isRefreshing = true)
        viewModelScope.launch {
            val result = movieRepository.refreshMovieDetails(movieId, force)
            val error = (result as? DataResult.Failure)?.error?.shownFor(
                hasCache = movieRepository.observeMovieDetails(movieId).first() != null,
                userInitiated = userInitiated,
            )
            refreshState.value = RefreshState(error = error)
        }
    }

    fun dismissRefreshError() {
        refreshState.update { it.copy(error = null) }
    }

    /**
     * Botões "Quero assistir" / "Assistido": tocar no status atual tira o filme da biblioteca.
     * Remover apaga também o histórico dele (D-029).
     */
    fun onStatusClick(status: MovieStatus) {
        // Toque duplo: o segundo leria o status já gravado e tiraria o título da biblioteca.
        if (statusJob?.isActive == true) return
        val current = (uiState.value as? MovieDetailsUiState.Success)?.personal?.status
        statusJob = viewModelScope.launch {
            if (current == status) removeFromLibrary(movieId) else setStatus(movieId, status)
            delay(STATUS_CLICK_GUARD_MILLIS)
        }
    }

    private var statusJob: Job? = null

    fun onFavoriteClick() {
        val favorite = (uiState.value as? MovieDetailsUiState.Success)?.personal?.isFavorite ?: false
        viewModelScope.launch { setFavorite(movieId, !favorite) }
    }

    /** Nota 1–10; `null` remove. */
    fun onRatingChange(rating: Int?) {
        viewModelScope.launch { setRating(movieId, rating) }
    }

    private companion object {
        const val ARG_ID = "id"
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val STATUS_CLICK_GUARD_MILLIS = 400L
    }
}
