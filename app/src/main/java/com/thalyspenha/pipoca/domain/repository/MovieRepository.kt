package com.thalyspenha.pipoca.domain.repository

import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.MovieDetails
import kotlinx.coroutines.flow.Flow

/**
 * Detalhes de filme com cache local (D-003): a UI observa o Room e pede [refreshMovieDetails].
 * Falha no refresh com cache presente é erro não bloqueante.
 */
interface MovieRepository {
    /** Emite `null` enquanto o filme não está em cache. */
    fun observeMovieDetails(id: Long): Flow<MovieDetails?>

    /** Busca no TMDB se não houver cache válido (ou se [force]) e grava no Room. */
    suspend fun refreshMovieDetails(id: Long, force: Boolean = false): DataResult<Unit>
}
