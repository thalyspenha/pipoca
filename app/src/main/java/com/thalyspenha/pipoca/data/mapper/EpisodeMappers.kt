package com.thalyspenha.pipoca.data.mapper

import com.thalyspenha.pipoca.data.local.entity.TmdbEpisodeEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbSeasonEntity
import com.thalyspenha.pipoca.data.remote.dto.SeasonDetailsDto
import com.thalyspenha.pipoca.domain.model.Episode

/** Temporada + episódios prontos para `TmdbEpisodeDao.saveSeason`. */
data class SeasonCacheBundle(val season: TmdbSeasonEntity, val episodes: List<TmdbEpisodeEntity>)

fun SeasonDetailsDto.toCacheBundle(showId: Long, fetchedAt: Long) = SeasonCacheBundle(
    season = TmdbSeasonEntity(
        id = id,
        showId = showId,
        seasonNumber = seasonNumber,
        name = name,
        overview = overview.nullIfBlank(),
        posterPath = posterPath.nullIfBlank(),
        airDate = airDate.toLocalDateOrNull(),
        episodeCount = episodes.size,
        fetchedAt = fetchedAt,
    ),
    episodes = episodes.map { episode ->
        TmdbEpisodeEntity(
            id = episode.id,
            showId = showId,
            seasonId = id,
            seasonNumber = episode.seasonNumber,
            episodeNumber = episode.episodeNumber,
            name = episode.name,
            overview = episode.overview.nullIfBlank(),
            stillPath = episode.stillPath.nullIfBlank(),
            airDate = episode.airDate.toLocalDateOrNull(),
            runtimeMinutes = episode.runtime?.takeIf { it > 0 },
            fetchedAt = fetchedAt,
        )
    },
)

fun TmdbEpisodeEntity.toDomain() = Episode(
    id = id,
    showId = showId,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    name = name,
    overview = overview,
    stillPath = stillPath,
    airDate = airDate,
    runtimeMinutes = runtimeMinutes,
)
