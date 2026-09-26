package com.thalyspenha.pipoca.data.mapper

import com.thalyspenha.pipoca.data.remote.dto.CastDto
import com.thalyspenha.pipoca.data.remote.dto.CreditsDto
import com.thalyspenha.pipoca.data.remote.dto.GenreDto
import com.thalyspenha.pipoca.data.remote.dto.MovieDetailsDto
import com.thalyspenha.pipoca.data.remote.dto.MovieSummaryDto
import com.thalyspenha.pipoca.data.remote.dto.PagedResponseDto
import com.thalyspenha.pipoca.data.remote.dto.SeasonSummaryDto
import com.thalyspenha.pipoca.data.remote.dto.TvShowDetailsDto
import com.thalyspenha.pipoca.data.remote.dto.TvShowSummaryDto
import com.thalyspenha.pipoca.domain.model.CastMember
import com.thalyspenha.pipoca.domain.model.Genre
import com.thalyspenha.pipoca.domain.model.MovieDetails
import com.thalyspenha.pipoca.domain.model.MovieSummary
import com.thalyspenha.pipoca.domain.model.SearchPage
import com.thalyspenha.pipoca.domain.model.SeasonSummary
import com.thalyspenha.pipoca.domain.model.TvShowDetails
import com.thalyspenha.pipoca.domain.model.TvShowSummary
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Só o elenco principal é mantido (DATABASE.md). */
const val MAX_CAST = 15

private const val DIRECTOR_JOB = "Director"

fun <T, R> PagedResponseDto<T>.toSearchPage(mapItem: (T) -> R): SearchPage<R> = SearchPage(
    page = page,
    totalPages = totalPages,
    totalResults = totalResults,
    items = results.map(mapItem),
)

fun MovieSummaryDto.toDomain() = MovieSummary(
    id = id,
    title = title,
    originalTitle = originalTitle,
    overview = overview.nullIfBlank(),
    posterPath = posterPath.nullIfBlank(),
    backdropPath = backdropPath.nullIfBlank(),
    releaseDate = releaseDate.toLocalDateOrNull(),
    voteAverage = voteAverage,
)

fun TvShowSummaryDto.toDomain() = TvShowSummary(
    id = id,
    name = name,
    originalName = originalName,
    overview = overview.nullIfBlank(),
    posterPath = posterPath.nullIfBlank(),
    backdropPath = backdropPath.nullIfBlank(),
    firstAirDate = firstAirDate.toLocalDateOrNull(),
    voteAverage = voteAverage,
)

fun MovieDetailsDto.toDomain() = MovieDetails(
    id = id,
    title = title,
    originalTitle = originalTitle,
    overview = overview.nullIfBlank(),
    posterPath = posterPath.nullIfBlank(),
    backdropPath = backdropPath.nullIfBlank(),
    releaseDate = releaseDate.toLocalDateOrNull(),
    runtimeMinutes = runtime?.takeIf { it > 0 },
    voteAverage = voteAverage,
    genres = genres.map { it.toDomain() },
    directors = credits.directors(),
    cast = credits.mainCast(),
)

fun TvShowDetailsDto.toDomain() = TvShowDetails(
    id = id,
    name = name,
    originalName = originalName,
    overview = overview.nullIfBlank(),
    posterPath = posterPath.nullIfBlank(),
    backdropPath = backdropPath.nullIfBlank(),
    firstAirDate = firstAirDate.toLocalDateOrNull(),
    tmdbStatus = status.nullIfBlank(),
    numberOfSeasons = numberOfSeasons,
    numberOfEpisodes = numberOfEpisodes,
    episodeRunTime = episodeRunTime.filter { it > 0 }.takeIf { it.isNotEmpty() }?.average()?.toInt(),
    voteAverage = voteAverage,
    genres = genres.map { it.toDomain() },
    creators = createdBy.map { it.name },
    seasons = seasons.map { it.toDomain() }.sortedBy { it.seasonNumber },
    cast = credits.mainCast(),
)

fun GenreDto.toDomain() = Genre(id = id, name = name)

fun SeasonSummaryDto.toDomain() = SeasonSummary(
    id = id,
    seasonNumber = seasonNumber,
    name = name,
    overview = overview.nullIfBlank(),
    posterPath = posterPath.nullIfBlank(),
    airDate = airDate.toLocalDateOrNull(),
    episodeCount = episodeCount,
)

fun CastDto.toDomain() = CastMember(
    id = id,
    name = name,
    character = character.nullIfBlank(),
    profilePath = profilePath.nullIfBlank(),
    order = order,
)

private fun CreditsDto?.directors(): List<String> =
    this?.crew.orEmpty().filter { it.job == DIRECTOR_JOB }.map { it.name }.distinct()

private fun CreditsDto?.mainCast(): List<CastMember> =
    this?.cast.orEmpty().sortedBy { it.order }.take(MAX_CAST).map { it.toDomain() }

/** TMDB manda `""` quando não há data; data inválida também vira `null`. */
internal fun String?.toLocalDateOrNull(): LocalDate? {
    if (isNullOrBlank()) return null
    return try {
        LocalDate.parse(this)
    } catch (_: DateTimeParseException) {
        null
    }
}

internal fun String?.nullIfBlank(): String? = this?.takeIf { it.isNotBlank() }
