package com.thalyspenha.pipoca.domain.repository

import com.thalyspenha.pipoca.domain.model.LibraryMovie
import com.thalyspenha.pipoca.domain.model.LibraryTvShow
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Biblioteca pessoal (dados do usuário, nunca apagados pelo cache TMDB).
 * Só persistência; regras (status, nota, histórico) ficam nos use cases de `domain/usecase/library`.
 */
interface LibraryRepository {
    /** Mais recentes primeiro (`updatedAt`). */
    fun observeMovies(): Flow<List<LibraryMovie>>

    fun observeMovie(movieId: Long): Flow<LibraryMovie?>

    suspend fun getMovie(movieId: Long): LibraryMovie?

    suspend fun saveMovie(movie: LibraryMovie)

    /** Grava o filme e registra uma visualização no histórico, na mesma transação. */
    suspend fun saveMovieWatched(movie: LibraryMovie, watchedAt: Instant)

    /** Remove o filme e o histórico dele (D-029). */
    suspend fun removeMovie(movieId: Long)

    /** Mais recentes primeiro (`updatedAt`). */
    fun observeTvShows(): Flow<List<LibraryTvShow>>

    fun observeTvShow(showId: Long): Flow<LibraryTvShow?>

    suspend fun getTvShow(showId: Long): LibraryTvShow?

    suspend fun saveTvShow(show: LibraryTvShow)

    suspend fun removeTvShow(showId: Long)
}
