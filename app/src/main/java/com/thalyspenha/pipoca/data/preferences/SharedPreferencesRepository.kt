package com.thalyspenha.pipoca.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.thalyspenha.pipoca.domain.model.ThemeMode
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

    private val viewMode = MutableStateFlow(prefs.getEnum(KEY_LIBRARY_VIEW_MODE, LibraryViewMode.GRID))

    private val theme = MutableStateFlow(prefs.getEnum(KEY_THEME_MODE, ThemeMode.SYSTEM))

    override val libraryViewMode: Flow<LibraryViewMode> = viewMode.asStateFlow()

    override fun setLibraryViewMode(mode: LibraryViewMode) {
        viewMode.value = mode
        prefs.edit { putString(KEY_LIBRARY_VIEW_MODE, mode.name) }
    }

    override val themeMode: Flow<ThemeMode> = theme.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        theme.value = mode
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
    }

    private companion object {
        const val FILE = "pipoca_preferences"
        const val KEY_LIBRARY_VIEW_MODE = "library_view_mode"
        const val KEY_THEME_MODE = "theme_mode"
    }
}

/** Valor salvo desconhecido (versão antiga/renomeado) cai no padrão. */
private inline fun <reified T : Enum<T>> SharedPreferences.getEnum(key: String, default: T): T =
    getString(key, null)?.let { saved -> enumValues<T>().firstOrNull { it.name == saved } } ?: default
