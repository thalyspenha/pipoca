package com.thalyspenha.pipoca.presentation.screens.history

import com.thalyspenha.pipoca.domain.model.HistoryEntry
import com.thalyspenha.pipoca.domain.model.HistoryType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class HistoryGroupingTest {
    private val base = Instant.parse("2026-09-28T18:00:00Z")

    private fun episode(id: Long, show: Long, minutesAgo: Long, number: Int = id.toInt()) = HistoryEntry(
        id = id, type = HistoryType.EPISODE, tmdbId = show, title = "Série $show", posterPath = null,
        seasonNumber = 1, episodeNumber = number, watchedAt = base.minusSeconds(minutesAgo * 60),
    )

    private fun movie(id: Long, minutesAgo: Long) = HistoryEntry(
        id = id, type = HistoryType.MOVIE, tmdbId = id, title = "Filme", posterPath = null,
        watchedAt = base.minusSeconds(minutesAgo * 60),
    )

    private fun List<HistoryItem>.shape() = map {
        when (it) {
            is HistoryItem.Single -> "S${it.entry.id}"
            is HistoryItem.EpisodeBatch -> "B" + it.entries.joinToString(",") { e -> e.id.toString() }
        }
    }

    @Test
    fun `temporada marcada de uma vez vira um lote`() {
        val entries = (22L downTo 1L).map { episode(it, show = 1408, minutesAgo = 0) }
        val items = entries.groupEpisodeBatches()
        assertEquals(1, items.size)
        assertEquals(22, (items.single() as HistoryItem.EpisodeBatch).entries.size)
    }

    @Test
    fun `toques seguidos juntam e maratona real fica separada`() {
        val items = listOf(
            episode(4, show = 1, minutesAgo = 0),
            episode(3, show = 1, minutesAgo = 1), // toques seguidos em "Assisti"
            episode(2, show = 1, minutesAgo = 50), // episódio seguinte assistido depois
            episode(1, show = 1, minutesAgo = 100),
        ).groupEpisodeBatches()
        assertEquals(listOf("B4,3", "S2", "S1"), items.shape())
    }

    @Test
    fun `outra série ou filme no meio quebra o lote`() {
        val items = listOf(
            episode(5, show = 1, minutesAgo = 0),
            episode(4, show = 1, minutesAgo = 0),
            movie(100, minutesAgo = 0),
            episode(3, show = 1, minutesAgo = 0),
            episode(2, show = 2, minutesAgo = 0),
            episode(1, show = 2, minutesAgo = 0),
        ).groupEpisodeBatches()
        assertEquals(listOf("B5,4", "S100", "S3", "B2,1"), items.shape())
    }

    @Test
    fun `episodio sozinho continua linha simples`() {
        assertEquals(listOf("S1"), listOf(episode(1, show = 1, minutesAgo = 0)).groupEpisodeBatches().shape())
    }
}
