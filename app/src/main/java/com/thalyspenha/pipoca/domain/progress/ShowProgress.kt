package com.thalyspenha.pipoca.domain.progress

import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.SeasonSummary
import java.time.LocalDate

/** Temporada 0 no TMDB = especiais; nunca contam no progresso (D-037). */
const val SPECIALS_SEASON = 0

private val FINISHED_TMDB_STATUSES = setOf("Ended", "Canceled")

/**
 * Progresso da série: assistidos / disponíveis, sem especiais.
 * Disponível = temporada > 0 e `air_date` <= hoje (episódio sem data ainda não saiu).
 */
data class ShowProgress(
    val watched: Int,
    val available: Int,
    /** Primeiro episódio disponível não assistido, na ordem (temporada, número). */
    val nextEpisode: Episode?,
    /** Próximo episódio ainda não exibido (com data futura ou sem data), para "em dia". */
    val upcomingEpisode: Episode?,
    /** Todas as temporadas regulares já estão no cache; sem isso o total pode estar incompleto. */
    val isComplete: Boolean,
    /** TMDB indica fim da série (`Ended`/`Canceled`). */
    val isShowFinished: Boolean,
) {
    val percent: Int get() = if (available == 0) 0 else watched * 100 / available

    /** Assistiu tudo que já saiu. */
    val allAvailableWatched: Boolean get() = isComplete && available > 0 && watched == available

    /** Regra (a) de D-035: concluída só se tudo assistido **e** a série terminou. */
    val isCompleted: Boolean get() = allAvailableWatched && isShowFinished

    /** Tudo que saiu foi assistido, mas a série segue no ar ("Em dia"). */
    val isCaughtUp: Boolean get() = allAvailableWatched && !isShowFinished

    /** Temporada do próximo episódio. */
    val nextSeason: Int? get() = nextEpisode?.seasonNumber
}

object ShowProgressCalculator {

    /** Já exibido: tem data e ela é hoje ou antes. Sem data = ainda não saiu. */
    fun isAired(episode: Episode, today: LocalDate): Boolean = episode.airDate?.let { !it.isAfter(today) } == true

    /** Conta no progresso: exibido e fora da temporada de especiais. */
    fun isAvailable(episode: Episode, today: LocalDate): Boolean =
        episode.seasonNumber != SPECIALS_SEASON && isAired(episode, today)

    /**
     * @param episodes episódios em cache (qualquer ordem, podem incluir especiais).
     * @param seasons temporadas da série (detalhes TMDB), usadas para saber se o cache está completo.
     * @param watchedEpisodeIds episódios assistidos; os que não estão entre os disponíveis não contam.
     */
    fun calculate(
        episodes: List<Episode>,
        seasons: List<SeasonSummary>,
        watchedEpisodeIds: Set<Long>,
        tmdbStatus: String?,
        today: LocalDate,
    ): ShowProgress {
        val regular = episodes
            .filter { it.seasonNumber != SPECIALS_SEASON }
            .sortedWith(compareBy(Episode::seasonNumber, Episode::episodeNumber))
        val available = regular.filter { isAvailable(it, today) }
        val expectedSeasons = seasons
            .filter { it.seasonNumber != SPECIALS_SEASON && it.episodeCount > 0 }
            .map { it.seasonNumber }
            .toSet()
        val loadedSeasons = regular.map { it.seasonNumber }.toSet()

        return ShowProgress(
            watched = available.count { it.id in watchedEpisodeIds },
            available = available.size,
            nextEpisode = available.firstOrNull { it.id !in watchedEpisodeIds },
            upcomingEpisode = regular.firstOrNull { !isAvailable(it, today) && it.id !in watchedEpisodeIds },
            isComplete = loadedSeasons.containsAll(expectedSeasons),
            isShowFinished = tmdbStatus in FINISHED_TMDB_STATUSES,
        )
    }
}
