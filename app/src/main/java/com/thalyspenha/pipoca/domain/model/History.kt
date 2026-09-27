package com.thalyspenha.pipoca.domain.model

import java.time.Instant

enum class HistoryType { MOVIE, EPISODE }

/**
 * Uma visualização (filme ou episódio). Campos de exibição vêm do cache TMDB e podem ser nulos;
 * temporada/número do episódio caem para `user_episode` quando não há cache.
 */
data class HistoryEntry(
    val id: Long,
    val type: HistoryType,
    /** Filme: id do filme; episódio: id da série. */
    val tmdbId: Long,
    val title: String?,
    val posterPath: String?,
    val episodeName: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val watchedAt: Instant,
)

/** Filtros básicos do histórico (`fase8.md`). */
enum class HistoryFilter {
    ALL, MOVIES, TV_SHOWS;

    fun matches(entry: HistoryEntry): Boolean = when (this) {
        ALL -> true
        MOVIES -> entry.type == HistoryType.MOVIE
        TV_SHOWS -> entry.type == HistoryType.EPISODE
    }
}
