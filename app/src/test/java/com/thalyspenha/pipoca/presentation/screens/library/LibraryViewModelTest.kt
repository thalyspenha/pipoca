package com.thalyspenha.pipoca.presentation.screens.library

import androidx.lifecycle.SavedStateHandle
import com.thalyspenha.pipoca.data.repository.MutableClock
import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.LibraryMovie
import com.thalyspenha.pipoca.domain.model.LibrarySort
import com.thalyspenha.pipoca.domain.model.LibraryTvShow
import com.thalyspenha.pipoca.domain.model.MovieLibraryFilter
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.model.TvShowLibraryFilter
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.repository.LibraryViewMode
import com.thalyspenha.pipoca.domain.repository.PreferencesRepository
import com.thalyspenha.pipoca.domain.usecase.episodes.FakeSeasonRepository
import com.thalyspenha.pipoca.domain.usecase.library.FakeLibraryRepository
import com.thalyspenha.pipoca.domain.usecase.librarylist.ObserveLibraryMoviesUseCase
import com.thalyspenha.pipoca.domain.usecase.librarylist.ObserveLibraryTvShowsUseCase
import com.thalyspenha.pipoca.domain.usecase.librarylist.ObserveMovieLibraryCountsUseCase
import com.thalyspenha.pipoca.domain.usecase.librarylist.ObserveTvShowLibraryCountsUseCase
import com.thalyspenha.pipoca.presentation.components.MediaCardProgress
import com.thalyspenha.pipoca.presentation.components.MediaStatusBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

private class FakePreferencesRepository : PreferencesRepository {
    override val libraryViewMode = MutableStateFlow(LibraryViewMode.GRID)

    override fun setLibraryViewMode(mode: LibraryViewMode) {
        libraryViewMode.value = mode
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val library = FakeLibraryRepository()
    private val seasons = FakeSeasonRepository()
    private val preferences = FakePreferencesRepository()
    private val savedState = SavedStateHandle()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.started() = LibraryViewModel(
        savedState,
        ObserveLibraryMoviesUseCase(library),
        ObserveLibraryTvShowsUseCase(library, seasons, MutableClock()),
        ObserveMovieLibraryCountsUseCase(library),
        ObserveTvShowLibraryCountsUseCase(library),
        preferences,
    ).also { vm -> backgroundScope.launch { vm.uiState.collect {} } }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    private suspend fun movie(id: Long, title: String, status: MovieStatus, favorite: Boolean = false) {
        library.saveMovie(LibraryMovie(id, status, isFavorite = favorite, addedAt = Instant.EPOCH, updatedAt = Instant.ofEpochSecond(id)))
        library.movieTitles.value += id to title
    }

    private suspend fun show(id: Long, name: String, status: TvShowStatus) {
        library.saveTvShow(LibraryTvShow(id, status, addedAt = Instant.EPOCH, updatedAt = Instant.ofEpochSecond(id)))
        library.tvShowNames.value += id to name
    }

    private fun ep(showId: Long, number: Int) = Episode(
        id = showId * 100 + number, showId = showId, seasonNumber = 1, episodeNumber = number, name = "E$number",
        overview = null, stillPath = null, airDate = LocalDate.of(2010, 1, number), runtimeMinutes = 45,
    )

    @Test
    fun `biblioteca vazia mostra estado vazio`() = test {
        val vm = started()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isLibraryEmpty)
    }

