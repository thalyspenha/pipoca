package com.thalyspenha.pipoca.domain.progress

import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.SeasonSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ShowProgressCalculatorTest {
    private val today = LocalDate.of(2026, 9, 27)

    private fun ep(season: Int, number: Int, airDate: LocalDate? = LocalDate.of(2020, 1, number)) = Episode(
        id = season * 100L + number, showId = 1, seasonNumber = season, episodeNumber = number,
        name = "S${season}E$number", overview = null, stillPath = null, airDate = airDate, runtimeMinutes = 45,
    )

    private fun season(number: Int, count: Int) =
        SeasonSummary(id = number.toLong(), seasonNumber = number, name = "T$number", overview = null,
            posterPath = null, airDate = null, episodeCount = count)

    /** Série finalizada: 2 temporadas (3 + 2 episódios) e 2 especiais. */
    private val ended = listOf(ep(0, 1), ep(0, 2), ep(1, 1), ep(1, 2), ep(1, 3), ep(2, 1), ep(2, 2))
    private val endedSeasons = listOf(season(0, 2), season(1, 3), season(2, 2))

    private fun calc(
        watched: Set<Long>,
        episodes: List<Episode> = ended,
        seasons: List<SeasonSummary> = endedSeasons,
        status: String? = "Ended",
    ) = ShowProgressCalculator.calculate(episodes, seasons, watched, status, today)

    @Test
    fun `nada assistido - 0 de 5, especiais fora, proximo e S1E1`() {
        val p = calc(emptySet())

        assertEquals(0, p.watched)
        assertEquals(5, p.available)
        assertEquals(0, p.percent)
        assertEquals(101L, p.nextEpisode?.id)
        assertEquals(1, p.nextSeason)
        assertFalse(p.isCompleted)
    }

    @Test
    fun `especiais assistidos nao contam`() {
        val p = calc(setOf(1, 2))

        assertEquals(0, p.watched)
        assertEquals(5, p.available)
    }

    @Test
    fun `percentual arredonda para baixo`() {
        val p = calc(setOf(101, 102))

        assertEquals(2, p.watched)
        assertEquals(40, p.percent)
    }

    @Test
    fun `proximo episodio pula para a proxima temporada`() {
        val p = calc(setOf(101, 102, 103))

        assertEquals(201L, p.nextEpisode?.id)
        assertEquals(2, p.nextSeason)
    }

    @Test
    fun `proximo episodio e o primeiro buraco, mesmo com posteriores assistidos`() {
        val p = calc(setOf(101, 103, 201))

        assertEquals(102L, p.nextEpisode?.id)
    }

    @Test
    fun `tudo assistido em serie finalizada - concluida`() {
        val p = calc(setOf(101, 102, 103, 201, 202))

        assertEquals(100, p.percent)
        assertNull(p.nextEpisode)
        assertTrue(p.isCompleted)
        assertFalse(p.isCaughtUp)
    }

    @Test
    fun `tudo assistido em serie no ar - em dia, nao concluida`() {
        val future = ep(2, 3, airDate = today.plusDays(7))
        val p = calc(
            watched = setOf(101, 102, 103, 201, 202),
            episodes = ended + future,
            seasons = listOf(season(1, 3), season(2, 3)),
            status = "Returning Series",
        )

        assertEquals(5, p.available)
        assertTrue(p.isCaughtUp)
        assertFalse(p.isCompleted)
        assertEquals(future.id, p.upcomingEpisode?.id)
    }

    @Test
    fun `episodio de hoje ja conta, sem data ou futuro nao`() {
        val episodes = listOf(ep(1, 1, today), ep(1, 2, null), ep(1, 3, today.plusDays(1)))
        val p = calc(emptySet(), episodes = episodes, seasons = listOf(season(1, 3)), status = "Returning Series")

        assertEquals(1, p.available)
        assertEquals(102L, p.upcomingEpisode?.id)
    }

    @Test
    fun `cache incompleto nunca conclui`() {
        val onlySeason1 = ended.filter { it.seasonNumber != 2 }
        val p = calc(setOf(101, 102, 103), episodes = onlySeason1)

        assertFalse(p.isComplete)
        assertEquals(3, p.available)
        assertFalse(p.isCompleted)
    }

    @Test
    fun `temporada anunciada sem episodios nao impede cache completo`() {
        val p = calc(setOf(101, 102, 103, 201, 202), seasons = endedSeasons + season(3, 0), status = "Canceled")

        assertTrue(p.isComplete)
        assertTrue(p.isCompleted)
    }

    @Test
    fun `assistido que saiu do TMDB nao conta`() {
        val p = calc(setOf(101, 999))

        assertEquals(1, p.watched)
    }

    @Test
    fun `serie sem episodios disponiveis`() {
        val p = calc(emptySet(), episodes = listOf(ep(1, 1, null)), seasons = listOf(season(1, 1)), status = "In Production")

        assertEquals(0, p.available)
        assertEquals(0, p.percent)
        assertFalse(p.allAvailableWatched)
        assertFalse(p.isCompleted)
    }
}
