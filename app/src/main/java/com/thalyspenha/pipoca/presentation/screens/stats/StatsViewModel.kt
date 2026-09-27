package com.thalyspenha.pipoca.presentation.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalyspenha.pipoca.domain.stats.Statistics
import com.thalyspenha.pipoca.domain.usecase.stats.ObserveStatisticsUseCase
import com.thalyspenha.pipoca.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Estatísticas locais (D-048, D-049); atualizam sozinhas via Flow do Room. */
@HiltViewModel
class StatsViewModel @Inject constructor(
    observeStatistics: ObserveStatisticsUseCase,
) : ViewModel() {

    val uiState: StateFlow<UiState<Statistics>> =
        observeStatistics()
            .map<Statistics, UiState<Statistics>> { UiState.Success(it) }
            .catch { emit(UiState.Error("Não foi possível calcular as estatísticas.", it)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
