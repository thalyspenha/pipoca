package com.thalyspenha.pipoca.data.repository

import com.thalyspenha.pipoca.data.local.dao.StatsDao
import com.thalyspenha.pipoca.domain.repository.StatsRepository
import com.thalyspenha.pipoca.domain.stats.EpisodeStats
import com.thalyspenha.pipoca.domain.stats.MovieStats
import com.thalyspenha.pipoca.domain.stats.Statistics
import com.thalyspenha.pipoca.domain.stats.StatsCalculator
import com.thalyspenha.pipoca.domain.stats.TvShowStats
import com.thalyspenha.pipoca.domain.stats.WatchTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class StatsRepositoryImpl @Inject constructor(
    private val dao: StatsDao,
) : StatsRepository {

    override fun observeStatistics(monthStart: Long, yearStart: Long): Flow<Statistics> {
        val counts = combine(
            dao.observeMovieCounts(),
            dao.observeTvShowCounts(),
            dao.observeEpisodeCounts(monthStart, yearStart),
            dao.observeWatchTime(),
        ) { movies, shows, episodes, time ->
            Statistics(
                movies = MovieStats(movies.watched, movies.wantToWatch, movies.inCollection, movies.favorites),
                tvShows = TvShowStats(shows.total, shows.watching, shows.completed, shows.wantToWatch),
                episodes = EpisodeStats(episodes.total, episodes.thisMonth, episodes.thisYear),
                watchTime = WatchTime(time.minutes, time.views, time.withoutRuntime),
                genres = emptyList(),
                ratings = emptyList(),
                collection = emptyList(),
            )
        }
        return combine(
            counts,
            dao.observeGenreCounts(),
            dao.observeRatingCounts(),
            dao.observeFormatCounts(),
        ) { stats, genres, ratings, formats ->
            stats.copy(
                genres = StatsCalculator.genreShares(genres.map { it.name to it.count }),
                ratings = StatsCalculator.ratingBuckets(ratings.associate { it.rating to it.count }),
                collection = StatsCalculator.formatCounts(formats.associate { it.format to it.count }),
            )
        }
    }
}
