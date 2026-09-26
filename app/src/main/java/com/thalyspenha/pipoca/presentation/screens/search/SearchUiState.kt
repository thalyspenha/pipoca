package com.thalyspenha.pipoca.presentation.screens.search

import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.MovieSummary
import com.thalyspenha.pipoca.domain.model.TvShowSummary

enum class SearchType { MOVIES, TV_SHOWS }

data class SearchUiState(
    /** Texto exatamente como digitado (o campo mostra isso). */
    val query: String = "",
    val type: SearchType = SearchType.MOVIES,
    val content: SearchContent = SearchContent.Idle,
)

sealed interface SearchContent {
    /** Nada pesquisado ainda (texto vazio ou curto demais). */
    data object Idle : SearchContent
    data object Loading : SearchContent
    data class Results(val items: List<SearchResultItem>) : SearchContent
    data class Empty(val query: String) : SearchContent
    data class Error(val error: DataError) : SearchContent
}

/** Item exibido na lista, igual para filmes e séries. */
data class SearchResultItem(
    val id: Long,
    val type: SearchType,
    val title: String,
    val year: Int?,
    val posterPath: String?,
)

fun MovieSummary.toSearchResultItem() = SearchResultItem(
    id = id,
    type = SearchType.MOVIES,
    title = title,
    year = releaseDate?.year,
    posterPath = posterPath,
)

fun TvShowSummary.toSearchResultItem() = SearchResultItem(
    id = id,
    type = SearchType.TV_SHOWS,
    title = name,
    year = firstAirDate?.year,
    posterPath = posterPath,
)
