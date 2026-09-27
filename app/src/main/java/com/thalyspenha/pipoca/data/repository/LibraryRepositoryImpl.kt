package com.thalyspenha.pipoca.data.repository

import com.thalyspenha.pipoca.data.local.dao.UserLibraryDao
import com.thalyspenha.pipoca.data.local.entity.UserEpisodeEntity
import com.thalyspenha.pipoca.data.local.entity.WatchHistoryEntity
import com.thalyspenha.pipoca.data.local.entity.WatchMediaType
import com.thalyspenha.pipoca.data.mapper.toDomain
import com.thalyspenha.pipoca.data.mapper.toEntity
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
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class LibraryRepositoryImpl @Inject constructor(
    private val dao: UserLibraryDao,
) : LibraryRepository {

    override fun observeMovies(): Flow<List<LibraryMovie>> =
        dao.observeMovies().map { list -> list.map { it.toDomain() } }

    override fun observeMovie(movieId: Long): Flow<LibraryMovie?> =
        dao.observeMovie(movieId).map { it?.toDomain() }

    override fun observeMovieItems(): Flow<List<LibraryMovieItem>> =
        dao.observeMoviesWithCache().map { list -> list.map { it.toDomain() } }

    override suspend fun getMovie(movieId: Long): LibraryMovie? = dao.getMovie(movieId)?.toDomain()

    override suspend fun saveMovie(movie: LibraryMovie) = dao.upsertMovie(movie.toEntity())

    override suspend fun saveMovieWatched(movie: LibraryMovie, watchedAt: Instant) =
        dao.upsertMovieWithWatch(
            movie = movie.toEntity(),
            event = WatchHistoryEntity(
                mediaType = WatchMediaType.MOVIE,
                movieId = movie.movieId,
                watchedAt = watchedAt.toEpochMilli(),
            ),
        )

    override suspend fun removeMovie(movieId: Long) = dao.deleteMovieWithHistory(movieId)

    override fun observeTvShows(): Flow<List<LibraryTvShow>> =
        dao.observeTvShows().map { list -> list.map { it.toDomain() } }

    override fun observeTvShow(showId: Long): Flow<LibraryTvShow?> =
        dao.observeTvShow(showId).map { it?.toDomain() }

    override fun observeTvShowItems(): Flow<List<LibraryTvShowItem>> =
        dao.observeTvShowsWithCache().map { list -> list.map { it.toDomain() } }

    override suspend fun getTvShow(showId: Long): LibraryTvShow? = dao.getTvShow(showId)?.toDomain()

    override suspend fun saveTvShow(show: LibraryTvShow) = dao.upsertTvShow(show.toEntity())

    override suspend fun removeTvShow(showId: Long) = dao.deleteTvShowWithEpisodes(showId)

    override fun observeWatchedEpisodes(showId: Long): Flow<List<WatchedEpisode>> =
        dao.observeWatchedEpisodes(showId).map { list -> list.map { it.toDomain() } }

    override fun observeMovieList(filter: MovieLibraryFilter, sort: LibrarySort): Flow<List<LibraryMovieItem>> =
        dao.observeMovieList(filter.status?.name, filter.favoritesOnly, sort.name).map { list -> list.map { it.toDomain() } }

    override fun observeTvShowList(filter: TvShowLibraryFilter, sort: LibrarySort): Flow<List<LibraryTvShowItem>> =
        dao.observeTvShowList(filter.status?.name, filter.favoritesOnly, sort.name).map { list -> list.map { it.toDomain() } }

    override fun observeMovieLibraryCounts(): Flow<MovieLibraryCounts> =
        dao.observeMovieLibraryCounts().map { MovieLibraryCounts(it.all, it.wantToWatch, it.watched, it.favorites) }

    override fun observeTvShowLibraryCounts(): Flow<TvShowLibraryCounts> =
        dao.observeTvShowLibraryCounts().map {
            TvShowLibraryCounts(it.all, it.wantToWatch, it.watching, it.completed, it.paused, it.dropped, it.favorites)
        }

    override fun observeLibraryShowsWatchedEpisodes(): Flow<List<WatchedEpisode>> =
        dao.observeLibraryShowsWatchedEpisodes().map { list -> list.map { it.toDomain() } }

    override fun observeHistory(): Flow<List<HistoryEntry>> =
        dao.observeHistory().map { list -> list.map { it.toDomain() } }

    override fun observeWatchingShowsWatchedEpisodes(): Flow<List<WatchedEpisode>> =
        dao.observeWatchingShowsWatchedEpisodes().map { list -> list.map { it.toDomain() } }

    override suspend fun markEpisodesWatched(episodes: List<Episode>, watchedAt: Instant) {
        if (episodes.isEmpty()) return
        val millis = watchedAt.toEpochMilli()
        dao.insertWatchedEpisodesWithHistory(
            episodes = episodes.map {
                UserEpisodeEntity(it.id, it.showId, it.seasonNumber, it.episodeNumber, watchedAt = millis)
            },
            events = episodes.map {
                WatchHistoryEntity(
                    mediaType = WatchMediaType.EPISODE,
                    showId = it.showId,
                    episodeId = it.id,
                    watchedAt = millis,
                )
            },
        )
    }

    override suspend fun unmarkEpisodes(episodeIds: List<Long>) {
        if (episodeIds.isNotEmpty()) dao.deleteWatchedEpisodesWithHistory(episodeIds)
    }
}
