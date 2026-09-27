package com.thalyspenha.pipoca.presentation.screens.details.tv

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.progress.ShowProgress
import com.thalyspenha.pipoca.domain.repository.CollectionRepository
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import com.thalyspenha.pipoca.domain.repository.SeasonRepository
import com.thalyspenha.pipoca.domain.repository.TvShowRepository
import com.thalyspenha.pipoca.domain.usecase.episodes.MarkEpisodeWatchedUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.ObserveShowProgressUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.RefreshShowEpisodesUseCase
import com.thalyspenha.pipoca.domain.usecase.library.RemoveTvShowFromLibraryUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowFavoriteUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowRatingUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Detalhes da série (D-032, D-034) com progresso e temporadas (D-038).
 * Série na biblioteca: baixa todas as temporadas uma vez para o progresso ficar completo.
 */
@HiltViewModel
class TvShowDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tvShowRepository: TvShowRepository,
    seasonRepository: SeasonRepository,
    private val library: LibraryRepository,
    collection: CollectionRepository,
    observeProgress: ObserveShowProgressUseCase,
    private val refreshEpisodes: RefreshShowEpisodesUseCase,
    private val setStatus: SetTvShowStatusUseCase,
    private val removeFromLibrary: RemoveTvShowFromLibraryUseCase,
    private val setFavorite: SetTvShowFavoriteUseCase,
    private val setRating: SetTvShowRatingUseCase,
    private val markEpisode: MarkEpisodeWatchedUseCase,
    private val clock: Clock,
) : ViewModel() {

    /** Argumento `id` de `TvShowDetailsRoute`. */
    private val showId: Long = checkNotNull(savedStateHandle.get<Long>(ARG_ID)) { "TvShowDetailsRoute sem id" }

    private data class RefreshState(val isRefreshing: Boolean = false, val error: DataError? = null)

    private data class EpisodesState(
        val progress: ShowProgress?,
        val seasons: List<SeasonRow>,
        val isLoading: Boolean,
    )

    private val refreshState = MutableStateFlow(RefreshState())
    private val episodesLoading = MutableStateFlow(false)

    private val episodesState = combine(
        observeProgress(showId),
        tvShowRepository.observeTvShowDetails(showId),
        seasonRepository.observeShowEpisodes(showId),
        library.observeWatchedEpisodes(showId),
        episodesLoading,
    ) { progress, show, episodes, watched, loading ->
        EpisodesState(
            progress = progress,
            seasons = buildSeasonRows(
                seasons = show?.seasons.orEmpty(),
                episodes = episodes,
                watchedEpisodeIds = watched.map { it.episodeId }.toSet(),
                today = LocalDate.now(clock),
            ),
            isLoading = loading,
        )
    }

    val uiState: StateFlow<TvShowDetailsUiState> =
        combine(
            tvShowRepository.observeTvShowDetails(showId),
            library.observeTvShow(showId),
            refreshState,
            episodesState,
            collection.observeItemsFor(showId, CollectionMediaType.TV_SHOW),
        ) { show, personal, refresh, episodes, items ->
            when {
                show != null -> TvShowDetailsUiState.Success(
                    show = show,
                    personal = personal.toPersonalTvShow(),
                    isRefreshing = refresh.isRefreshing,
                    refreshError = refresh.error,
                    progress = episodes.progress,
                    seasons = episodes.seasons,
                    isLoadingEpisodes = episodes.isLoading,
                    collection = items,
                )
                refresh.error != null -> TvShowDetailsUiState.Error(refresh.error)
                else -> TvShowDetailsUiState.Loading
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TvShowDetailsUiState.Loading)

    init {
        refresh()
        // Quando (ou se) a série estiver na biblioteca, baixa as temporadas uma vez.
        viewModelScope.launch {
            library.observeTvShow(showId).filterNotNull().first()
            loadEpisodes()
        }
    }

    fun refresh(force: Boolean = false) {
        if (refreshState.value.isRefreshing) return
        refreshState.value = RefreshState(isRefreshing = true)
        viewModelScope.launch {
            val result = tvShowRepository.refreshTvShowDetails(showId, force)
            refreshState.value = RefreshState(error = (result as? DataResult.Failure)?.error)
        }
    }

    /** Baixa as temporadas que faltam; falha aparece como erro de refresh não bloqueante. */
    fun loadEpisodes() {
        if (episodesLoading.value) return
        episodesLoading.value = true
        viewModelScope.launch {
            val result = refreshEpisodes(showId)
            episodesLoading.value = false
            if (result is DataResult.Failure) refreshState.update { it.copy(error = result.error) }
        }
    }

    fun dismissRefreshError() {
        refreshState.update { it.copy(error = null) }
    }

    /** Tocar no status atual tira a série da biblioteca (e os episódios assistidos, D-037). */
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

    /** Atalho "marcar próximo episódio" do card de progresso. */
    fun onMarkNextEpisode() {
        val next = (uiState.value as? TvShowDetailsUiState.Success)?.progress?.nextEpisode ?: return
        viewModelScope.launch { markEpisode(next) }
    }

    private companion object {
        const val ARG_ID = "id"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
