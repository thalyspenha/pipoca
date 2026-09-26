package com.thalyspenha.pipoca.presentation.screens.home

import androidx.lifecycle.ViewModel
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import com.thalyspenha.pipoca.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tmdbConfig: TmdbConfig,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<HomeContent>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeContent>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = UiState.Success(HomeContent(tmdbConfigured = tmdbConfig.isConfigured))
    }
}
