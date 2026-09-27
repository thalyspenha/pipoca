package com.thalyspenha.pipoca.presentation.screens.history

import androidx.lifecycle.SavedStateHandle
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.HistoryEntry
import com.thalyspenha.pipoca.domain.model.HistoryFilter
import com.thalyspenha.pipoca.domain.model.HistoryType
import com.thalyspenha.pipoca.domain.model.MovieDetails
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import com.thalyspenha.pipoca.domain.repository.MovieRepository
import com.thalyspenha.pipoca.domain.usecase.episodes.FakeTvShowRepository
import com.thalyspenha.pipoca.domain.usecase.library.FakeLibraryRepository
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val library = FakeLibraryRepository()
    private val movies = FakeMovieRepository()
    private val savedState = SavedStateHandle()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.started(token: String = "token") =
        HistoryViewModel(savedState, library, TmdbConfig(token), movies, FakeTvShowRepository())
            .also { vm ->
                vm.zone = ZoneOffset.UTC
                backgroundScope.launch { vm.uiState.collect {} }
            }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    private fun movie(id: Long, at: String, title: String? = "Filme $id") =
        HistoryEntry(id, HistoryType.MOVIE, tmdbId = 600 + id, title = title, posterPath = null, watchedAt = Instant.parse(at))

    private fun episode(id: Long, at: String) = HistoryEntry(
        id, HistoryType.EPISODE, tmdbId = 1396, title = "Breaking Bad", posterPath = null,
        episodeName = "Pilot", seasonNumber = 1, episodeNumber = 1, watchedAt = Instant.parse(at),
    )

    @Test
    fun `historico vazio`() = test {
        val vm = started()
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isHistoryEmpty)
    }

    @Test
    fun `agrupa por dia mantendo o mais recente primeiro`() = test {
        library.history.value = listOf(
            episode(3, "2026-09-27T20:00:00Z"),
            movie(2, "2026-09-27T10:00:00Z"),
            movie(1, "2026-09-25T22:00:00Z"),
        )
        val vm = started()
        advanceUntilIdle()

        val days = vm.uiState.value.days
        assertEquals(listOf(LocalDate.of(2026, 9, 27), LocalDate.of(2026, 9, 25)), days.map { it.date })
        assertEquals(listOf(3L, 2L), days[0].entries.map { it.id })
        assertEquals(3, vm.uiState.value.totalCount)
    }

    @Test
    fun `filtros filmes e series`() = test {
        library.history.value = listOf(episode(2, "2026-09-27T20:00:00Z"), movie(1, "2026-09-27T10:00:00Z"))
        val vm = started()

        vm.onFilterChange(HistoryFilter.MOVIES)
        advanceUntilIdle()
        assertEquals(listOf(1L), vm.uiState.value.days.flatMap { it.entries }.map { it.id })

        vm.onFilterChange(HistoryFilter.TV_SHOWS)
        advanceUntilIdle()
        assertEquals(listOf(2L), vm.uiState.value.days.flatMap { it.entries }.map { it.id })
        assertFalse(vm.uiState.value.isHistoryEmpty)
        assertEquals(HistoryFilter.TV_SHOWS, savedState.get<HistoryFilter>("filter"))
    }

    @Test
    fun `atualiza quando o historico muda`() = test {
        val vm = started()
        advanceUntilIdle()

        library.history.value = listOf(movie(1, "2026-09-27T10:00:00Z"))
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.totalCount)
    }

    @Test
    fun `sem titulo busca detalhes uma vez por titulo, so com token`() = test {
        library.history.value = listOf(
            movie(1, "2026-09-27T10:00:00Z", title = null),
            HistoryEntry(2, HistoryType.MOVIE, 601, null, null, watchedAt = Instant.parse("2026-09-26T10:00:00Z")),
        )
        started()
        advanceUntilIdle()
        assertEquals(listOf(601L), movies.refreshed)

        movies.refreshed.clear()
        started(token = "")
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
