package com.thalyspenha.pipoca.presentation.screens.stats

import com.thalyspenha.pipoca.data.repository.MutableClock
import com.thalyspenha.pipoca.domain.repository.StatsRepository
import com.thalyspenha.pipoca.domain.stats.EpisodeStats
import com.thalyspenha.pipoca.domain.stats.MovieStats
import com.thalyspenha.pipoca.domain.stats.Statistics
import com.thalyspenha.pipoca.domain.stats.TvShowStats
import com.thalyspenha.pipoca.domain.stats.WatchTime
import com.thalyspenha.pipoca.domain.usecase.stats.ObserveStatisticsUseCase
import com.thalyspenha.pipoca.util.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun stats(watched: Int) = Statistics(
        movies = MovieStats(watched, 0, 0, 0),
        tvShows = TvShowStats(0, 0, 0, 0),
        episodes = EpisodeStats(0, 0, 0),
        watchTime = WatchTime(0, 0, 0),
        genres = emptyList(),
        ratings = emptyList(),
        collection = emptyList(),
    )

    private fun viewModel(repository: StatsRepository) =
        StatsViewModel(ObserveStatisticsUseCase(repository, MutableClock(), ZoneOffset.UTC))

    @Test
    fun `carrega e atualiza quando os dados mudam`() = runTest(dispatcher) {
        val source = MutableStateFlow(stats(1))
        val vm = viewModel(object : StatsRepository {
            override fun observeStatistics(monthStart: Long, yearStart: Long): Flow<Statistics> = source
        })
        assertEquals(UiState.Loading, vm.uiState.value)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(1, (vm.uiState.value as UiState.Success).data.movies.watched)

        source.value = stats(2)
        advanceUntilIdle()
        assertEquals(2, (vm.uiState.value as UiState.Success).data.movies.watched)
    }

    @Test
    fun `falha vira erro`() = runTest(dispatcher) {
        val vm = viewModel(object : StatsRepository {
            override fun observeStatistics(monthStart: Long, yearStart: Long): Flow<Statistics> =
                flow { throw IllegalStateException("db") }
        })
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(vm.uiState.value is UiState.Error)
    }
}
