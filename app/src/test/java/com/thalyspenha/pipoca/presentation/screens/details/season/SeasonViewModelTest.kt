package com.thalyspenha.pipoca.presentation.screens.details.season

import androidx.lifecycle.SavedStateHandle
import com.thalyspenha.pipoca.data.repository.MutableClock
import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.usecase.episodes.FakeSeasonRepository
import com.thalyspenha.pipoca.domain.usecase.episodes.FakeTvShowRepository
import com.thalyspenha.pipoca.domain.usecase.episodes.MarkEpisodeWatchedUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.MarkSeasonWatchedUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.ObserveShowProgressUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.SyncShowStatusUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.UnmarkEpisodeUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.UnmarkSeasonUseCase
import com.thalyspenha.pipoca.domain.usecase.library.FakeLibraryRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SeasonViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val shows = FakeTvShowRepository()
    private val seasons = FakeSeasonRepository()
    private val library = FakeLibraryRepository()
    private val clock = MutableClock() // 2026-09-26

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.started(): SeasonViewModel {
        val progress = ObserveShowProgressUseCase(shows, seasons, library, clock)
        val sync = SyncShowStatusUseCase(library, progress, SetTvShowStatusUseCase(library, clock))
        return SeasonViewModel(
            savedStateHandle = SavedStateHandle(mapOf("showId" to SHOW_ID, "seasonNumber" to 1)),
            tvShowRepository = shows,
            seasonRepository = seasons,
            library = library,
            markEpisode = MarkEpisodeWatchedUseCase(library, sync, clock),
            unmarkEpisode = UnmarkEpisodeUseCase(library, sync),
            markSeason = MarkSeasonWatchedUseCase(library, seasons, sync, clock),
            unmarkSeason = UnmarkSeasonUseCase(library, sync),
            clock = clock,
        ).also { vm -> backgroundScope.launch { vm.uiState.collect {} } }
    }

    private fun SeasonViewModel.success() = uiState.value as SeasonUiState.Success

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    private fun ep(number: Int, airDate: LocalDate? = LocalDate.of(2010, 1, number)) = Episode(
        id = 100L + number, showId = SHOW_ID, seasonNumber = 1, episodeNumber = number, name = "E$number",
        overview = null, stillPath = null, airDate = airDate, runtimeMinutes = 47,
    )

    @Test
    fun `lista episodios com futuros nao marcaveis`() = test {
        seasons.episodes.value = listOf(ep(1), ep(2), ep(3, LocalDate.of(2030, 1, 1)))
        val vm = started()
        advanceUntilIdle()

        val rows = vm.success().episodes
        assertEquals(listOf(true, true, false), rows.map { it.isAired })
        assertEquals(2, vm.success().aired)
        assertEquals(0, vm.success().watched)
        assertEquals(listOf(1), seasons.refreshed)
    }

    @Test
    fun `sem cache e erro na rede mostra erro`() = test {
        seasons.failOn = 1
        val vm = started()
        advanceUntilIdle()

        assertEquals(SeasonUiState.Error(DataError.Network), vm.uiState.value)
    }

    @Test
    fun `marcar e desmarcar episodio`() = test {
        seasons.episodes.value = listOf(ep(1), ep(2))
        val vm = started()
        advanceUntilIdle()

        vm.onEpisodeToggle(ep(1), true)
        advanceUntilIdle()
        assertTrue(vm.success().episodes[0].isWatched)
        assertEquals(TvShowStatus.WATCHING, library.tvShows.value[SHOW_ID]?.status)

        vm.onEpisodeToggle(ep(1), false)
        advanceUntilIdle()
        assertFalse(vm.success().episodes[0].isWatched)
    }

    @Test
    fun `marcar e desmarcar temporada inteira`() = test {
        seasons.episodes.value = listOf(ep(1), ep(2), ep(3, null))
        val vm = started()
        advanceUntilIdle()

        vm.onMarkSeason()
        advanceUntilIdle()
        assertTrue(vm.success().allAiredWatched)
        assertEquals(2, vm.success().watched)

        vm.onUnmarkSeason()
        advanceUntilIdle()
        assertEquals(0, vm.success().watched)
    }

    private companion object {
        const val SHOW_ID = 1396L
    }
}
