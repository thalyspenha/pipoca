package com.thalyspenha.pipoca.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.thalyspenha.pipoca.domain.repository.LibraryViewMode
import com.thalyspenha.pipoca.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Preferências em `SharedPreferences`; o valor em memória é a fonte para a UI (escrita com `apply`). */
@Singleton
class SharedPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) : PreferencesRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val viewMode = MutableStateFlow(
        prefs.getString(KEY_LIBRARY_VIEW_MODE, null)
            ?.let { saved -> LibraryViewMode.entries.firstOrNull { it.name == saved } }
            ?: LibraryViewMode.GRID,
    )

    override val libraryViewMode: Flow<LibraryViewMode> = viewMode.asStateFlow()

    override fun setLibraryViewMode(mode: LibraryViewMode) {
        viewMode.value = mode
        prefs.edit { putString(KEY_LIBRARY_VIEW_MODE, mode.name) }
    }

    private companion object {
        const val FILE = "pipoca_preferences"
        const val KEY_LIBRARY_VIEW_MODE = "library_view_mode"
    }
}
