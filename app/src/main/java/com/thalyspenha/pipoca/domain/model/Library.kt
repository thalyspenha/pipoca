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

/** Filme da biblioteca com dados do cache TMDB para exibir. `title` nulo = ainda sem cache. */
data class LibraryMovieItem(
    val movie: LibraryMovie,
    val title: String?,
    val posterPath: String?,
    val year: Int?,
)

data class LibraryTvShowItem(
    val show: LibraryTvShow,
    val name: String?,
    val posterPath: String?,
    val year: Int?,
)
