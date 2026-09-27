package com.thalyspenha.pipoca.domain.usecase.library

import com.thalyspenha.pipoca.domain.model.LibraryMovie
import com.thalyspenha.pipoca.domain.model.LibraryMovieItem
import com.thalyspenha.pipoca.domain.model.LibraryTvShow
import com.thalyspenha.pipoca.domain.model.LibraryTvShowItem
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Biblioteca em memória; `movieWatches` guarda o histórico de visualizações por filme.
 * `movieTitles`/`tvShowNames` simulam o cache TMDB (ausente = sem cache).
 */
class FakeLibraryRepository : LibraryRepository {
    val movies = MutableStateFlow<Map<Long, LibraryMovie>>(emptyMap())
    val tvShows = MutableStateFlow<Map<Long, LibraryTvShow>>(emptyMap())
    val movieWatches = mutableMapOf<Long, MutableList<Instant>>()
    val movieTitles = MutableStateFlow<Map<Long, String>>(emptyMap())
    val tvShowNames = MutableStateFlow<Map<Long, String>>(emptyMap())

    override fun observeMovieItems(): Flow<List<LibraryMovieItem>> =
        combine(observeMovies(), movieTitles) { list, titles ->
            list.map { LibraryMovieItem(it, titles[it.movieId], posterPath = null, year = null) }
        }

    override fun observeTvShowItems(): Flow<List<LibraryTvShowItem>> =
        combine(observeTvShows(), tvShowNames) { list, names ->
            list.map { LibraryTvShowItem(it, names[it.showId], posterPath = null, year = null) }
        }

    override fun observeMovies(): Flow<List<LibraryMovie>> =
        movies.map { it.values.sortedByDescending(LibraryMovie::updatedAt) }

    override fun observeMovie(movieId: Long): Flow<LibraryMovie?> = movies.map { it[movieId] }

    override suspend fun getMovie(movieId: Long): LibraryMovie? = movies.value[movieId]

    override suspend fun saveMovie(movie: LibraryMovie) {
        movies.value += movie.movieId to movie
    }

    override suspend fun saveMovieWatched(movie: LibraryMovie, watchedAt: Instant) {
        saveMovie(movie)
        movieWatches.getOrPut(movie.movieId) { mutableListOf() } += watchedAt
    }

    override suspend fun removeMovie(movieId: Long) {
        movies.value -= movieId
        movieWatches.remove(movieId)
    }

    override fun observeTvShows(): Flow<List<LibraryTvShow>> =
        tvShows.map { it.values.sortedByDescending(LibraryTvShow::updatedAt) }

    override fun observeTvShow(showId: Long): Flow<LibraryTvShow?> = tvShows.map { it[showId] }

    override suspend fun getTvShow(showId: Long): LibraryTvShow? = tvShows.value[showId]

    override suspend fun saveTvShow(show: LibraryTvShow) {
        tvShows.value += show.showId to show
    }

    override suspend fun removeTvShow(showId: Long) {
        tvShows.value -= showId
    }
}
