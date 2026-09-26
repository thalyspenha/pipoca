package com.thalyspenha.pipoca.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Página de resultados de `search/movie` e `search/tv`. */
@Serializable
data class PagedResponseDto<T>(
    val page: Int = 1,
    val results: List<T> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 0,
    @SerialName("total_results") val totalResults: Int = 0,
)

@Serializable
data class MovieSummaryDto(
    val id: Long,
    val title: String = "",
    @SerialName("original_title") val originalTitle: String = "",
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    /** `yyyy-MM-dd`; pode vir vazio. */
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("genre_ids") val genreIds: List<Long> = emptyList(),
    @SerialName("vote_average") val voteAverage: Double? = null,
)

@Serializable
data class TvShowSummaryDto(
    val id: Long,
    val name: String = "",
    @SerialName("original_name") val originalName: String = "",
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    /** `yyyy-MM-dd`; pode vir vazio. */
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("genre_ids") val genreIds: List<Long> = emptyList(),
    @SerialName("vote_average") val voteAverage: Double? = null,
)
