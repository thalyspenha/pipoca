package com.thalyspenha.pipoca.domain.model

import java.time.Instant

/** Episódio marcado como assistido (dado pessoal, independe do cache TMDB). */
data class WatchedEpisode(
    val episodeId: Long,
    val showId: Long,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val watchedAt: Instant,
)
