package com.thalyspenha.pipoca.presentation.screens.details.season

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.progress.ShowProgressCalculator
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import com.thalyspenha.pipoca.domain.repository.SeasonRepository
import com.thalyspenha.pipoca.domain.repository.TvShowRepository
import com.thalyspenha.pipoca.domain.usecase.episodes.MarkEpisodeWatchedUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.MarkSeasonWatchedUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.UnmarkEpisodeUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.UnmarkSeasonUseCase
import com.thalyspenha.pipoca.presentation.components.shownFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Episódios de uma temporada com marcação (D-037, D-038). */
@HiltViewModel
class SeasonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    tvShowRepository: TvShowRepository,
    private val seasonRepository: SeasonRepository,
    library: LibraryRepository,
    private val markEpisode: MarkEpisodeWatchedUseCase,
    private val unmarkEpisode: UnmarkEpisodeUseCase,
    private val markSeason: MarkSeasonWatchedUseCase,
    private val unmarkSeason: UnmarkSeasonUseCase,
    private val clock: Clock,
) : ViewModel() {

    /** Argumentos de `SeasonRoute`. */
    private val showId: Long = checkNotNull(savedStateHandle.get<Long>("showId")) { "SeasonRoute sem showId" }
    private val seasonNumber: Int = checkNotNull(savedStateHandle.get<Int>("seasonNumber")) { "SeasonRoute sem seasonNumber" }

    private data class RefreshState(val isRefreshing: Boolean = false, val error: DataError? = null)

    private val refreshState = MutableStateFlow(RefreshState())

    val uiState: StateFlow<SeasonUiState> =
        combine(
            tvShowRepository.observeTvShowDetails(showId),
            seasonRepository.observeSeasonEpisodes(showId, seasonNumber),
            library.observeWatchedEpisodes(showId),
            refreshState,
        ) { show, episodes, watched, refresh ->
            val watchedIds = watched.map { it.episodeId }.toSet()
            val today = LocalDate.now(clock)
            when {
                episodes.isNotEmpty() -> SeasonUiState.Success(
                    showName = show?.name,
                    seasonName = show?.seasons?.firstOrNull { it.seasonNumber == seasonNumber }?.name,
                    episodes = episodes.map {
                        EpisodeRow(it, isWatched = it.id in watchedIds, isAired = ShowProgressCalculator.isAired(it, today))
                    },
                    isRefreshing = refresh.isRefreshing,
                    refreshError = refresh.error,
                )
                refresh.error != null -> SeasonUiState.Error(refresh.error)
                refresh.isRefreshing -> SeasonUiState.Loading
                // Refresh ok e nenhum episódio: temporada vazia no TMDB.
                else -> SeasonUiState.Success(showName = show?.name, seasonName = null, episodes = emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SeasonUiState.Loading)

    init {
        refresh(userInitiated = false)
    }

    /** [userInitiated] falso = refresh ao abrir: sem internet e com episódios salvos, não avisa (D-055). */
    fun refresh(force: Boolean = false, userInitiated: Boolean = true) {
        if (refreshState.value.isRefreshing) return
        refreshState.value = RefreshState(isRefreshing = true)
        viewModelScope.launch {
            val result = seasonRepository.refreshSeason(showId, seasonNumber, force)
            val error = (result as? DataResult.Failure)?.error?.shownFor(
                hasCache = seasonRepository.observeSeasonEpisodes(showId, seasonNumber).first().isNotEmpty(),
                userInitiated = userInitiated,
            )
            refreshState.value = RefreshState(error = error)
        }
    }

    fun dismissRefreshError() {
        refreshState.update { it.copy(error = null) }
    }

    fun onEpisodeToggle(episode: Episode, watched: Boolean) {
        viewModelScope.launch { if (watched) markEpisode(episode) else unmarkEpisode(episode) }
    }

    fun onMarkSeason() {
        viewModelScope.launch { markSeason(showId, seasonNumber) }
    }

    fun onUnmarkSeason() {
        viewModelScope.launch { unmarkSeason(showId, seasonNumber) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
