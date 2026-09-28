package com.thalyspenha.pipoca.domain.repository

import kotlinx.coroutines.flow.Flow

/** Modo de exibição da Biblioteca. */
enum class LibraryViewMode { GRID, LIST }

/** Preferências locais simples do usuário (D-050: `SharedPreferences`, sem dependência nova). */
interface PreferencesRepository {
    val libraryViewMode: Flow<LibraryViewMode>

    fun setLibraryViewMode(mode: LibraryViewMode)
}
