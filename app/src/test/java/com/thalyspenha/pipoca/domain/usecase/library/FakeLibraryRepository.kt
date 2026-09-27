package com.thalyspenha.pipoca.domain.usecase.library

import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.LibraryMovie
import com.thalyspenha.pipoca.domain.model.LibraryMovieItem
import com.thalyspenha.pipoca.domain.model.LibraryTvShow
import com.thalyspenha.pipoca.domain.model.LibraryTvShowItem
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.model.WatchedEpisode
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
    val tvShowStatuses = mutableMapOf<Long, String>()

    override fun observeMovieItems(): Flow<List<LibraryMovieItem>> =
        combine(observeMovies(), movieTitles) { list, titles ->
            list.map { LibraryMovieItem(it, titles[it.movieId], posterPath = null, year = null) }
        }

    override fun observeTvShowItems(): Flow<List<LibraryTvShowItem>> =
        combine(observeTvShows(), tvShowNames) { list, names ->
            list.map { LibraryTvShowItem(it, names[it.showId], posterPath = null, year = null, tmdbStatus = tvShowStatuses[it.showId]) }
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
        watchedEpisodes.value = watchedEpisodes.value.filterValues { it.showId != showId }
        episodeWatches.keys.removeAll { key -> key !in watchedEpisodes.value }
    }

    /** Episódios assistidos por id; `episodeWatches` simula os eventos de histórico por episódio. */
    val watchedEpisodes = MutableStateFlow<Map<Long, WatchedEpisode>>(emptyMap())
    val episodeWatches = mutableMapOf<Long, Int>()

    override fun observeWatchedEpisodes(showId: Long): Flow<List<WatchedEpisode>> =
        watchedEpisodes.map { all -> all.values.filter { it.showId == showId } }

    /** Status das séries vem de `tvShows`; só as `WATCHING` entram. */
    override fun observeWatchingShowsWatchedEpisodes(): Flow<List<WatchedEpisode>> =
        combine(watchedEpisodes, tvShows) { watched, shows ->
            watched.values.filter { shows[it.showId]?.status == TvShowStatus.WATCHING }
        }

    override suspend fun markEpisodesWatched(episodes: List<Episode>, watchedAt: Instant) {
        watchedEpisodes.value += episodes.associate {
            it.id to WatchedEpisode(it.id, it.showId, it.seasonNumber, it.episodeNumber, watchedAt)
        }
        episodes.forEach { episodeWatches[it.id] = (episodeWatches[it.id] ?: 0) + 1 }
    }

    override suspend fun unmarkEpisodes(episodeIds: List<Long>) {
        watchedEpisodes.value -= episodeIds.toSet()
        episodeIds.forEach { episodeWatches.remove(it) }
    }
}
