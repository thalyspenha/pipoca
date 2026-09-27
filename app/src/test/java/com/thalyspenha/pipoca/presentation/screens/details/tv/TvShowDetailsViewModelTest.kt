package com.thalyspenha.pipoca.presentation.screens.details.tv

import androidx.lifecycle.SavedStateHandle
import com.thalyspenha.pipoca.data.repository.MutableClock
import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.TvShowDetails
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.repository.TvShowRepository
import com.thalyspenha.pipoca.domain.usecase.library.FakeLibraryRepository
import com.thalyspenha.pipoca.domain.usecase.library.RemoveTvShowFromLibraryUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowFavoriteUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowRatingUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowStatusUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class TvShowDetailsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val shows = FakeTvShowRepository()
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

    private fun TestScope.started(): TvShowDetailsViewModel = TvShowDetailsViewModel(
        savedStateHandle = SavedStateHandle(mapOf("id" to SHOW_ID)),
        tvShowRepository = shows,
        library = library,
        setStatus = SetTvShowStatusUseCase(library, clock),
        removeFromLibrary = RemoveTvShowFromLibraryUseCase(library),
        setFavorite = SetTvShowFavoriteUseCase(library, clock),
        setRating = SetTvShowRatingUseCase(library, clock),
    ).also { vm -> backgroundScope.launch { vm.uiState.collect {} } }

    private fun TvShowDetailsViewModel.success() = uiState.value as TvShowDetailsUiState.Success

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    @Test
    fun `sem cache e refresh falhando mostra erro`() = test {
        shows.onRefresh = { DataResult.Failure(DataError.NotFound) }
        val vm = started()
        advanceUntilIdle()

        assertEquals(TvShowDetailsUiState.Error(DataError.NotFound), vm.uiState.value)
    }

    @Test
    fun `refresh grava e mostra a serie`() = test {
        shows.onRefresh = {
            shows.details.value = breakingBad()
            DataResult.Success(Unit)
        }
        val vm = started()
        advanceUntilIdle()

        assertEquals(breakingBad(), vm.success().show)
        assertFalse(vm.success().personal.inLibrary)
    }

    @Test
    fun `com cache, falha do refresh nao bloqueia`() = test {
        shows.details.value = breakingBad()
        shows.onRefresh = { DataResult.Failure(DataError.Network) }
        val vm = started()
        advanceUntilIdle()

        assertEquals(DataError.Network, vm.success().refreshError)
    }

    @Test
    fun `status quero ver, assistindo, concluida e tocar de novo remove`() = test {
        shows.details.value = breakingBad()
        val vm = started()
        advanceUntilIdle()

        for (status in TV_SHOW_SELECTABLE_STATUSES) {
            vm.onStatusClick(status)
            advanceUntilIdle()
            assertEquals(status, vm.success().personal.status)
        }

        vm.onStatusClick(TvShowStatus.COMPLETED)
        advanceUntilIdle()
        assertNull(library.tvShows.value[SHOW_ID])
    }

    @Test
    fun `favoritar e avaliar`() = test {
        shows.details.value = breakingBad()
        val vm = started()
        advanceUntilIdle()

        vm.onFavoriteClick()
        vm.onRatingChange(10)
        advanceUntilIdle()

        assertTrue(vm.success().personal.isFavorite)
        assertEquals(10, vm.success().personal.rating)
        assertEquals(TvShowStatus.WANT_TO_WATCH, vm.success().personal.status)
    }

    @Test
    fun `status de producao do TMDB em portugues`() {
        assertEquals("Finalizada", tmdbStatusLabel("Ended"))
        assertEquals("Em exibição", tmdbStatusLabel("Returning Series"))
        assertEquals("Algo novo", tmdbStatusLabel("Algo novo"))
    }

    private companion object {
        const val SHOW_ID = 1396L

        fun breakingBad() = TvShowDetails(
            id = SHOW_ID, name = "Breaking Bad", originalName = "Breaking Bad", overview = null,
            posterPath = null, backdropPath = null, firstAirDate = null, tmdbStatus = "Ended",
            numberOfSeasons = 5, numberOfEpisodes = 62, episodeRunTime = null, voteAverage = 8.9,
            genres = emptyList(), creators = emptyList(), seasons = emptyList(), cast = emptyList(),
        )
    }
}

private class FakeTvShowRepository : TvShowRepository {
    val details = MutableStateFlow<TvShowDetails?>(null)
    var onRefresh: suspend () -> DataResult<Unit> = { DataResult.Success(Unit) }

    override fun observeTvShowDetails(id: Long): Flow<TvShowDetails?> = details

    override suspend fun refreshTvShowDetails(id: Long, force: Boolean): DataResult<Unit> = onRefresh()
}
