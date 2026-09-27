package com.thalyspenha.pipoca.presentation.screens.details.tv

import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.LibraryTvShow
import com.thalyspenha.pipoca.domain.model.TvShowDetails
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.progress.ShowProgress

/** Mesmo contrato do filme (D-032): Loading/Error só sem cache; refresh com cache não bloqueia. */
sealed interface TvShowDetailsUiState {
    data object Loading : TvShowDetailsUiState
    data class Error(val error: DataError) : TvShowDetailsUiState
    data class Success(
        val show: TvShowDetails,
        val personal: PersonalTvShow,
        val isRefreshing: Boolean = false,
        val refreshError: DataError? = null,
        /** Nulo antes do cálculo; parcial (`isComplete = false`) até todas as temporadas estarem em cache. */
        val progress: ShowProgress? = null,
        val seasons: List<SeasonRow> = emptyList(),
        /** Baixando as temporadas para o progresso (só séries na biblioteca, D-038). */
        val isLoadingEpisodes: Boolean = false,
    ) : TvShowDetailsUiState
}

/** Dados pessoais da série; `status` nulo = fora da biblioteca. */
data class PersonalTvShow(
    val status: TvShowStatus? = null,
    val isFavorite: Boolean = false,
    val rating: Int? = null,
) {
    val inLibrary: Boolean get() = status != null
}

fun LibraryTvShow?.toPersonalTvShow(): PersonalTvShow =
    this?.let { PersonalTvShow(status = it.status, isFavorite = it.isFavorite, rating = it.rating) } ?: PersonalTvShow()

/** Status oferecidos na tela; `PAUSED`/`DROPPED` ficam para fases futuras. */
val TV_SHOW_SELECTABLE_STATUSES = listOf(TvShowStatus.WANT_TO_WATCH, TvShowStatus.WATCHING, TvShowStatus.COMPLETED)
