package com.thalyspenha.pipoca.data.remote.interceptor

import com.thalyspenha.pipoca.domain.model.TmdbConfig
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class InterceptorsTest {
    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun call(token: String, path: String = "/movie/1") {
        server.enqueue(MockResponse(code = 200))
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(TmdbConfig(token)))
            .addInterceptor(LanguageInterceptor())
            .build()
        client.newCall(Request.Builder().url(server.url(path)).build()).execute().close()
    }

    @Test
    fun `token configurado adiciona header Bearer`() {
        call(token = "abc")

        assertEquals("Bearer abc", server.takeRequest().headers["Authorization"])
    }

    @Test
    fun `sem token nao adiciona header`() {
        call(token = "")

        assertNull(server.takeRequest().headers["Authorization"])
    }

    @Test
    fun `adiciona language pt-BR por padrao`() {
        call(token = "abc")

        assertEquals("pt-BR", server.takeRequest().url.queryParameter("language"))
    }

    @Test
    fun `mantem language ja definido na chamada`() {
        call(token = "abc", path = "/movie/1?language=en-US")

        assertEquals("en-US", server.takeRequest().url.queryParameter("language"))
    }
}
