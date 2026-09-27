package com.thalyspenha.pipoca.presentation.screens.details.tv

import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.SeasonSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SeasonRowsTest {
    private val today = LocalDate.of(2026, 9, 27)

    private fun summary(number: Int, count: Int) = SeasonSummary(
        id = number.toLong(), seasonNumber = number, name = "T$number", overview = null,
        posterPath = null, airDate = null, episodeCount = count,
    )

    private fun ep(season: Int, number: Int, airDate: LocalDate? = LocalDate.of(2020, 1, 1)) = Episode(
        id = season * 100L + number, showId = 1, seasonNumber = season, episodeNumber = number, name = "",
        overview = null, stillPath = null, airDate = airDate, runtimeMinutes = null,
    )

    @Test
    fun `especiais por ultimo, vazias fora, contagem so com cache`() {
        val rows = buildSeasonRows(
            seasons = listOf(summary(0, 2), summary(1, 2), summary(2, 3), summary(3, 0)),
            episodes = listOf(ep(1, 1), ep(1, 2), ep(0, 1)),
            watchedEpisodeIds = setOf(101, 102),
            today = today,
        )

        assertEquals(listOf(1, 2, 0), rows.map { it.seasonNumber })
        assertEquals(2, rows[0].watched)
        assertTrue(rows[0].isFullyWatched)
        assertNull(rows[1].watched)
        assertEquals(3, rows[1].episodeCount)
        assertTrue(rows[2].isSpecials)
    }

    @Test
    fun `episodios futuros nao entram na contagem da temporada`() {
        val rows = buildSeasonRows(
            seasons = listOf(summary(1, 2)),
            episodes = listOf(ep(1, 1), ep(1, 2, today.plusDays(1))),
            watchedEpisodeIds = setOf(101),
            today = today,
        )

        assertEquals(1, rows[0].aired)
        assertTrue(rows[0].isFullyWatched)
    }
}
