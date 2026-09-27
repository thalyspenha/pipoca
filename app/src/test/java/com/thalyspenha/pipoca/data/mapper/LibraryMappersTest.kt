package com.thalyspenha.pipoca.data.mapper

import com.thalyspenha.pipoca.data.local.entity.UserMovieEntity
import com.thalyspenha.pipoca.data.local.entity.UserTvShowEntity
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class LibraryMappersTest {

    @Test
    fun `filme ida e volta preserva todos os campos`() {
        val entity = UserMovieEntity(603, MovieStatus.WATCHED, true, 9, "Clássico", 1_000, 2_000)

        val domain = entity.toDomain()

        assertEquals(Instant.ofEpochMilli(1_000), domain.addedAt)
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun `serie ida e volta preserva todos os campos`() {
        val entity = UserTvShowEntity(1396, TvShowStatus.COMPLETED, false, null, null, 5, 6)

        assertEquals(entity, entity.toDomain().toEntity())
    }
}
