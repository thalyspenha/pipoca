package com.thalyspenha.pipoca.util

/** Estado genérico de tela. Telas complexas podem usar uma data class própria. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>
}
