package com.thalyspenha.pipoca.presentation.screens.details.season

import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.Episode

/** Mesmo contrato das telas de detalhes: Loading/Error só sem cache. */
sealed interface SeasonUiState {
    data object Loading : SeasonUiState
    data class Error(val error: DataError) : SeasonUiState
    data class Success(
        val showName: String?,
        val seasonName: String?,
        val episodes: List<EpisodeRow>,
        val isRefreshing: Boolean = false,
        val refreshError: DataError? = null,
    ) : SeasonUiState {
        val aired: Int get() = episodes.count { it.isAired }
        val watched: Int get() = episodes.count { it.isWatched && it.isAired }
        val allAiredWatched: Boolean get() = aired > 0 && watched == aired
    }
}

data class EpisodeRow(
    val episode: Episode,
    val isWatched: Boolean,
    /** Já exibido; só esses podem ser marcados. */
    val isAired: Boolean,
)
