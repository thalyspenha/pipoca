package com.thalyspenha.pipoca.data.cache

import java.time.Duration
import java.time.Instant

/** Validade do cache TMDB (ARCHITECTURE.md → Cache). */
object CachePolicy {
    val MOVIE: Duration = Duration.ofDays(7)
    val TV_SHOW_ACTIVE: Duration = Duration.ofDays(1)
    val TV_SHOW_FINISHED: Duration = Duration.ofDays(30)

    private val FINISHED_STATUSES = setOf("Ended", "Canceled")

    fun tvShowTtl(tmdbStatus: String?): Duration =
        if (tmdbStatus in FINISHED_STATUSES) TV_SHOW_FINISHED else TV_SHOW_ACTIVE

    fun isFresh(fetchedAt: Long, ttl: Duration, now: Instant): Boolean =
        now.toEpochMilli() - fetchedAt < ttl.toMillis()
}
