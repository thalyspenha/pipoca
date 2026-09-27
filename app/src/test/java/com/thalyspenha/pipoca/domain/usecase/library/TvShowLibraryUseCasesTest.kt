package com.thalyspenha.pipoca.domain.usecase.library

import com.thalyspenha.pipoca.data.repository.MutableClock
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class TvShowLibraryUseCasesTest {
    private val repository = FakeLibraryRepository()
    private val clock = MutableClock()
    private val add = AddTvShowToLibraryUseCase(repository, clock)
    private val remove = RemoveTvShowFromLibraryUseCase(repository)
    private val setStatus = SetTvShowStatusUseCase(repository, clock)
    private val setFavorite = SetTvShowFavoriteUseCase(repository, clock)
    private val setRating = SetTvShowRatingUseCase(repository, clock)

    private val id = 1396L

    @Test
    fun `adicionar cria serie quero assistir`() = runTest {
        add(id)

        val show = repository.getTvShow(id)!!
        assertEquals(TvShowStatus.WANT_TO_WATCH, show.status)
        assertEquals(clock.now, show.addedAt)
    }

    @Test
    fun `adicionar serie existente nao sobrescreve status`() = runTest {
        setStatus(id, TvShowStatus.WATCHING)

        add(id, TvShowStatus.COMPLETED)

        assertEquals(TvShowStatus.WATCHING, repository.getTvShow(id)!!.status)
    }

    @Test
    fun `remover apaga serie`() = runTest {
        add(id)

        remove(id)

        assertNull(repository.getTvShow(id))
    }

    @Test
    fun `transicoes quero assistir assistindo concluida atualizam data e mantem addedAt`() = runTest {
        add(id)
        val addedAt = clock.now

        clock.advance(Duration.ofDays(1))
        setStatus(id, TvShowStatus.WATCHING)
        assertEquals(TvShowStatus.WATCHING, repository.getTvShow(id)!!.status)

        clock.advance(Duration.ofDays(10))
        setStatus(id, TvShowStatus.COMPLETED)

        val show = repository.getTvShow(id)!!
        assertEquals(TvShowStatus.COMPLETED, show.status)
        assertEquals(addedAt, show.addedAt)
        assertEquals(clock.now, show.updatedAt)
    }

    @Test
    fun `marcar status igual nao altera updatedAt`() = runTest {
        setStatus(id, TvShowStatus.WATCHING)
        val updatedAt = repository.getTvShow(id)!!.updatedAt
        clock.advance(Duration.ofHours(1))

        setStatus(id, TvShowStatus.WATCHING)

        assertEquals(updatedAt, repository.getTvShow(id)!!.updatedAt)
    }

    @Test
    fun `favoritar e remover favorito`() = runTest {
        setFavorite(id, true)
        assertTrue(repository.getTvShow(id)!!.isFavorite)

        setFavorite(id, false)
        assertFalse(repository.getTvShow(id)!!.isFavorite)
    }

    @Test
    fun `adicionar e remover nota`() = runTest {
        setRating(id, 9)
        assertEquals(9, repository.getTvShow(id)!!.rating)

        setRating(id, null)
        assertNull(repository.getTvShow(id)!!.rating)
    }

    @Test
    fun `nota fora de 1 a 10 e rejeitada`() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { setRating(id, -1) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { setRating(id, 11) } }
    }
}
