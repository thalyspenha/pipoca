package com.thalyspenha.pipoca.domain.repository

import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.HistoryEntry
import com.thalyspenha.pipoca.domain.model.LibraryMovie
import com.thalyspenha.pipoca.domain.model.LibrarySort
import com.thalyspenha.pipoca.domain.model.MovieLibraryCounts
import com.thalyspenha.pipoca.domain.model.MovieLibraryFilter
import com.thalyspenha.pipoca.domain.model.TvShowLibraryCounts
import com.thalyspenha.pipoca.domain.model.TvShowLibraryFilter
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

    /** Biblioteca (D-050): filtro e ordenação no banco; [LibrarySort.PROGRESS] cai na ordem padrão. */
    fun observeMovieList(filter: MovieLibraryFilter, sort: LibrarySort): Flow<List<LibraryMovieItem>>

    fun observeTvShowList(filter: TvShowLibraryFilter, sort: LibrarySort): Flow<List<LibraryTvShowItem>>

    fun observeMovieLibraryCounts(): Flow<MovieLibraryCounts>

    fun observeTvShowLibraryCounts(): Flow<TvShowLibraryCounts>

    /** Assistidos de todas as séries da biblioteca, numa consulta. */
    fun observeLibraryShowsWatchedEpisodes(): Flow<List<WatchedEpisode>>

    /** Histórico de visualizações, mais recente primeiro (D-046). */
    fun observeHistory(): Flow<List<HistoryEntry>>

    /** Assistidos de todas as séries `WATCHING`, numa consulta (D-044). */
    fun observeWatchingShowsWatchedEpisodes(): Flow<List<WatchedEpisode>>

    /** Marca como assistidos (data [watchedAt]) e registra no histórico, numa transação. */
    suspend fun markEpisodesWatched(episodes: List<Episode>, watchedAt: Instant)

    /** Desmarca e apaga os eventos de histórico desses episódios, numa transação. */
    suspend fun unmarkEpisodes(episodeIds: List<Long>)
}
