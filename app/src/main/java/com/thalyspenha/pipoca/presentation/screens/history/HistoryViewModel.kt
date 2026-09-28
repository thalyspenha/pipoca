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
import java.time.Duration
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

data class HistoryDay(
    val date: LocalDate,
    val entries: List<HistoryEntry>,
    /** O que a tela mostra: episódios marcados juntos viram uma linha só (D-062). */
    val items: List<HistoryItem> = entries.groupEpisodeBatches(),
)

/** Linha do histórico: uma visualização ou um lote de episódios da mesma série. */
sealed interface HistoryItem {
    val key: String

    data class Single(val entry: HistoryEntry) : HistoryItem {
        override val key: String get() = "e-${entry.id}"
    }

    /** Mais recente primeiro, como o histórico. Sempre 2 ou mais episódios da mesma série. */
    data class EpisodeBatch(val entries: List<HistoryEntry>) : HistoryItem {
        override val key: String get() = "b-${entries.first().id}"
        val latest: HistoryEntry get() = entries.first()
    }
}

/** Intervalo máximo entre episódios seguidos da mesma série para contarem como marcados juntos. */
internal val BATCH_GAP: Duration = Duration.ofMinutes(10)

/**
 * Junta episódios consecutivos da mesma série marcados com até [BATCH_GAP] de diferença
 * ("marcar temporada" grava todos no mesmo instante; toques seguidos em "Assisti" ficam a segundos).
 * Episódios vistos em dias/horários diferentes (maratona real, ~45 min) continuam separados.
 * A lista chega do mais recente para o mais antigo; a ordem é mantida.
 */
fun List<HistoryEntry>.groupEpisodeBatches(): List<HistoryItem> {
    val result = mutableListOf<HistoryItem>()
    var batch = mutableListOf<HistoryEntry>()
    fun flush() {
        when (batch.size) {
            0 -> Unit
            1 -> result += HistoryItem.Single(batch.single())
            else -> result += HistoryItem.EpisodeBatch(batch)
        }
        batch = mutableListOf()
    }
    for (entry in this) {
        val last = batch.lastOrNull()
        val joins = last != null && entry.type == HistoryType.EPISODE && last.tmdbId == entry.tmdbId &&
            Duration.between(entry.watchedAt, last.watchedAt).abs() <= BATCH_GAP
        if (!joins) flush()
        if (entry.type == HistoryType.EPISODE) batch += entry else result += HistoryItem.Single(entry)
    }
    flush()
    return result
}

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
