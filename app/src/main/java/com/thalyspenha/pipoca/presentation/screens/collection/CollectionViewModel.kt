package com.thalyspenha.pipoca.presentation.screens.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalyspenha.pipoca.domain.model.CollectionEntry
import com.thalyspenha.pipoca.domain.model.CollectionFilter
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.CollectionSort
import com.thalyspenha.pipoca.domain.model.filterAndSort
import com.thalyspenha.pipoca.domain.repository.CollectionRepository
import com.thalyspenha.pipoca.domain.usecase.cache.FetchMissingDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * "Minha Coleção": observa a coleção (Room), aplica filtro e ordenação (D-040, D-041).
 * Filtro e ordenação sobrevivem à recriação do processo (SavedStateHandle).
 * Item sem cache TMDB dispara uma busca de detalhes por título, como a Home (D-030, D-055).
 */
@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    repository: CollectionRepository,
    private val fetchMissingDetails: FetchMissingDetailsUseCase,
) : ViewModel() {

    private val filter = savedStateHandle.getStateFlow(KEY_FILTER, CollectionFilter.ALL)
    private val sort = savedStateHandle.getStateFlow(KEY_SORT, CollectionSort.TITLE)

    val uiState: StateFlow<CollectionUiState> =
        combine(repository.observeEntries(), filter, sort) { entries, filter, sort ->
            fetchMissingDetails(entries)
            CollectionUiState(
                filter = filter,
                sort = sort,
                items = entries.filterAndSort(filter, sort).map { it.toListItem() },
                totalCount = entries.size,
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CollectionUiState())

    fun onFilterChange(filter: CollectionFilter) {
        savedStateHandle[KEY_FILTER] = filter
    }

    fun onSortChange(sort: CollectionSort) {
        savedStateHandle[KEY_SORT] = sort
    }

    private fun fetchMissingDetails(entries: List<CollectionEntry>) {
        val missing = entries.filter { it.title == null }.map { it.item }
        fetchMissingDetails.request(
            scope = viewModelScope,
            movieIds = missing.filter { it.mediaType == CollectionMediaType.MOVIE }.map { it.tmdbId },
            tvShowIds = missing.filter { it.mediaType == CollectionMediaType.TV_SHOW }.map { it.tmdbId },
        )
    }

    private companion object {
        const val KEY_FILTER = "filter"
        const val KEY_SORT = "sort"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
