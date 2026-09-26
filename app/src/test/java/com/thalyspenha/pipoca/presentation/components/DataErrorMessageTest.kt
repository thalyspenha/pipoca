package com.thalyspenha.pipoca.presentation.components

import com.thalyspenha.pipoca.domain.model.DataError
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataErrorMessageTest {
    @Test
    fun `chave ausente orienta a configurar local properties`() {
        assertTrue(DataError.MissingApiKey.toMessage().contains("TMDB_API_TOKEN"))
    }

    @Test
    fun `so erros passageiros permitem tentar de novo`() {
        assertTrue(DataError.Network.isRetryable)
        assertTrue(DataError.Unknown().isRetryable)
        assertFalse(DataError.MissingApiKey.isRetryable)
        assertFalse(DataError.InvalidApiKey.isRetryable)
        assertFalse(DataError.NotFound.isRetryable)
    }
}
