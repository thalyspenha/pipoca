package com.thalyspenha.pipoca.presentation.screens.home

import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.LibraryMovie
import com.thalyspenha.pipoca.domain.model.LibraryTvShow
import com.thalyspenha.pipoca.domain.model.MovieDetails
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import com.thalyspenha.pipoca.domain.model.TvShowDetails
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.repository.MovieRepository
import com.thalyspenha.pipoca.domain.repository.TvShowRepository
import com.thalyspenha.pipoca.domain.usecase.library.FakeLibraryRepository
import com.thalyspenha.pipoca.util.UiState
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val library = FakeLibraryRepository()
    private val movies = FakeMovieRepository()
    private val tvShows = FakeTvShowRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(token: String = "token") = HomeViewModel(TmdbConfig(apiToken = token), library, movies, tvShows)

    /** Mantém o `stateIn` ativo durante o teste. */
    private fun TestScope.collect(viewModel: HomeViewModel) {
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    private fun HomeViewModel.content() = (uiState.value as UiState.Success).data

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    @Test
    fun `comeca carregando e mostra biblioteca vazia`() = test {
        val viewModel = viewModel()
        assertEquals(UiState.Loading, viewModel.uiState.value)

        collect(viewModel)
        advanceUntilIdle()

        assertTrue(viewModel.content().isLibraryEmpty)
        assertTrue(viewModel.content().tmdbConfigured)
    }

    @Test
    fun `token vazio sinaliza tmdb nao configurado`() = test {
        val viewModel = viewModel(token = " ")
        collect(viewModel)
        advanceUntilIdle()

        assertEquals(false, viewModel.content().tmdbConfigured)
    }

    @Test
    fun `atualiza quando a biblioteca muda`() = test {
        library.movieTitles.value = mapOf(603L to "Matrix")
        val viewModel = viewModel()
        collect(viewModel)
        advanceUntilIdle()

        library.saveMovie(LibraryMovie(603, MovieStatus.WANT_TO_WATCH, addedAt = Instant.EPOCH, updatedAt = Instant.EPOCH))
        advanceUntilIdle()

        assertEquals(listOf("Matrix"), viewModel.content().wantToWatch.map { it.title })
        assertEquals(1, viewModel.content().stats.total)
    }

    @Test
    fun `busca detalhes uma vez para itens sem cache`() = test {
        library.saveMovie(LibraryMovie(603, MovieStatus.WATCHED, addedAt = Instant.EPOCH, updatedAt = Instant.EPOCH))
        library.saveTvShow(LibraryTvShow(1396, TvShowStatus.WATCHING, addedAt = Instant.EPOCH, updatedAt = Instant.EPOCH))
        val viewModel = viewModel()
        collect(viewModel)
        advanceUntilIdle()

        library.saveMovie(LibraryMovie(603, MovieStatus.WATCHED, isFavorite = true, addedAt = Instant.EPOCH, updatedAt = Instant.EPOCH))
        advanceUntilIdle()

        assertEquals(listOf(603L), movies.refreshed)
        assertEquals(listOf(1396L), tvShows.refreshed)
    }

    @Test
    fun `sem token nao busca detalhes`() = test {
        library.saveMovie(LibraryMovie(603, MovieStatus.WATCHED, addedAt = Instant.EPOCH, updatedAt = Instant.EPOCH))
        val viewModel = viewModel(token = "")
        collect(viewModel)
        advanceUntilIdle()

        assertEquals(emptyList<Long>(), movies.refreshed)
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

private class FakeTvShowRepository : TvShowRepository {
    val refreshed = mutableListOf<Long>()
    override fun observeTvShowDetails(id: Long): Flow<TvShowDetails?> = emptyFlow()
    override suspend fun refreshTvShowDetails(id: Long, force: Boolean): DataResult<Unit> {
        refreshed += id
        return DataResult.Success(Unit)
    }
}
