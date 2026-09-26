package com.thalyspenha.pipoca.presentation.screens.home

import com.thalyspenha.pipoca.domain.model.TmdbConfig
import com.thalyspenha.pipoca.util.UiState
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelTest {

    @Test
    fun `token configurado resulta em sucesso com tmdbConfigured true`() {
        val viewModel = HomeViewModel(TmdbConfig(apiToken = "token"))

        assertEquals(UiState.Success(HomeContent(tmdbConfigured = true)), viewModel.uiState.value)
    }

    @Test
    fun `token vazio resulta em sucesso com tmdbConfigured false`() {
        val viewModel = HomeViewModel(TmdbConfig(apiToken = "  "))

        assertEquals(UiState.Success(HomeContent(tmdbConfigured = false)), viewModel.uiState.value)
    }
}
