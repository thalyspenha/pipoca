package com.thalyspenha.pipoca.presentation.screens.collection

import androidx.lifecycle.SavedStateHandle
import com.thalyspenha.pipoca.data.repository.MutableClock
import com.thalyspenha.pipoca.domain.model.CollectionFilter
import com.thalyspenha.pipoca.domain.model.CollectionItemDraft
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.CollectionSort
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.MediaFormat
import com.thalyspenha.pipoca.domain.model.MovieDetails
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import com.thalyspenha.pipoca.domain.repository.MovieRepository
import com.thalyspenha.pipoca.domain.usecase.collection.AddCollectionItemUseCase
import com.thalyspenha.pipoca.domain.usecase.collection.FakeCollectionRepository
import com.thalyspenha.pipoca.domain.usecase.cache.FetchMissingDetailsUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.FakeSeasonRepository
import com.thalyspenha.pipoca.domain.usecase.episodes.FakeTvShowRepository
import com.thalyspenha.pipoca.domain.usecase.episodes.RefreshShowEpisodesUseCase
import com.thalyspenha.pipoca.presentation.navigation.MovieDetailsRoute
import com.thalyspenha.pipoca.presentation.navigation.TvShowDetailsRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeCollectionRepository()
    private val movies = FakeMovieRepository()
    private val tvShows = FakeTvShowRepository()
    private val clock = MutableClock()
    private val add = AddCollectionItemUseCase(repository, clock)
    private val savedState = SavedStateHandle()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.started(token: String = "token") = CollectionViewModel(
        savedState,
        repository,
        FetchMissingDetailsUseCase(
            TmdbConfig(token), movies, tvShows, RefreshShowEpisodesUseCase(tvShows, FakeSeasonRepository()),
        ),
    ).also { vm -> backgroundScope.launch { vm.uiState.collect {} } }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    private suspend fun addItem(id: Long, format: MediaFormat, title: String?, type: CollectionMediaType = CollectionMediaType.MOVIE) {
        title?.let { repository.titles[type to id] = it }
        add(CollectionItemDraft(tmdbId = id, mediaType = type, format = format))
        clock.advance(Duration.ofMinutes(1))
    }

    @Test
    fun `colecao vazia`() = test {
        val vm = started()
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isCollectionEmpty)
    }

    @Test
    fun `ordena por titulo por padrao e troca ordenacao`() = test {
        addItem(1, MediaFormat.BLURAY, "Zodíaco")
        addItem(2, MediaFormat.DVD, "Alien")
        val vm = started()
        advanceUntilIdle()

        assertEquals(listOf("Alien", "Zodíaco"), vm.uiState.value.items.map { it.title })

        vm.onSortChange(CollectionSort.RECENTLY_ADDED)
        advanceUntilIdle()
        assertEquals(listOf("Alien", "Zodíaco"), vm.uiState.value.items.map { it.title })
        assertEquals(CollectionSort.RECENTLY_ADDED, vm.uiState.value.sort)
    }

    @Test
    fun `filtro vazio nao e colecao vazia`() = test {
        addItem(1, MediaFormat.BLURAY, "Matrix")
        val vm = started()
        advanceUntilIdle()

        vm.onFilterChange(CollectionFilter.DVD)
        advanceUntilIdle()

        assertEquals(emptyList<CollectionListItem>(), vm.uiState.value.items)
        assertFalse(vm.uiState.value.isCollectionEmpty)
        assertEquals(1, vm.uiState.value.totalCount)
    }

    @Test
    fun `filtro e ordenacao ficam no SavedStateHandle`() = test {
        val vm = started()
        vm.onFilterChange(CollectionFilter.UHD_4K)
        vm.onSortChange(CollectionSort.ACQUISITION_DATE)

        assertEquals(CollectionFilter.UHD_4K, savedState.get<CollectionFilter>("filter"))
        assertEquals(CollectionSort.ACQUISITION_DATE, savedState.get<CollectionSort>("sort"))
    }

    @Test
    fun `atualiza quando a colecao muda`() = test {
        val vm = started()
        advanceUntilIdle()

        addItem(1, MediaFormat.UHD_4K_BLURAY, "Matrix")
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.items.size)
        assertEquals(MediaFormat.UHD_4K_BLURAY, vm.uiState.value.items[0].format)
    }

    @Test
    fun `item sem cache busca detalhes uma vez, so com token`() = test {
        addItem(603, MediaFormat.BLURAY, title = null)
        addItem(603, MediaFormat.DVD, title = null)
        started()
        advanceUntilIdle()
        assertEquals(listOf(603L), movies.refreshed)

        movies.refreshed.clear()
        started(token = "")
        advanceUntilIdle()
        assertEquals(emptyList<Long>(), movies.refreshed)
    }

    @Test
    fun `rota de detalhes conforme o tipo`() {
        fun item(type: CollectionMediaType) =
            CollectionListItem(1, 7, type, "T", null, MediaFormat.DVD, null, 1)

        assertEquals(MovieDetailsRoute(7), item(CollectionMediaType.MOVIE).detailsRoute())
        assertEquals(TvShowDetailsRoute(7), item(CollectionMediaType.TV_SHOW).detailsRoute())
    }
}

private class FakeMovieRepository : MovieRepository {
    val refreshed = mutableListOf<Long>()
    override fun observeMovieDetails(id: Long): Flow<MovieDetails?> = emptyFlow()
    override suspend fun refreshMovieDetails(id: Long, force: Boolean): DataResult<Unit> {
        refreshed += id
        return DataResult.Success(Unit)
    }
}
