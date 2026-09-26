package com.thalyspenha.pipoca.data.repository

import com.thalyspenha.pipoca.data.remote.TmdbRemoteDataSource
import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration

class MovieRepositoryImplTest {
    private val api = FakeTmdbApi()
    private val dao = FakeTmdbCacheDao()
    private val clock = MutableClock()
    private val repository = MovieRepositoryImpl(TmdbRemoteDataSource(api, TmdbConfig("token")), dao, clock)

    @Test
    fun `sem cache busca no TMDB, grava e o Flow emite os detalhes`() = runBlocking {
        assertNull(repository.observeMovieDetails(603).first())

        val result = repository.refreshMovieDetails(603)

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(1, api.calls)
        val movie = repository.observeMovieDetails(603).first()!!
        assertEquals("Matrix", movie.title)
        assertEquals(listOf("Lana Wachowski", "Lilly Wachowski"), movie.directors)
        assertEquals("Keanu Reeves", movie.cast.first().name)
        assertEquals(movie.genres.sortedBy { it.name }, movie.genres)
    }

    @Test
    fun `cache valido nao chama a rede`() = runBlocking {
        repository.refreshMovieDetails(603)
        clock.advance(Duration.ofDays(6))

        repository.refreshMovieDetails(603)

        assertEquals(1, api.calls)
    }

    @Test
    fun `cache vencido busca de novo`() = runBlocking {
        repository.refreshMovieDetails(603)
        clock.advance(Duration.ofDays(8))

        repository.refreshMovieDetails(603)

        assertEquals(2, api.calls)
    }

    @Test
    fun `force ignora a validade`() = runBlocking {
        repository.refreshMovieDetails(603)

        repository.refreshMovieDetails(603, force = true)

        assertEquals(2, api.calls)
    }

    @Test
    fun `falha de rede mantem o cache e devolve o erro`() = runBlocking {
        repository.refreshMovieDetails(603)
        clock.advance(Duration.ofDays(8))
        api.failWithNetworkError = true

        val result = repository.refreshMovieDetails(603)

        assertEquals(DataResult.Failure(DataError.Network), result)
        assertEquals("Matrix", repository.observeMovieDetails(603).first()?.title)
    }
}
