package com.thalyspenha.pipoca.presentation.screens.settings

import com.thalyspenha.pipoca.domain.model.AppInfo
import com.thalyspenha.pipoca.domain.model.CacheClearResult
import com.thalyspenha.pipoca.domain.model.DatabaseInfo
import com.thalyspenha.pipoca.domain.model.ThemeMode
import com.thalyspenha.pipoca.domain.repository.LibraryViewMode
import com.thalyspenha.pipoca.domain.repository.MaintenanceRepository
import com.thalyspenha.pipoca.domain.repository.PreferencesRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private class FakePreferences : PreferencesRepository {
    override val libraryViewMode = MutableStateFlow(LibraryViewMode.GRID)
    override fun setLibraryViewMode(mode: LibraryViewMode) {
        libraryViewMode.value = mode
    }

    override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }
}

private class FakeMaintenance : MaintenanceRepository {
    val info = MutableStateFlow(databaseInfo(movies = 1))
    var clearCalls = 0
    var gate: CompletableDeferred<Unit>? = null
    var failure: Exception? = null

    override fun observeDatabaseInfo() = info

    override suspend fun clearCache(): CacheClearResult {
        clearCalls++
        gate?.await()
        failure?.let { throw it }
        return CacheClearResult(movies = 2, tvShows = 1)
    }
}

private fun databaseInfo(movies: Int) = DatabaseInfo(4, movies, 0, 0, 0, 0, 0, 0, 0, 1024)

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val preferences = FakePreferences()
    private val maintenance = FakeMaintenance()
    private val appInfo = AppInfo("1.2.3", 7)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingsViewModel(preferences, maintenance, appInfo)

    @Test
    fun `mostra tema, banco e versão e acompanha mudanças`() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, vm.uiState.value.themeMode)
        assertEquals(1, vm.uiState.value.databaseInfo?.movies)
        assertEquals(appInfo, vm.uiState.value.appInfo)

        maintenance.info.value = databaseInfo(movies = 5)
        advanceUntilIdle()
        assertEquals(5, vm.uiState.value.databaseInfo?.movies)
    }

    @Test
    fun `escolher tema grava a preferência`() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, preferences.themeMode.value)
        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)
    }

    @Test
    fun `limpar cache informa o resultado e ignora toques repetidos`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>().also { maintenance.gate = it }
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.clearCache()
        vm.clearCache()
        advanceUntilIdle()
        assertEquals(CacheClearState.Running, vm.uiState.value.cacheClear)
        assertEquals(1, maintenance.clearCalls)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(CacheClearState.Done(CacheClearResult(2, 1)), vm.uiState.value.cacheClear)

        vm.onCacheClearMessageShown()
        advanceUntilIdle()
        assertEquals(CacheClearState.Idle, vm.uiState.value.cacheClear)
    }

    @Test
    fun `falha ao limpar cache vira mensagem`() = runTest(dispatcher) {
        maintenance.failure = IllegalStateException("disco")
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.clearCache()
        advanceUntilIdle()
        assertEquals(CacheClearState.Failed, vm.uiState.value.cacheClear)
    }

    @Test
    fun `erro ao ler o banco não derruba a tela`() = runTest(dispatcher) {
        val vm = SettingsViewModel(
            preferences,
            object : MaintenanceRepository {
                override fun observeDatabaseInfo() = kotlinx.coroutines.flow.flow<DatabaseInfo> { error("io") }
                override suspend fun clearCache() = CacheClearResult(0, 0)
            },
            appInfo,
        )
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertNull(vm.uiState.value.databaseInfo)
    }

    @Test
    fun `tamanho em KB e MB`() {
        assertEquals("1,0 KB", formatBytes(1024))
        assertEquals("1,5 MB", formatBytes(1_572_864))
    }
}
