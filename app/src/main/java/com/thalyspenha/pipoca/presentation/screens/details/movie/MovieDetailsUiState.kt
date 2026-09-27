package com.thalyspenha.pipoca.presentation.screens.details.movie

import com.thalyspenha.pipoca.domain.model.CollectionItem
import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.LibraryMovie
import com.thalyspenha.pipoca.domain.model.MovieDetails
import com.thalyspenha.pipoca.domain.model.MovieStatus

/**
 * Loading/Error só quando não há cache; com cache é sempre [Success] (D-032).
 * Falha de refresh com cache vira [Success.refreshError], não bloqueante.
 */
sealed interface MovieDetailsUiState {
    data object Loading : MovieDetailsUiState
    data class Error(val error: DataError) : MovieDetailsUiState
    data class Success(
        val movie: MovieDetails,
        val personal: PersonalMovie,
        val isRefreshing: Boolean = false,
        val refreshError: DataError? = null,
        /** Itens da coleção deste filme (independentes do status, D-039). */
        val collection: List<CollectionItem> = emptyList(),
    ) : MovieDetailsUiState
}

/** Dados pessoais do filme; `status` nulo = fora da biblioteca. */
data class PersonalMovie(
    val status: MovieStatus? = null,
    val isFavorite: Boolean = false,
    val rating: Int? = null,
) {
    val inLibrary: Boolean get() = status != null
}

fun LibraryMovie?.toPersonalMovie(): PersonalMovie =
    this?.let { PersonalMovie(status = it.status, isFavorite = it.isFavorite, rating = it.rating) } ?: PersonalMovie()
