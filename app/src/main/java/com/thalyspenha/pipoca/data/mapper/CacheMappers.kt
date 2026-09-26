package com.thalyspenha.pipoca.data.mapper

import com.thalyspenha.pipoca.data.local.dao.MovieCacheBundle
import com.thalyspenha.pipoca.data.local.dao.TvShowCacheBundle
import com.thalyspenha.pipoca.data.local.entity.CastRow
import com.thalyspenha.pipoca.data.local.entity.CreditMediaType
import com.thalyspenha.pipoca.data.local.entity.TmdbCreditEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbGenreEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbMovieEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbPersonEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbSeasonEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbTvShowEntity
import com.thalyspenha.pipoca.data.remote.dto.MovieDetailsDto
import com.thalyspenha.pipoca.data.remote.dto.TvShowDetailsDto
import com.thalyspenha.pipoca.domain.model.CastMember
import com.thalyspenha.pipoca.domain.model.Genre
import com.thalyspenha.pipoca.domain.model.MovieDetails
import com.thalyspenha.pipoca.domain.model.SeasonSummary
import com.thalyspenha.pipoca.domain.model.TvShowDetails

// DTO → Entity passa pelo modelo de domínio para reaproveitar as regras de TmdbMappers
// (datas, strings vazias, diretores, elenco principal).

fun MovieDetailsDto.toCacheBundle(fetchedAt: Long): MovieCacheBundle {
    val movie = toDomain()
    val cast = movie.cast.distinctBy { it.id }
    return MovieCacheBundle(
        movie = TmdbMovieEntity(
            id = movie.id,
            title = movie.title,
            originalTitle = movie.originalTitle,
            overview = movie.overview,
            posterPath = movie.posterPath,
            backdropPath = movie.backdropPath,
            releaseDate = movie.releaseDate,
            runtimeMinutes = movie.runtimeMinutes,
            voteAverage = movie.voteAverage,
            directors = movie.directors,
            fetchedAt = fetchedAt,
        ),
        genres = movie.genres.map { it.toEntity() },
        persons = cast.map { it.toPersonEntity() },
        credits = cast.map { it.toCreditEntity(CreditMediaType.MOVIE, movie.id) },
    )
}

fun TvShowDetailsDto.toCacheBundle(fetchedAt: Long): TvShowCacheBundle {
    val show = toDomain()
    val cast = show.cast.distinctBy { it.id }
    return TvShowCacheBundle(
        show = TmdbTvShowEntity(
            id = show.id,
            name = show.name,
            originalName = show.originalName,
            overview = show.overview,
            posterPath = show.posterPath,
            backdropPath = show.backdropPath,
            firstAirDate = show.firstAirDate,
            tmdbStatus = show.tmdbStatus,
            numberOfSeasons = show.numberOfSeasons,
            numberOfEpisodes = show.numberOfEpisodes,
            episodeRunTime = show.episodeRunTime,
            voteAverage = show.voteAverage,
            creators = show.creators,
            fetchedAt = fetchedAt,
        ),
        genres = show.genres.map { it.toEntity() },
        seasons = show.seasons.map { it.toEntity(show.id, fetchedAt) },
        persons = cast.map { it.toPersonEntity() },
        credits = cast.map { it.toCreditEntity(CreditMediaType.TV, show.id) },
    )
}

fun TmdbMovieEntity.toDomain(genres: List<TmdbGenreEntity>, cast: List<CastRow>) = MovieDetails(
    id = id,
    title = title,
    originalTitle = originalTitle,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    runtimeMinutes = runtimeMinutes,
    voteAverage = voteAverage,
    genres = genres.map { it.toDomain() },
    directors = directors,
    cast = cast.map { it.toDomain() },
)

fun TmdbTvShowEntity.toDomain(
    genres: List<TmdbGenreEntity>,
    seasons: List<TmdbSeasonEntity>,
    cast: List<CastRow>,
) = TvShowDetails(
    id = id,
    name = name,
    originalName = originalName,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    firstAirDate = firstAirDate,
    tmdbStatus = tmdbStatus,
    numberOfSeasons = numberOfSeasons,
    numberOfEpisodes = numberOfEpisodes,
    episodeRunTime = episodeRunTime,
    voteAverage = voteAverage,
    genres = genres.map { it.toDomain() },
    creators = creators,
    seasons = seasons.map { it.toDomain() },
    cast = cast.map { it.toDomain() },
)

private fun Genre.toEntity() = TmdbGenreEntity(id = id, name = name)

private fun TmdbGenreEntity.toDomain() = Genre(id = id, name = name)

private fun SeasonSummary.toEntity(showId: Long, fetchedAt: Long) = TmdbSeasonEntity(
    id = id,
    showId = showId,
    seasonNumber = seasonNumber,
    name = name,
    overview = overview,
    posterPath = posterPath,
    airDate = airDate,
    episodeCount = episodeCount,
    fetchedAt = fetchedAt,
)

private fun TmdbSeasonEntity.toDomain() = SeasonSummary(
    id = id,
    seasonNumber = seasonNumber,
    name = name,
    overview = overview,
    posterPath = posterPath,
    airDate = airDate,
    episodeCount = episodeCount,
)

private fun CastMember.toPersonEntity() = TmdbPersonEntity(id = id, name = name, profilePath = profilePath)

private fun CastMember.toCreditEntity(mediaType: CreditMediaType, mediaId: Long) = TmdbCreditEntity(
    mediaType = mediaType,
    mediaId = mediaId,
    personId = id,
    character = character,
    order = order,
)

private fun CastRow.toDomain() = CastMember(
    id = id,
    name = name,
    character = character,
    profilePath = profilePath,
    order = order,
)
