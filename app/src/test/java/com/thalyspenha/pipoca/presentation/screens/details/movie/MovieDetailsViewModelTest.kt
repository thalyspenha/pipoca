package com.thalyspenha.pipoca.presentation.screens.details.movie

import androidx.lifecycle.SavedStateHandle
import com.thalyspenha.pipoca.data.repository.MutableClock
import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.MovieDetails
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.repository.MovieRepository
import com.thalyspenha.pipoca.domain.usecase.library.FakeLibraryRepository
import com.thalyspenha.pipoca.domain.usecase.library.RemoveMovieFromLibraryUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetMovieFavoriteUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetMovieRatingUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetMovieStatusUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val movies = FakeMovieRepository()
    private val library = FakeLibraryRepository()
    private val clock = MutableClock()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = MovieDetailsViewModel(
        savedStateHandle = SavedStateHandle(mapOf("id" to MOVIE_ID)),
        movieRepository = movies,
        library = library,
        setStatus = SetMovieStatusUseCase(library, clock),
        removeFromLibrary = RemoveMovieFromLibraryUseCase(library),
        setFavorite = SetMovieFavoriteUseCase(library, clock),
        setRating = SetMovieRatingUseCase(library, clock),
    )

    /** Mantém o `stateIn` ativo durante o teste. */
    private fun TestScope.started(): MovieDetailsViewModel = viewModel().also { vm ->
        backgroundScope.launch { vm.uiState.collect {} }
    }

    private fun MovieDetailsViewModel.success() = uiState.value as MovieDetailsUiState.Success

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    @Test
    fun `sem cache fica carregando ate o refresh gravar`() = test {
        val gate = CompletableDeferred<Unit>()
        movies.onRefresh = {
            gate.await()
            movies.details.value = matrix()
            DataResult.Success(Unit)
        }
        val vm = started()
        advanceUntilIdle()
        assertEquals(MovieDetailsUiState.Loading, vm.uiState.value)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(matrix(), vm.success().movie)
        assertFalse(vm.success().isRefreshing)
    }

    @Test
    fun `sem cache e refresh falhando mostra erro`() = test {
        movies.onRefresh = { DataResult.Failure(DataError.Network) }
        val vm = started()
        advanceUntilIdle()

        assertEquals(MovieDetailsUiState.Error(DataError.Network), vm.uiState.value)
    }

    @Test
    fun `com cache, falha do refresh nao bloqueia`() = test {
        movies.details.value = matrix()
        movies.onRefresh = { DataResult.Failure(DataError.Network) }
        val vm = started()
        advanceUntilIdle()

        assertEquals(matrix(), vm.success().movie)
        assertEquals(DataError.Network, vm.success().refreshError)

        vm.dismissRefreshError()
        advanceUntilIdle()
        assertNull(vm.success().refreshError)
    }

    @Test
    fun `retry depois do erro busca de novo`() = test {
        movies.onRefresh = { DataResult.Failure(DataError.Network) }
        val vm = started()
        advanceUntilIdle()

        movies.onRefresh = {
            movies.details.value = matrix()
            DataResult.Success(Unit)
        }
        vm.refresh()
        advanceUntilIdle()

        assertEquals(2, movies.refreshCalls)
        assertEquals(matrix(), vm.success().movie)
    }

    @Test
    fun `filme fora da biblioteca`() = test {
        movies.details.value = matrix()
        val vm = started()
        advanceUntilIdle()

        assertEquals(PersonalMovie(), vm.success().personal)
        assertFalse(vm.success().personal.inLibrary)
    }

    @Test
    fun `marcar assistido grava e reflete na tela com historico`() = test {
        movies.details.value = matrix()
        val vm = started()
        advanceUntilIdle()

        vm.onStatusClick(MovieStatus.WATCHED)
        advanceUntilIdle()

        assertEquals(MovieStatus.WATCHED, vm.success().personal.status)
        assertEquals(1, library.movieWatches[MOVIE_ID]!!.size)
    }

    @Test
    fun `trocar de quero assistir para assistido`() = test {
        movies.details.value = matrix()
        val vm = started()
        advanceUntilIdle()

        vm.onStatusClick(MovieStatus.WANT_TO_WATCH)
        advanceUntilIdle()
        assertEquals(MovieStatus.WANT_TO_WATCH, vm.success().personal.status)

        vm.onStatusClick(MovieStatus.WATCHED)
        advanceUntilIdle()
        assertEquals(MovieStatus.WATCHED, vm.success().personal.status)
    }

    @Test
    fun `tocar no status atual tira da biblioteca`() = test {
        movies.details.value = matrix()
        val vm = started()
        advanceUntilIdle()
        vm.onStatusClick(MovieStatus.WANT_TO_WATCH)
        advanceUntilIdle()

        vm.onStatusClick(MovieStatus.WANT_TO_WATCH)
        advanceUntilIdle()

        assertFalse(vm.success().personal.inLibrary)
        assertNull(library.movies.value[MOVIE_ID])
    }

    @Test
    fun `favoritar alterna`() = test {
        movies.details.value = matrix()
        val vm = started()
        advanceUntilIdle()

        vm.onFavoriteClick()
        advanceUntilIdle()
        assertTrue(vm.success().personal.isFavorite)

        vm.onFavoriteClick()
        advanceUntilIdle()
        assertFalse(vm.success().personal.isFavorite)
    }

    @Test
    fun `avaliar e remover nota`() = test {
        movies.details.value = matrix()
        val vm = started()
        advanceUntilIdle()

        vm.onRatingChange(8)
        advanceUntilIdle()
        assertEquals(8, vm.success().personal.rating)

        vm.onRatingChange(null)
        advanceUntilIdle()
        assertNull(vm.success().personal.rating)
    }

    private companion object {
        const val MOVIE_ID = 603L

        fun matrix() = MovieDetails(
            id = MOVIE_ID, title = "Matrix", originalTitle = "The Matrix", overview = "Neo...",
            posterPath = "/p.jpg", backdropPath = "/b.jpg", releaseDate = LocalDate.of(1999, 3, 31),
            runtimeMinutes = 136, voteAverage = 8.2, genres = emptyList(),
            directors = listOf("Lana Wachowski"), cast = emptyList(),
        )
    }
}

private class FakeMovieRepository : MovieRepository {
    val details = MutableStateFlow<MovieDetails?>(null)
    var refreshCalls = 0
    var onRefresh: suspend () -> DataResult<Unit> = { DataResult.Success(Unit) }

    override fun observeMovieDetails(id: Long): Flow<MovieDetails?> = details

    override suspend fun refreshMovieDetails(id: Long, force: Boolean): DataResult<Unit> {
        refreshCalls++
        return onRefresh()
    }
}
