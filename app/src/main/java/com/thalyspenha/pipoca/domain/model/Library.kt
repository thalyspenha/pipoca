package com.thalyspenha.pipoca.domain.model

import java.time.Instant

/** Nota pessoal aceita (DATABASE.md). */
val PERSONAL_RATING_RANGE = 1..10

/** Filme na biblioteca do usuário. Dados do TMDB (título, poster) vêm do cache, não daqui. */
data class LibraryMovie(
    val movieId: Long,
    val status: MovieStatus,
    val isFavorite: Boolean = false,
    val rating: Int? = null,
    val notes: String? = null,
    val addedAt: Instant,
    val updatedAt: Instant,
)

/** Série na biblioteca do usuário. */
data class LibraryTvShow(
    val showId: Long,
    val status: TvShowStatus,
    val isFavorite: Boolean = false,
    val rating: Int? = null,
    val notes: String? = null,
    val addedAt: Instant,
    val updatedAt: Instant,
)
