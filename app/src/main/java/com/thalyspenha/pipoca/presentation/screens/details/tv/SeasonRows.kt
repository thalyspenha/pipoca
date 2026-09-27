package com.thalyspenha.pipoca.presentation.screens.details.tv

import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.SeasonSummary
import com.thalyspenha.pipoca.domain.progress.SPECIALS_SEASON
import com.thalyspenha.pipoca.domain.progress.ShowProgressCalculator
import java.time.LocalDate

/**
 * Linha da lista de temporadas. `watched`/`aired` só existem quando os episódios da temporada
 * estão em cache; antes disso a linha mostra só o total do TMDB.
 */
data class SeasonRow(
    val seasonNumber: Int,
    val name: String,
    val posterPath: String?,
    val episodeCount: Int,
    val watched: Int? = null,
    val aired: Int? = null,
) {
    val isSpecials: Boolean get() = seasonNumber == SPECIALS_SEASON
    val isFullyWatched: Boolean get() = aired != null && aired > 0 && watched == aired
}

/** Regulares primeiro, especiais por último; temporadas vazias ficam de fora. */
fun buildSeasonRows(
    seasons: List<SeasonSummary>,
    episodes: List<Episode>,
    watchedEpisodeIds: Set<Long>,
    today: LocalDate,
): List<SeasonRow> {
    val bySeason = episodes.groupBy { it.seasonNumber }
    return seasons
        .filter { it.episodeCount > 0 }
        .sortedWith(compareBy({ it.seasonNumber == SPECIALS_SEASON }, { it.seasonNumber }))
        .map { season ->
            val cached = bySeason[season.seasonNumber]
            val aired = cached?.filter { ShowProgressCalculator.isAired(it, today) }
            SeasonRow(
                seasonNumber = season.seasonNumber,
                name = season.name,
                posterPath = season.posterPath,
                episodeCount = season.episodeCount,
                watched = aired?.count { it.id in watchedEpisodeIds },
                aired = aired?.size,
            )
        }
}
