package com.thalyspenha.pipoca.data.mapper

import com.thalyspenha.pipoca.data.local.dao.UserMovieWithCache
import com.thalyspenha.pipoca.data.local.dao.WatchHistoryWithCache
import com.thalyspenha.pipoca.data.local.entity.WatchMediaType
import com.thalyspenha.pipoca.domain.model.HistoryEntry
import com.thalyspenha.pipoca.domain.model.HistoryType
import com.thalyspenha.pipoca.data.local.dao.UserTvShowWithCache
import com.thalyspenha.pipoca.data.local.entity.UserEpisodeEntity
import com.thalyspenha.pipoca.data.local.entity.UserMovieEntity
import com.thalyspenha.pipoca.data.local.entity.UserTvShowEntity
import com.thalyspenha.pipoca.domain.model.LibraryMovie
import com.thalyspenha.pipoca.domain.model.LibraryMovieItem
import com.thalyspenha.pipoca.domain.model.LibraryTvShow
import com.thalyspenha.pipoca.domain.model.LibraryTvShowItem
import com.thalyspenha.pipoca.domain.model.WatchedEpisode
import java.time.Instant

fun UserMovieEntity.toDomain() = LibraryMovie(
    movieId = movieId,
    status = status,
    isFavorite = isFavorite,
    rating = rating,
    notes = notes,
    addedAt = Instant.ofEpochMilli(addedAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

fun LibraryMovie.toEntity() = UserMovieEntity(
    movieId = movieId,
    status = status,
    isFavorite = isFavorite,
    rating = rating,
    notes = notes,
    addedAt = addedAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

fun UserTvShowEntity.toDomain() = LibraryTvShow(
    showId = showId,
    status = status,
    isFavorite = isFavorite,
    rating = rating,
    notes = notes,
    addedAt = Instant.ofEpochMilli(addedAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

fun LibraryTvShow.toEntity() = UserTvShowEntity(
    showId = showId,
    status = status,
    isFavorite = isFavorite,
    rating = rating,
    notes = notes,
    addedAt = addedAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

fun UserMovieWithCache.toDomain() = LibraryMovieItem(
    movie = movie.toDomain(),
    title = title,
    posterPath = posterPath,
    year = releaseDate?.year,
)

fun UserTvShowWithCache.toDomain() = LibraryTvShowItem(
    show = show.toDomain(),
    name = name,
    posterPath = posterPath,
    year = firstAirDate?.year,
    tmdbStatus = tmdbStatus,
    lastWatchedAt = lastWatchedAt?.let(Instant::ofEpochMilli),
)

fun UserEpisodeEntity.toDomain() = WatchedEpisode(
    episodeId = episodeId,
    showId = showId,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    watchedAt = Instant.ofEpochMilli(watchedAt),
)

fun WatchHistoryWithCache.toDomain(): HistoryEntry = when (event.mediaType) {
    WatchMediaType.MOVIE -> HistoryEntry(
        id = event.id,
        type = HistoryType.MOVIE,
        tmdbId = checkNotNull(event.movieId),
        title = movieTitle,
        posterPath = posterPath,
        watchedAt = Instant.ofEpochMilli(event.watchedAt),
    )
    WatchMediaType.EPISODE -> HistoryEntry(
        id = event.id,
        type = HistoryType.EPISODE,
        tmdbId = checkNotNull(event.showId),
        title = showName,
        posterPath = posterPath,
        episodeName = episodeName,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        watchedAt = Instant.ofEpochMilli(event.watchedAt),
    )
}
