package com.thalyspenha.pipoca.domain.model

import java.time.LocalDate

/** Página de resultados de busca. */
data class SearchPage<T>(
    val page: Int,
    val totalPages: Int,
    val totalResults: Int,
    val items: List<T>,
) {
    val hasNextPage: Boolean get() = page < totalPages
}

data class MovieSummary(
    val id: Long,
    val title: String,
    val originalTitle: String,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: LocalDate?,
    val voteAverage: Double?,
)

data class TvShowSummary(
    val id: Long,
    val name: String,
    val originalName: String,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val firstAirDate: LocalDate?,
    val voteAverage: Double?,
)

data class MovieDetails(
    val id: Long,
    val title: String,
    val originalTitle: String,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: LocalDate?,
    val runtimeMinutes: Int?,
    val voteAverage: Double?,
    val genres: List<Genre>,
    val directors: List<String>,
    val cast: List<CastMember>,
)

data class TvShowDetails(
    val id: Long,
    val name: String,
    val originalName: String,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val firstAirDate: LocalDate?,
    /** Status no TMDB ("Returning Series", "Ended"...), não é status pessoal. */
    val tmdbStatus: String?,
    val numberOfSeasons: Int?,
    val numberOfEpisodes: Int?,
    /** Duração média do episódio em minutos. */
    val episodeRunTime: Int?,
    val voteAverage: Double?,
    val genres: List<Genre>,
    val creators: List<String>,
    val seasons: List<SeasonSummary>,
    val cast: List<CastMember>,
)

data class Genre(val id: Long, val name: String)

data class CastMember(
    val id: Long,
    val name: String,
    val character: String?,
    val profilePath: String?,
    val order: Int,
)

data class SeasonSummary(
    val id: Long,
    val seasonNumber: Int,
    val name: String,
    val overview: String?,
    val posterPath: String?,
    val airDate: LocalDate?,
    val episodeCount: Int,
)
