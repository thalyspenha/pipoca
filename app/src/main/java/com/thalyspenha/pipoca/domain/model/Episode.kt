package com.thalyspenha.pipoca.domain.model

import java.time.LocalDate

/** Episódio vindo do cache TMDB. Temporada 0 = especiais. */
data class Episode(
    val id: Long,
    val showId: Long,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String,
    val overview: String?,
    val stillPath: String?,
    /** Nula quando o TMDB ainda não anunciou a data. */
    val airDate: LocalDate?,
    val runtimeMinutes: Int?,
)
