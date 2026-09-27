package com.thalyspenha.pipoca.domain.stats

import com.thalyspenha.pipoca.domain.model.MediaFormat
import com.thalyspenha.pipoca.domain.repository.StatsRepository
import com.thalyspenha.pipoca.domain.usecase.stats.ObserveStatisticsUseCase
import com.thalyspenha.pipoca.data.repository.MutableClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class StatsCalculatorTest {

    @Test
    fun `tempo em horas e dias equivalentes`() {
        val time = WatchTime(minutes = 3_000, views = 40, withoutRuntime = 2)

        assertEquals(50L, time.hours)
        assertEquals(3_000 / 1_440.0, time.days, 0.0001)
    }

    @Test
    fun `tempo zero`() {
        val time = WatchTime(0, 0, 0)

        assertEquals(0L, time.hours)
        assertEquals(0.0, time.days, 0.0)
    }

    @Test
    fun `participacao de generos arredonda para baixo e mantem a ordem`() {
        val shares = StatsCalculator.genreShares(listOf("Drama" to 3, "Ação" to 2, "Crime" to 1))

        assertEquals(listOf("Drama", "Ação", "Crime"), shares.map { it.name })
        assertEquals(listOf(50, 33, 16), shares.map { it.percent })
    }

    @Test
    fun `sem generos`() {
        assertEquals(emptyList<GenreShare>(), StatsCalculator.genreShares(emptyList()))
    }

    @Test
    fun `notas sempre de 1 a 10 com zeros e media`() {
        val buckets = StatsCalculator.ratingBuckets(mapOf(8 to 2, 10 to 1))

        assertEquals((1..10).toList(), buckets.map { it.rating })
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 0, 2, 0, 1), buckets.map { it.count })
        val stats = emptyStats().copy(ratings = buckets)
        assertEquals(3, stats.ratedCount)
        assertEquals(26.0 / 3, stats.averageRating!!, 0.0001)
    }

    @Test
    fun `sem notas a media e nula`() {
        assertNull(emptyStats().copy(ratings = StatsCalculator.ratingBuckets(emptyMap())).averageRating)
    }

    @Test
    fun `colecao sempre com os cinco formatos em ordem`() {
        val counts = StatsCalculator.formatCounts(mapOf(MediaFormat.DVD to 3, MediaFormat.UHD_4K_BLURAY to 1))

        assertEquals(MediaFormat.entries.toList(), counts.map { it.format })
        assertEquals(listOf(1, 0, 3, 0, 0), counts.map { it.count })
    }

    @Test
    fun `inicio do mes e do ano no fuso do aparelho`() {
        val zone = ZoneId.of("America/Sao_Paulo")
        // 01/01/2027 01:00 em UTC ainda é 31/12/2026 em São Paulo.
        val (month, year) = StatsCalculator.periodStarts(Instant.parse("2027-01-01T01:00:00Z"), zone)

        assertEquals(ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli(), month)
        assertEquals(ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli(), year)
    }

    @Test
    fun `use case passa os limites do calendario atual ao repositorio`() {
        val repository = CapturingStatsRepository()
        val zone = ZoneId.of("UTC")
        ObserveStatisticsUseCase(repository, MutableClock(Instant.parse("2026-09-26T12:00:00Z")), zone)()

        assertEquals(Instant.parse("2026-09-01T00:00:00Z").toEpochMilli(), repository.monthStart)
        assertEquals(Instant.parse("2026-01-01T00:00:00Z").toEpochMilli(), repository.yearStart)
    }

    private fun emptyStats() = Statistics(
        movies = MovieStats(0, 0, 0, 0),
        tvShows = TvShowStats(0, 0, 0, 0),
        episodes = EpisodeStats(0, 0, 0),
        watchTime = WatchTime(0, 0, 0),
        genres = emptyList(),
        ratings = emptyList(),
        collection = emptyList(),
    )
}

private class CapturingStatsRepository : StatsRepository {
    var monthStart = 0L
    var yearStart = 0L

    override fun observeStatistics(monthStart: Long, yearStart: Long): Flow<Statistics> {
        this.monthStart = monthStart
        this.yearStart = yearStart
        return emptyFlow()
    }
}
