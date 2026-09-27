package com.thalyspenha.pipoca.presentation.screens.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalyspenha.pipoca.domain.model.HistoryEntry
import com.thalyspenha.pipoca.domain.model.HistoryFilter
import com.thalyspenha.pipoca.domain.model.HistoryType
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import com.thalyspenha.pipoca.domain.repository.MovieRepository
import com.thalyspenha.pipoca.domain.repository.TvShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HistoryUiState(
    val filter: HistoryFilter = HistoryFilter.ALL,
    /** Grupos por dia (fuso do aparelho), mais recente primeiro. */
    val days: List<HistoryDay> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
) {
    val isHistoryEmpty: Boolean get() = !isLoading && totalCount == 0
}

data class HistoryDay(val date: LocalDate, val entries: List<HistoryEntry>)

/** Agrupa por dia local mantendo a ordem (mais recente primeiro). */
fun List<HistoryEntry>.groupByDay(zone: ZoneId): List<HistoryDay> =
    groupBy { it.watchedAt.atZone(zone).toLocalDate() }
        .map { (date, entries) -> HistoryDay(date, entries) }

/**
 * Histórico (D-046): observa `watch_history` com cache (Room) e aplica o filtro. Gerado ao marcar
 * filme como assistido (D-029) e cada episódio (D-037); desmarcar episódio apaga o evento.
 * Item sem cache busca detalhes uma vez por título, só com token.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    library: LibraryRepository,
    private val tmdbConfig: TmdbConfig,
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository,
) : ViewModel() {

    /** Fuso usado para os grupos por dia; trocável em teste. */
    internal var zone: ZoneId = ZoneId.systemDefault()

    private val requested = mutableSetOf<Pair<HistoryType, Long>>()
    private val filter = savedStateHandle.getStateFlow(KEY_FILTER, HistoryFilter.ALL)

    val uiState: StateFlow<HistoryUiState> =
        combine(library.observeHistory(), filter) { entries, filter ->
            fetchMissing(entries)
            HistoryUiState(
                filter = filter,
                days = entries.filter(filter::matches).groupByDay(zone),
                totalCount = entries.size,
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HistoryUiState())

    fun onFilterChange(filter: HistoryFilter) {
        savedStateHandle[KEY_FILTER] = filter
    }

    private fun fetchMissing(entries: List<HistoryEntry>) {
        if (!tmdbConfig.isConfigured) return
        entries.filter { it.title == null && requested.add(it.type to it.tmdbId) }.forEach { entry ->
            viewModelScope.launch {
                when (entry.type) {
                    HistoryType.MOVIE -> movieRepository.refreshMovieDetails(entry.tmdbId)
                    HistoryType.EPISODE -> tvShowRepository.refreshTvShowDetails(entry.tmdbId)
                }
            }
        }
    }

    private companion object {
        const val KEY_FILTER = "filter"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
