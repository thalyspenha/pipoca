package com.thalyspenha.pipoca.presentation.screens.collection.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.MediaFormat
import com.thalyspenha.pipoca.domain.repository.CollectionRepository
import com.thalyspenha.pipoca.domain.repository.MovieRepository
import com.thalyspenha.pipoca.domain.repository.TvShowRepository
import com.thalyspenha.pipoca.domain.usecase.collection.AddCollectionItemUseCase
import com.thalyspenha.pipoca.domain.usecase.collection.RemoveCollectionItemUseCase
import com.thalyspenha.pipoca.domain.usecase.collection.UpdateCollectionItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Adicionar/editar/remover item da coleção (D-042). Não toca na biblioteca (D-039). */
@HiltViewModel
class CollectionItemFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CollectionRepository,
    movieRepository: MovieRepository,
    tvShowRepository: TvShowRepository,
    private val add: AddCollectionItemUseCase,
    private val update: UpdateCollectionItemUseCase,
    private val remove: RemoveCollectionItemUseCase,
    private val clock: Clock,
) : ViewModel() {

    /** Argumentos de `CollectionItemFormRoute`. */
    private val tmdbId: Long = checkNotNull(savedStateHandle.get<Long>("tmdbId"))
    private val mediaType = CollectionMediaType.valueOf(checkNotNull(savedStateHandle.get<String>("mediaType")))
    private val itemId: Long = savedStateHandle.get<Long>("itemId") ?: 0

    private val _state = MutableStateFlow(
        CollectionItemFormState(tmdbId = tmdbId, mediaType = mediaType, isEditing = itemId != 0L, isLoading = itemId != 0L),
    )
    val state: StateFlow<CollectionItemFormState> = _state.asStateFlow()

    val today: LocalDate get() = LocalDate.now(clock)

    init {
        viewModelScope.launch {
            val title = when (mediaType) {
                CollectionMediaType.MOVIE -> movieRepository.observeMovieDetails(tmdbId).first()?.title
                CollectionMediaType.TV_SHOW -> tvShowRepository.observeTvShowDetails(tmdbId).first()?.name
            }
            _state.update { it.copy(title = title) }
        }
        if (itemId != 0L) {
            viewModelScope.launch {
                val item = repository.getItem(itemId)
                _state.update { current ->
                    // Item apagado em outro lugar: nada a editar, volta.
                    if (item == null) current.copy(isLoading = false, isDone = true)
                    else current.withItem(item).copy(isLoading = false)
                }
            }
        }
    }

    fun onFormatChange(format: MediaFormat) = _state.update { it.copy(format = format) }
    fun onEditionChange(text: String) = _state.update { it.copy(edition = text) }
    fun onRegionChange(text: String) = _state.update { it.copy(region = text) }
    fun onQuantityChange(text: String) = _state.update { it.copy(quantity = text.filter(Char::isDigit).take(2)) }
    fun onAcquiredAtChange(date: LocalDate?) = _state.update { it.copy(acquiredAt = date) }
    fun onNotesChange(text: String) = _state.update { it.copy(notes = text) }

    fun onSave() {
        val current = _state.value
        if (current.isLoading || current.isDone) return
        if (!current.isValid(today)) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            if (current.isEditing) update(itemId, current.toDraft()) else add(current.toDraft())
            _state.update { it.copy(isLoading = false, isDone = true) }
        }
    }

    fun onDelete() {
        if (itemId == 0L || _state.value.isDone) return
        viewModelScope.launch {
            remove(itemId)
            _state.update { it.copy(isDone = true) }
        }
    }
}
