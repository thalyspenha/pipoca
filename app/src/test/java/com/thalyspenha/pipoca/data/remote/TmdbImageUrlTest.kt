package com.thalyspenha.pipoca.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbImageUrlTest {
    @Test
    fun `monta url com tamanho e path`() {
        assertEquals(
            "https://image.tmdb.org/t/p/w500/abc.jpg",
            TmdbImageUrl.build("/abc.jpg", TmdbImageUrl.POSTER),
        )
    }

    @Test
    fun `path ausente ou vazio retorna null`() {
        assertNull(TmdbImageUrl.build(null, TmdbImageUrl.POSTER))
        assertNull(TmdbImageUrl.build("", TmdbImageUrl.POSTER))
    }
}
