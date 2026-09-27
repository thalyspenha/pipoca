package com.thalyspenha.pipoca.domain.repository

import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.LibraryMovie
import com.thalyspenha.pipoca.domain.model.LibraryMovieItem
import com.thalyspenha.pipoca.domain.model.LibraryTvShow
import com.thalyspenha.pipoca.domain.model.LibraryTvShowItem
import com.thalyspenha.pipoca.domain.model.WatchedEpisode
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

    /** Filmes com título/poster do cache TMDB, mais recentes primeiro. Reemite quando o cache muda. */
    fun observeMovieItems(): Flow<List<LibraryMovieItem>>

    suspend fun getMovie(movieId: Long): LibraryMovie?

    suspend fun saveMovie(movie: LibraryMovie)

    /** Grava o filme e registra uma visualização no histórico, na mesma transação. */
    suspend fun saveMovieWatched(movie: LibraryMovie, watchedAt: Instant)

    /** Remove o filme e o histórico dele (D-029). */
    suspend fun removeMovie(movieId: Long)

    /** Mais recentes primeiro (`updatedAt`). */
    fun observeTvShows(): Flow<List<LibraryTvShow>>

    fun observeTvShow(showId: Long): Flow<LibraryTvShow?>

    /** Séries com nome/poster do cache TMDB, mais recentes primeiro. */
    fun observeTvShowItems(): Flow<List<LibraryTvShowItem>>

    suspend fun getTvShow(showId: Long): LibraryTvShow?

    suspend fun saveTvShow(show: LibraryTvShow)

    /** Remove a série, os episódios assistidos e o histórico dela (D-037). */
    suspend fun removeTvShow(showId: Long)

    fun observeWatchedEpisodes(showId: Long): Flow<List<WatchedEpisode>>

    /** Assistidos de todas as séries `WATCHING`, numa consulta (D-044). */
    fun observeWatchingShowsWatchedEpisodes(): Flow<List<WatchedEpisode>>

    /** Marca como assistidos (data [watchedAt]) e registra no histórico, numa transação. */
    suspend fun markEpisodesWatched(episodes: List<Episode>, watchedAt: Instant)

    /** Desmarca e apaga os eventos de histórico desses episódios, numa transação. */
    suspend fun unmarkEpisodes(episodeIds: List<Long>)
}
