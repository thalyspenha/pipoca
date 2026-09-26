package com.thalyspenha.pipoca.data.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class CachePolicyTest {
    private val now = Instant.parse("2026-09-26T12:00:00Z")

    private fun fetchedAgo(duration: Duration) = now.minus(duration).toEpochMilli()

    @Test
    fun `filme vale 7 dias`() {
        assertTrue(CachePolicy.isFresh(fetchedAgo(Duration.ofDays(6)), CachePolicy.MOVIE, now))
        assertFalse(CachePolicy.isFresh(fetchedAgo(Duration.ofDays(7)), CachePolicy.MOVIE, now))
    }

    @Test
    fun `serie encerrada ou cancelada vale 30 dias, em exibicao 1 dia`() {
        assertEquals(CachePolicy.TV_SHOW_FINISHED, CachePolicy.tvShowTtl("Ended"))
        assertEquals(CachePolicy.TV_SHOW_FINISHED, CachePolicy.tvShowTtl("Canceled"))
        assertEquals(CachePolicy.TV_SHOW_ACTIVE, CachePolicy.tvShowTtl("Returning Series"))
        assertEquals(CachePolicy.TV_SHOW_ACTIVE, CachePolicy.tvShowTtl(null))
    }
}
