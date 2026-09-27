package com.thalyspenha.pipoca.domain.usecase.library

import com.thalyspenha.pipoca.data.repository.MutableClock
import com.thalyspenha.pipoca.domain.model.MovieStatus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class MovieLibraryUseCasesTest {
    private val repository = FakeLibraryRepository()
    private val clock = MutableClock()
    private val add = AddMovieToLibraryUseCase(repository, clock)
    private val remove = RemoveMovieFromLibraryUseCase(repository)
    private val setStatus = SetMovieStatusUseCase(repository, clock)
    private val setFavorite = SetMovieFavoriteUseCase(repository, clock)
    private val setRating = SetMovieRatingUseCase(repository, clock)

    private val id = 603L

    @Test
    fun `adicionar cria filme quero assistir com datas`() = runTest {
        add(id)

        val movie = repository.getMovie(id)!!
        assertEquals(MovieStatus.WANT_TO_WATCH, movie.status)
        assertEquals(clock.now, movie.addedAt)
        assertEquals(clock.now, movie.updatedAt)
        assertNull(repository.movieWatches[id])
    }

    @Test
    fun `adicionar como assistido registra visualizacao`() = runTest {
        add(id, MovieStatus.WATCHED)

        assertEquals(MovieStatus.WATCHED, repository.getMovie(id)!!.status)
        assertEquals(listOf(clock.now), repository.movieWatches[id])
    }

    @Test
    fun `adicionar filme existente nao sobrescreve dados`() = runTest {
        setRating(id, 8)
        clock.advance(Duration.ofDays(1))

        add(id, MovieStatus.WATCHED)

        val movie = repository.getMovie(id)!!
        assertEquals(MovieStatus.WANT_TO_WATCH, movie.status)
        assertEquals(8, movie.rating)
    }

    @Test
    fun `remover apaga filme e historico`() = runTest {
        add(id, MovieStatus.WATCHED)

        remove(id)

        assertNull(repository.getMovie(id))
        assertNull(repository.movieWatches[id])
    }

    @Test
    fun `marcar assistido registra visualizacao e atualiza data mantendo addedAt`() = runTest {
        add(id)
        val addedAt = clock.now
        clock.advance(Duration.ofHours(2))

        setStatus(id, MovieStatus.WATCHED)

        val movie = repository.getMovie(id)!!
        assertEquals(MovieStatus.WATCHED, movie.status)
        assertEquals(addedAt, movie.addedAt)
        assertEquals(clock.now, movie.updatedAt)
        assertEquals(listOf(clock.now), repository.movieWatches[id])
    }

    @Test
    fun `marcar assistido duas vezes nao duplica historico`() = runTest {
        setStatus(id, MovieStatus.WATCHED)
        clock.advance(Duration.ofMinutes(1))
        setStatus(id, MovieStatus.WATCHED)

        assertEquals(1, repository.movieWatches[id]!!.size)
    }

    @Test
    fun `voltar para quero assistir mantem historico e reassistir soma visualizacao`() = runTest {
        setStatus(id, MovieStatus.WATCHED)
        clock.advance(Duration.ofDays(30))
        setStatus(id, MovieStatus.WANT_TO_WATCH)
        assertEquals(MovieStatus.WANT_TO_WATCH, repository.getMovie(id)!!.status)
        assertEquals(1, repository.movieWatches[id]!!.size)

        clock.advance(Duration.ofDays(1))
        setStatus(id, MovieStatus.WATCHED)

        assertEquals(2, repository.movieWatches[id]!!.size)
    }

    @Test
    fun `favoritar filme fora da biblioteca adiciona como quero assistir`() = runTest {
        setFavorite(id, true)

        val movie = repository.getMovie(id)!!
        assertTrue(movie.isFavorite)
        assertEquals(MovieStatus.WANT_TO_WATCH, movie.status)
    }

    @Test
    fun `remover favorito mantem o filme`() = runTest {
        setFavorite(id, true)
        setFavorite(id, false)

        assertFalse(repository.getMovie(id)!!.isFavorite)
    }

    @Test
    fun `remover favorito de filme fora da biblioteca nao adiciona`() = runTest {
        setFavorite(id, false)

        assertNull(repository.getMovie(id))
    }

    @Test
    fun `adicionar e remover nota`() = runTest {
        setRating(id, 10)
        assertEquals(10, repository.getMovie(id)!!.rating)

        setRating(id, null)
        assertNull(repository.getMovie(id)!!.rating)
    }

    @Test
    fun `nota fora de 1 a 10 e rejeitada`() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { setRating(id, 0) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { setRating(id, 11) } }
        assertNull(repository.movies.value[id])
    }
}
