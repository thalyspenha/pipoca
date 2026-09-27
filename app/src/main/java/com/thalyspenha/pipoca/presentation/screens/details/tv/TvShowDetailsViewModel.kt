package com.thalyspenha.pipoca.presentation.screens.details.tv

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import com.thalyspenha.pipoca.domain.repository.TvShowRepository
import com.thalyspenha.pipoca.domain.usecase.library.RemoveTvShowFromLibraryUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowFavoriteUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowRatingUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Detalhes da série: mesmo fluxo do filme (D-032, D-034). */
@HiltViewModel
class TvShowDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tvShowRepository: TvShowRepository,
    library: LibraryRepository,
    private val setStatus: SetTvShowStatusUseCase,
    private val removeFromLibrary: RemoveTvShowFromLibraryUseCase,
    private val setFavorite: SetTvShowFavoriteUseCase,
    private val setRating: SetTvShowRatingUseCase,
) : ViewModel() {

    /** Argumento `id` de `TvShowDetailsRoute`. */
    private val showId: Long = checkNotNull(savedStateHandle.get<Long>(ARG_ID)) { "TvShowDetailsRoute sem id" }

    private data class RefreshState(val isRefreshing: Boolean = false, val error: DataError? = null)

    private val refreshState = MutableStateFlow(RefreshState())

    val uiState: StateFlow<TvShowDetailsUiState> =
        combine(
            tvShowRepository.observeTvShowDetails(showId),
            library.observeTvShow(showId),
            refreshState,
        ) { show, personal, refresh ->
            when {
                show != null -> TvShowDetailsUiState.Success(
                    show = show,
                    personal = personal.toPersonalTvShow(),
                    isRefreshing = refresh.isRefreshing,
                    refreshError = refresh.error,
                )
                refresh.error != null -> TvShowDetailsUiState.Error(refresh.error)
                else -> TvShowDetailsUiState.Loading
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TvShowDetailsUiState.Loading)

    init {
        refresh()
    }

    fun refresh(force: Boolean = false) {
        if (refreshState.value.isRefreshing) return
        refreshState.value = RefreshState(isRefreshing = true)
        viewModelScope.launch {
            val result = tvShowRepository.refreshTvShowDetails(showId, force)
            refreshState.value = RefreshState(error = (result as? DataResult.Failure)?.error)
        }
    }

    fun dismissRefreshError() {
        refreshState.update { it.copy(error = null) }
    }

    /** Tocar no status atual tira a série da biblioteca. */
    fun onStatusClick(status: TvShowStatus) {
        val current = (uiState.value as? TvShowDetailsUiState.Success)?.personal?.status
        viewModelScope.launch {
            if (current == status) removeFromLibrary(showId) else setStatus(showId, status)
        }
    }

    fun onFavoriteClick() {
        val favorite = (uiState.value as? TvShowDetailsUiState.Success)?.personal?.isFavorite ?: false
        viewModelScope.launch { setFavorite(showId, !favorite) }
    }

    fun onRatingChange(rating: Int?) {
        viewModelScope.launch { setRating(showId, rating) }
    }

    private companion object {
        const val ARG_ID = "id"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
