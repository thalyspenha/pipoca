package com.thalyspenha.pipoca.data.mapper

import com.thalyspenha.pipoca.data.local.entity.UserMovieEntity
import com.thalyspenha.pipoca.data.local.entity.UserTvShowEntity
import com.thalyspenha.pipoca.domain.model.LibraryMovie
import com.thalyspenha.pipoca.domain.model.LibraryTvShow
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
