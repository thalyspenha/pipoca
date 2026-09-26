package com.thalyspenha.pipoca.data.remote

import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class TmdbRemoteDataSourceTest {
    private val server = MockWebServer()
    private lateinit var api: TmdbApi

    @Before
    fun setUp() {
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/3/"))
            .addConverterFactory(TmdbJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TmdbApi::class.java)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun dataSource(token: String = "token") = TmdbRemoteDataSource(api, TmdbConfig(token))

    @Test
    fun `sucesso devolve o DTO`() = runBlocking {
        server.enqueue(MockResponse(code = 200, body = fixture("movie_details.json")))

        val result = dataSource().getMovieDetails(603)

        assertEquals(603L, (result as DataResult.Success).data.id)
    }

    @Test
    fun `sem token nao chama a rede`() = runBlocking {
        val result = dataSource(token = "").searchMovies("matrix")

        assertEquals(DataResult.Failure(DataError.MissingApiKey), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `401 vira InvalidApiKey`() = runBlocking {
        server.enqueue(
            MockResponse(code = 401, body = """{"status_code":7,"status_message":"Invalid API key"}"""),
        )

        assertEquals(DataResult.Failure(DataError.InvalidApiKey), dataSource().searchTvShows("x"))
    }

    @Test
    fun `404 vira NotFound`() = runBlocking {
        server.enqueue(MockResponse(code = 404, body = fixture("error_not_found.json")))

        assertEquals(DataResult.Failure(DataError.NotFound), dataSource().getTvShowDetails(999999999))
    }

    @Test
    fun `500 vira Unknown`() = runBlocking {
        server.enqueue(MockResponse(code = 500))

        val result = dataSource().getMovieDetails(1)

        assertTrue((result as DataResult.Failure).error is DataError.Unknown)
    }

    @Test
    fun `falha de conexao vira Network`() = runBlocking {
        server.close()

        assertEquals(DataResult.Failure(DataError.Network), dataSource().searchMovies("matrix"))
    }

    @Test
    fun `JSON invalido vira Unknown`() = runBlocking {
        server.enqueue(MockResponse(code = 200, body = """{"results": "nao e lista"}"""))

        val result = dataSource().searchMovies("matrix")

        assertTrue((result as DataResult.Failure).error is DataError.Unknown)
    }
}