    @Test
    fun `filmes com status, favorito e contagens`() = test {
        movie(1, "Predator", MovieStatus.WATCHED, favorite = true)
        movie(2, "Alien", MovieStatus.WANT_TO_WATCH)
        val vm = started()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLibraryEmpty)
        assertEquals(2, state.movieCounts.all)
        assertEquals(1, state.movieCounts.of(MovieLibraryFilter.WATCHED))
        val predator = state.items.single { it.id == 1L }
        assertEquals(MediaStatusBadge.WATCHED, predator.status)
        assertTrue(predator.isFavorite)
        assertTrue(predator.isMovie)
        assertNull(predator.progress)
    }

    @Test
    fun `filtro de filmes e mensagem de vazio especifica`() = test {
        movie(1, "Predator", MovieStatus.WANT_TO_WATCH)
        val vm = started()
        vm.onMovieFilterChange(MovieLibraryFilter.WATCHED)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.items.isEmpty())
        assertEquals("Você ainda não marcou nenhum título como assistido.", state.emptyMessage)
    }

    @Test
    fun `pesquisa local filtra pelo titulo`() = test {
        movie(1, "Predator", MovieStatus.WATCHED)
        movie(2, "Predator 2", MovieStatus.WATCHED)
        movie(3, "Alien", MovieStatus.WATCHED)
        val vm = started()
        vm.onQueryChange("predator")
        advanceUntilIdle()

        assertEquals(setOf(1L, 2L), vm.uiState.value.items.map { it.id }.toSet())

        vm.onQueryChange("xyz")
        advanceUntilIdle()
        assertEquals("Nenhum título encontrado para \"xyz\".", vm.uiState.value.emptyMessage)
    }

    @Test
    fun `aba de series mostra progresso e status`() = test {
        show(10, "Breaking Bad", TvShowStatus.WATCHING)
        show(11, "Lost", TvShowStatus.PAUSED)
        seasons.episodes.value = listOf(ep(10, 1), ep(10, 2), ep(10, 3), ep(10, 4))
        library.markEpisodesWatched(listOf(ep(10, 1), ep(10, 2), ep(10, 3)), Instant.EPOCH)
        val vm = started()
        vm.onTabChange(LibraryTab.TV_SHOWS)
        advanceUntilIdle()

        val items = vm.uiState.value.items.associateBy { it.id }
        assertEquals(MediaCardProgress(3, 4, 75), items.getValue(10).progress)
        assertEquals(MediaStatusBadge.WATCHING, items.getValue(10).status)
        assertEquals(MediaStatusBadge.PAUSED, items.getValue(11).status)
        assertNull(items.getValue(11).progress)
        assertEquals(1, vm.uiState.value.tvShowCounts.of(TvShowLibraryFilter.PAUSED))
    }

    @Test
    fun `ordenacoes so de series nao valem para filmes`() = test {
        val vm = started()
        vm.onSortChange(LibrarySort.PROGRESS)
        advanceUntilIdle()
        assertEquals(LibrarySort.RECENTLY_ADDED, vm.uiState.value.selection.sort)
        assertFalse(LibrarySort.PROGRESS in vm.uiState.value.sortOptions)

        vm.onTabChange(LibraryTab.TV_SHOWS)
        vm.onSortChange(LibrarySort.PROGRESS)
        advanceUntilIdle()
        assertEquals(LibrarySort.PROGRESS, vm.uiState.value.selection.sort)
        assertTrue(LibrarySort.PROGRESS in vm.uiState.value.sortOptions)
        assertEquals(LibrarySort.RECENTLY_ADDED, vm.uiState.value.selection.movieSort)
    }

    @Test
    fun `modo grid ou lista vem das preferencias e e salvo`() = test {
        val vm = started()
        advanceUntilIdle()
        assertEquals(LibraryViewMode.GRID, vm.uiState.value.viewMode)

        vm.onViewModeChange(LibraryViewMode.LIST)
        advanceUntilIdle()

        assertEquals(LibraryViewMode.LIST, preferences.libraryViewMode.value)
        assertEquals(LibraryViewMode.LIST, vm.uiState.value.viewMode)
    }

    @Test
    fun `atualiza sozinha quando o status muda`() = test {
        movie(1, "Predator", MovieStatus.WANT_TO_WATCH)
        val vm = started()
        vm.onMovieFilterChange(MovieLibraryFilter.WATCHED)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.items.isEmpty())

        movie(1, "Predator", MovieStatus.WATCHED)
        advanceUntilIdle()

        assertEquals(listOf(1L), vm.uiState.value.items.map { it.id })
        assertEquals(1, vm.uiState.value.movieCounts.watched)
    }

    @Test
    fun `selecao sobrevive a recriacao`() = test {
        started().apply {
            onTabChange(LibraryTab.TV_SHOWS)
            onTvShowFilterChange(TvShowLibraryFilter.DROPPED)
        }
        val vm = started()
        advanceUntilIdle()

        assertEquals(LibraryTab.TV_SHOWS, vm.uiState.value.selection.tab)
        assertEquals(TvShowLibraryFilter.DROPPED, vm.uiState.value.selection.tvShowFilter)
    }
}
