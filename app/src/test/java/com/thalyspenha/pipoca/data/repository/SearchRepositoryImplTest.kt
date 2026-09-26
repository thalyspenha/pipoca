package com.thalyspenha.pipoca.data.repository

import com.thalyspenha.pipoca.data.remote.TmdbRemoteDataSource
import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRepositoryImplTest {
    private val api = FakeTmdbApi()

    private fun repository(token: String = "token") = SearchRepositoryImpl(TmdbRemoteDataSource(api, TmdbConfig(token)))

    @Test
    fun `busca de filmes devolve modelos de dominio`() = runBlocking {
        val result = repository().searchMovies("matrix") as DataResult.Success

        assertEquals("Matrix", result.data.items.first().title)
        assertEquals(92, result.data.totalResults)
    }

    @Test
    fun `busca de series devolve modelos de dominio`() = runBlocking {
        val result = repository().searchTvShows("breaking bad") as DataResult.Success

        assertEquals("Breaking Bad", result.data.items.first().name)
    }

    @Test
    fun `consulta curta devolve pagina vazia sem chamar a rede`() = runBlocking {
        val result = repository().searchMovies(" a ") as DataResult.Success

        assertTrue(result.data.items.isEmpty())
        assertEquals(0, api.calls)
    }

    @Test
    fun `erro do data source e repassado`() = runBlocking {
        assertEquals(DataResult.Failure(DataError.MissingApiKey), repository(token = "").searchTvShows("breaking bad"))
    }
}
