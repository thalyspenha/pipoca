package com.thalyspenha.pipoca.domain.usecase.library

import com.thalyspenha.pipoca.domain.model.LibraryMovie
import com.thalyspenha.pipoca.domain.model.LibraryTvShow
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/** Biblioteca em memória; `movieWatches` guarda o histórico de visualizações por filme. */
class FakeLibraryRepository : LibraryRepository {
    val movies = MutableStateFlow<Map<Long, LibraryMovie>>(emptyMap())
    val tvShows = MutableStateFlow<Map<Long, LibraryTvShow>>(emptyMap())
    val movieWatches = mutableMapOf<Long, MutableList<Instant>>()

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
