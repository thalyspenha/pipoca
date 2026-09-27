package com.thalyspenha.pipoca.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `movie/{id}?append_to_response=credits`. */
@Serializable
data class MovieDetailsDto(
    val id: Long,
    val title: String = "",
    @SerialName("original_title") val originalTitle: String = "",
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val runtime: Int? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    val genres: List<GenreDto> = emptyList(),
    val credits: CreditsDto? = null,
)

/** `tv/{id}?append_to_response=credits`. */
@Serializable
data class TvShowDetailsDto(
    val id: Long,
    val name: String = "",
    @SerialName("original_name") val originalName: String = "",
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    /** Status no TMDB ("Returning Series", "Ended"...), não é status pessoal. */
    val status: String? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    /** Frequentemente vazio em séries recentes. */
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    @SerialName("vote_average") val voteAverage: Double? = null,
    val genres: List<GenreDto> = emptyList(),
    val seasons: List<SeasonSummaryDto> = emptyList(),
    @SerialName("created_by") val createdBy: List<CreatorDto> = emptyList(),
    val credits: CreditsDto? = null,
)

@Serializable
data class GenreDto(
    val id: Long,
    val name: String = "",
)

@Serializable
data class SeasonSummaryDto(
    val id: Long,
    @SerialName("season_number") val seasonNumber: Int,
    val name: String = "",
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("episode_count") val episodeCount: Int = 0,
)

@Serializable
data class CreatorDto(
    val id: Long,
    val name: String = "",
)

@Serializable
data class CreditsDto(
    val cast: List<CastDto> = emptyList(),
    val crew: List<CrewDto> = emptyList(),
)

@Serializable
data class CastDto(
    val id: Long,
    val name: String = "",
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    val order: Int = Int.MAX_VALUE,
)

@Serializable
data class CrewDto(
    val id: Long,
    val name: String = "",
    val job: String? = null,
    val department: String? = null,
)

/** `tv/{id}/season/{n}`: temporada com a lista de episódios. */
@Serializable
data class SeasonDetailsDto(
    val id: Long,
    @SerialName("season_number") val seasonNumber: Int,
    val name: String = "",
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    val episodes: List<EpisodeDto> = emptyList(),
)

/** Episódio. Futuros podem vir sem `air_date`, `runtime` e `still_path`. */
@Serializable
data class EpisodeDto(
    val id: Long,
    @SerialName("episode_number") val episodeNumber: Int,
    @SerialName("season_number") val seasonNumber: Int,
    val name: String = "",
    val overview: String? = null,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    val runtime: Int? = null,
)
