package com.thalyspenha.pipoca.presentation.screens.favorites

import androidx.lifecycle.SavedStateHandle
import com.thalyspenha.pipoca.data.repository.MutableClock
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.usecase.library.FakeLibraryRepository
import com.thalyspenha.pipoca.domain.usecase.library.SetMovieFavoriteUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetMovieStatusUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowFavoriteUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowStatusUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val library = FakeLibraryRepository()
    private val clock = MutableClock()
    private val setMovieFavorite = SetMovieFavoriteUseCase(library, clock)
    private val setTvShowFavorite = SetTvShowFavoriteUseCase(library, clock)
    private val savedState = SavedStateHandle()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.started() = FavoritesViewModel(savedState, library, setMovieFavorite, setTvShowFavorite)
        .also { vm -> backgroundScope.launch { vm.uiState.collect {} } }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    @Test
    fun `lista so favoritos, separados por tipo, com status`() = test {
        library.movieTitles.value = mapOf(603L to "Matrix", 550L to "Clube da Luta")
        SetMovieStatusUseCase(library, clock)(603, MovieStatus.WATCHED)
        setMovieFavorite(603, true)
        SetMovieStatusUseCase(library, clock)(550, MovieStatus.WANT_TO_WATCH) // não favorito
        SetTvShowStatusUseCase(library, clock)(1396, TvShowStatus.WATCHING)
        setTvShowFavorite(1396, true)
        val vm = started()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf("Matrix"), state.movies.map { it.title })
        assertEquals("Assistido", state.movies[0].statusLabel)
        assertEquals(listOf(1396L), state.tvShows.map { it.id })
        assertEquals("Assistindo", state.tvShows[0].statusLabel)
        assertEquals(state.movies, state.items)
    }

    @Test
    fun `aba de series e guardada`() = test {
        val vm = started()
        vm.onTabChange(FavoritesTab.TV_SHOWS)
        advanceUntilIdle()

        assertEquals(FavoritesTab.TV_SHOWS, vm.uiState.value.tab)
        assertEquals(FavoritesTab.TV_SHOWS, savedState.get<FavoritesTab>("tab"))
    }

    @Test
    fun `remover tira dos favoritos mas mantem na biblioteca, desfazer volta`() = test {
        setMovieFavorite(603, true)
        val vm = started()
        advanceUntilIdle()
        val item = vm.uiState.value.movies.single()

        vm.onRemove(item)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.movies.isEmpty())
        assertEquals(false, library.movies.value[603]?.isFavorite)

        clock.advance(Duration.ofSeconds(2))
        vm.undoRemove(item)
        advanceUntilIdle()
        assertEquals(listOf(603L), vm.uiState.value.movies.map { it.id })
    }

    @Test
    fun `remover serie favorita`() = test {
        setTvShowFavorite(1396, true)
        val vm = started()
        advanceUntilIdle()

        vm.onRemove(vm.uiState.value.tvShows.single())
        advanceUntilIdle()

        assertTrue(vm.uiState.value.tvShows.isEmpty())
        assertEquals(false, library.tvShows.value[1396]?.isFavorite)
    }
}
