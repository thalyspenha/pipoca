package com.thalyspenha.pipoca.data.remote

import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Confere caminhos e parâmetros enviados ao TMDB e o parsing via Retrofit. */
class TmdbApiTest {
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

    private fun enqueue(fixtureName: String) {
        server.enqueue(MockResponse(code = 200, body = fixture(fixtureName)))
    }

    @Test
    fun `searchMovies chama search movie com query`() = runBlocking {
        enqueue("search_movie.json")

        val page = api.searchMovies("matrix")

        val url = server.takeRequest().url
        assertEquals("/3/search/movie", url.encodedPath)
        assertEquals("matrix", url.queryParameter("query"))
        assertEquals("1", url.queryParameter("page"))
        assertEquals("false", url.queryParameter("include_adult"))
        assertEquals(603L, page.results.first().id)
    }

    @Test
    fun `searchTvShows chama search tv`() = runBlocking {
        enqueue("search_tv.json")

        api.searchTvShows("breaking bad")

        val url = server.takeRequest().url
        assertEquals("/3/search/tv", url.encodedPath)
        assertEquals("breaking bad", url.queryParameter("query"))
    }

    @Test
    fun `getMovieDetails pede credits junto`() = runBlocking {
        enqueue("movie_details.json")

        api.getMovieDetails(603)

        val url = server.takeRequest().url
        assertEquals("/3/movie/603", url.encodedPath)
        assertEquals("credits", url.queryParameter("append_to_response"))
    }

    @Test
    fun `getTvShowDetails pede credits junto`() = runBlocking {
        enqueue("tv_details.json")

        api.getTvShowDetails(1396)

        val url = server.takeRequest().url
        assertEquals("/3/tv/1396", url.encodedPath)
        assertEquals("credits", url.queryParameter("append_to_response"))
    }
}
