package com.thalyspenha.pipoca.domain.stats

import com.thalyspenha.pipoca.domain.model.MediaFormat
import com.thalyspenha.pipoca.domain.model.PERSONAL_RATING_RANGE
import java.time.Instant
import java.time.ZoneId

data class MovieStats(val watched: Int, val wantToWatch: Int, val inCollection: Int, val favorites: Int)

data class TvShowStats(val total: Int, val watching: Int, val completed: Int, val wantToWatch: Int)

data class EpisodeStats(val total: Int, val thisMonth: Int, val thisYear: Int)

/**
 * Tempo aproximado (D-048). `views` = visualizações (reassistir conta); `withoutRuntime` = das
 * visualizações, quantas ficaram de fora por falta de duração (ou de cache).
 */
data class WatchTime(val minutes: Long, val views: Int, val withoutRuntime: Int) {
    val hours: Long get() = minutes / MINUTES_PER_HOUR
    val days: Double get() = minutes / MINUTES_PER_DAY.toDouble()

    private companion object {
        const val MINUTES_PER_HOUR = 60L
        const val MINUTES_PER_DAY = 24L * 60L
    }
}

data class GenreShare(val name: String, val count: Int, val percent: Int)

data class RatingBucket(val rating: Int, val count: Int)

data class FormatCount(val format: MediaFormat, val count: Int)

data class Statistics(
    val movies: MovieStats,
    val tvShows: TvShowStats,
    val episodes: EpisodeStats,
    val watchTime: WatchTime,
    val genres: List<GenreShare>,
    /** Sempre 1..10, com zero onde não há nota. */
    val ratings: List<RatingBucket>,
    /** Sempre os 5 formatos, na ordem do enum. */
    val collection: List<FormatCount>,
) {
    val ratedCount: Int get() = ratings.sumOf { it.count }

    /** Média das notas pessoais, ou nulo sem notas. */
    val averageRating: Double?
        get() = if (ratedCount == 0) null else ratings.sumOf { it.rating * it.count }.toDouble() / ratedCount
}

/** Cálculos puros sobre o resultado das consultas (testáveis sem banco). */
object StatsCalculator {

    /** Participação de cada gênero sobre o total de ocorrências (soma ~100, arredondado para baixo). */
    fun genreShares(counts: List<Pair<String, Int>>): List<GenreShare> {
        val total = counts.sumOf { it.second }
        if (total == 0) return emptyList()
        return counts.map { (name, count) -> GenreShare(name, count, count * 100 / total) }
    }

    fun ratingBuckets(counts: Map<Int, Int>): List<RatingBucket> =
        PERSONAL_RATING_RANGE.map { RatingBucket(it, counts[it] ?: 0) }

    fun formatCounts(counts: Map<MediaFormat, Int>): List<FormatCount> =
        MediaFormat.entries.map { FormatCount(it, counts[it] ?: 0) }

    /** Início do mês e do ano atuais no fuso dado, em epoch millis. */
    fun periodStarts(now: Instant, zone: ZoneId): Pair<Long, Long> {
        val today = now.atZone(zone).toLocalDate()
        val month = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val year = today.withDayOfYear(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return month to year
    }
}
