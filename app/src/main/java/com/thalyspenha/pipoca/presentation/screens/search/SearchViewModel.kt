package com.thalyspenha.pipoca.presentation.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.map
import com.thalyspenha.pipoca.domain.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Busca com:
 * - debounce de [DEBOUNCE_MS] no texto (digitação rápida não dispara requisição);
 * - texto vazio/curto não chama a rede;
 * - mesma pesquisa (texto + tipo) não é repetida;
 * - pesquisa nova cancela a anterior (`flatMapLatest`).
 * Trocar Filmes/Séries pesquisa na hora, sem debounce.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val query = MutableStateFlow("")
    private val type = MutableStateFlow(SearchType.MOVIES)

    /** Incrementado por [retry] para forçar a mesma pesquisa de novo. */
    private val attempt = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(
                query.map { it.trim() }.distinctUntilChanged().debounce(DEBOUNCE_MS),
                type,
                attempt,
            ) { text, type, attempt -> SearchRequest(text, type, attempt) }
                .distinctUntilChanged()
                .flatMapLatest { request -> contentFor(request) }
                .collect { content -> _uiState.update { it.copy(content = content) } }
        }
    }

    fun onQueryChange(text: String) {
        _uiState.update { it.copy(query = text) }
        query.value = text
    }

    fun onTypeChange(newType: SearchType) {
        _uiState.update { it.copy(type = newType) }
        type.value = newType
    }

    fun retry() {
        attempt.update { it + 1 }
    }

    private fun contentFor(request: SearchRequest): Flow<SearchContent> =
        if (request.text.length < SearchRepository.MIN_QUERY_LENGTH) {
            flowOf(SearchContent.Idle)
        } else {
            flow {
                emit(SearchContent.Loading)
                emit(search(request))
            }
        }

    private suspend fun search(request: SearchRequest): SearchContent {
        val result = when (request.type) {
            SearchType.MOVIES -> repository.searchMovies(request.text)
                .map { page -> page.items.map { it.toSearchResultItem() } }
            SearchType.TV_SHOWS -> repository.searchTvShows(request.text)
                .map { page -> page.items.map { it.toSearchResultItem() } }
        }
        return when (result) {
            is DataResult.Success ->
                if (result.data.isEmpty()) SearchContent.Empty(request.text) else SearchContent.Results(result.data)
            is DataResult.Failure -> SearchContent.Error(result.error)
        }
    }

    private data class SearchRequest(val text: String, val type: SearchType, val attempt: Int)

    companion object {
        const val DEBOUNCE_MS = 400L
    }
}
