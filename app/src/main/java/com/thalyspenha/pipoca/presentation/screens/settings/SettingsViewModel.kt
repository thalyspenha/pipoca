package com.thalyspenha.pipoca.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalyspenha.pipoca.domain.model.AppInfo
import com.thalyspenha.pipoca.domain.model.CacheClearResult
import com.thalyspenha.pipoca.domain.model.DatabaseInfo
import com.thalyspenha.pipoca.domain.model.ThemeMode
import com.thalyspenha.pipoca.domain.repository.MaintenanceRepository
import com.thalyspenha.pipoca.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Estado da limpeza de cache: mensagem única depois de terminar. */
sealed interface CacheClearState {
    data object Idle : CacheClearState
    data object Running : CacheClearState
    data class Done(val result: CacheClearResult) : CacheClearState
    data object Failed : CacheClearState
}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Nulo enquanto carrega ou se a leitura falhar. */
    val databaseInfo: DatabaseInfo? = null,
    val cacheClear: CacheClearState = CacheClearState.Idle,
    val appInfo: AppInfo,
)

/** Configurações (D-057): tema, limpar cache, banco e versão. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val maintenance: MaintenanceRepository,
    private val appInfo: AppInfo,
) : ViewModel() {

    private val cacheClear = MutableStateFlow<CacheClearState>(CacheClearState.Idle)

    private val databaseInfo = maintenance.observeDatabaseInfo()
        .map<DatabaseInfo, DatabaseInfo?> { it }
        .onStart { emit(null) }
        .catch { emit(null) }

    val uiState: StateFlow<SettingsUiState> =
        combine(preferences.themeMode, databaseInfo, cacheClear) { theme, info, clear ->
            SettingsUiState(themeMode = theme, databaseInfo = info, cacheClear = clear, appInfo = appInfo)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            SettingsUiState(appInfo = appInfo),
        )

    fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)

    fun clearCache() {
        if (cacheClear.value == CacheClearState.Running) return
        cacheClear.value = CacheClearState.Running
        viewModelScope.launch {
            cacheClear.value = try {
                CacheClearState.Done(maintenance.clearCache())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                CacheClearState.Failed
            }
        }
    }

    /** A tela já mostrou o resultado. */
    fun onCacheClearMessageShown() {
        cacheClear.update { if (it is CacheClearState.Running) it else CacheClearState.Idle }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
