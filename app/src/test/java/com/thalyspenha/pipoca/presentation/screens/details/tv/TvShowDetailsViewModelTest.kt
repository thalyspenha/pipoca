package com.thalyspenha.pipoca.presentation.screens.details.tv

import androidx.lifecycle.SavedStateHandle
import com.thalyspenha.pipoca.data.repository.MutableClock
import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.SeasonSummary
import com.thalyspenha.pipoca.domain.model.TvShowDetails
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.usecase.episodes.FakeSeasonRepository
import com.thalyspenha.pipoca.domain.usecase.episodes.FakeTvShowRepository
import com.thalyspenha.pipoca.domain.usecase.episodes.MarkEpisodeWatchedUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.ObserveShowProgressUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.RefreshShowEpisodesUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.SyncShowStatusUseCase
import com.thalyspenha.pipoca.domain.usecase.collection.FakeCollectionRepository
import com.thalyspenha.pipoca.domain.usecase.library.FakeLibraryRepository
import com.thalyspenha.pipoca.domain.usecase.library.RemoveTvShowFromLibraryUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowFavoriteUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowRatingUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowStatusUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
class TvShowDetailsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val shows = FakeTvShowRepository()
    private val seasons = FakeSeasonRepository()
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

    private fun TestScope.started(): TvShowDetailsViewModel {
        val progress = ObserveShowProgressUseCase(shows, seasons, library, clock)
        val setStatus = SetTvShowStatusUseCase(library, clock)
        return TvShowDetailsViewModel(
            savedStateHandle = SavedStateHandle(mapOf("id" to SHOW_ID)),
            tvShowRepository = shows,
            seasonRepository = seasons,
            library = library,
            collection = FakeCollectionRepository(),
            observeProgress = progress,
            refreshEpisodes = RefreshShowEpisodesUseCase(shows, seasons),
            setStatus = setStatus,
            removeFromLibrary = RemoveTvShowFromLibraryUseCase(library),
            setFavorite = SetTvShowFavoriteUseCase(library, clock),
            setRating = SetTvShowRatingUseCase(library, clock),
            markEpisode = MarkEpisodeWatchedUseCase(library, SyncShowStatusUseCase(library, progress, setStatus), clock),
            clock = clock,
        ).also { vm -> backgroundScope.launch { vm.uiState.collect {} } }
    }

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
    fun `com cache, sem internet ao abrir nao avisa e ao tentar de novo avisa`() = test {
        shows.details.value = breakingBad()
        shows.onRefresh = { DataResult.Failure(DataError.Network) }
        val vm = started()
        advanceUntilIdle()
        assertNull(vm.success().refreshError)

        vm.refresh()
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
    fun `toque duplo no status não tira a série da biblioteca`() = test {
        shows.details.value = breakingBad()
        val vm = started()
        advanceUntilIdle()

        vm.onStatusClick(TvShowStatus.WATCHING)
        runCurrent()
        vm.onStatusClick(TvShowStatus.WATCHING)
        advanceUntilIdle()
        assertEquals(TvShowStatus.WATCHING, library.tvShows.value[SHOW_ID]?.status)
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
    fun `serie fora da biblioteca nao baixa temporadas`() = test {
        shows.details.value = breakingBad()
        started()
        advanceUntilIdle()

        assertEquals(emptyList<Int>(), seasons.refreshed)
    }

    @Test
    fun `ao entrar na biblioteca baixa temporadas regulares e mostra progresso`() = test {
        shows.details.value = breakingBad()
        val vm = started()
        advanceUntilIdle()

        vm.onStatusClick(TvShowStatus.WATCHING)
        seasons.episodes.value = listOf(ep(1, 1), ep(1, 2), ep(2, 1))
        advanceUntilIdle()

        assertEquals(listOf(1, 2), seasons.refreshed)
        val progress = vm.success().progress!!
        assertEquals(0, progress.watched)
        assertEquals(3, progress.available)
        assertTrue(progress.isComplete)
        assertEquals(listOf(1, 2, 0), vm.success().seasons.map { it.seasonNumber })
    }

    @Test
    fun `marcar proximo episodio atualiza progresso e temporada`() = test {
        shows.details.value = breakingBad()
        seasons.episodes.value = listOf(ep(1, 1), ep(1, 2), ep(2, 1))
        val vm = started()
        advanceUntilIdle()

        vm.onMarkNextEpisode()
        advanceUntilIdle()

        assertEquals(1, vm.success().progress?.watched)
        assertEquals(102L, vm.success().progress?.nextEpisode?.id)
        assertEquals(TvShowStatus.WATCHING, vm.success().personal.status)
        assertEquals(1, vm.success().seasons.first { it.seasonNumber == 1 }.watched)
    }

    @Test
    fun `toque duplo em marcar proximo marca um episodio so`() = test {
        shows.details.value = breakingBad()
        seasons.episodes.value = listOf(ep(1, 1), ep(1, 2), ep(2, 1))
        val vm = started()
        advanceUntilIdle()

        vm.onMarkNextEpisode()
        runCurrent()
        vm.onMarkNextEpisode()
        advanceUntilIdle()

        assertEquals(1, vm.success().progress?.watched)
    }

    @Test
    fun `sem internet ao baixar temporadas automaticamente nao avisa, pedido pelo usuario avisa`() = test {
        shows.details.value = breakingBad()
        seasons.failOn = 2
        val vm = started()
        advanceUntilIdle()

        vm.onStatusClick(TvShowStatus.WATCHING)
        advanceUntilIdle()
        assertNull(vm.success().refreshError)
        assertFalse(vm.success().isLoadingEpisodes)

        vm.loadEpisodes()
        advanceUntilIdle()
        assertEquals(DataError.Network, vm.success().refreshError)
    }

    @Test
    fun `status de producao do TMDB em portugues`() {
        assertEquals("Finalizada", tmdbStatusLabel("Ended"))
        assertEquals("Em exibição", tmdbStatusLabel("Returning Series"))
        assertEquals("Algo novo", tmdbStatusLabel("Algo novo"))
    }

    private companion object {
        const val SHOW_ID = 1396L

        fun ep(season: Int, number: Int) = Episode(
            id = season * 100L + number, showId = SHOW_ID, seasonNumber = season, episodeNumber = number,
            name = "S${season}E$number", overview = null, stillPath = null,
            airDate = LocalDate.of(2010, 1, number), runtimeMinutes = 47,
        )

        fun summary(number: Int, count: Int) = SeasonSummary(
            id = number.toLong(), seasonNumber = number, name = "Temporada $number", overview = null,
            posterPath = null, airDate = null, episodeCount = count,
        )

        fun breakingBad() = TvShowDetails(
            id = SHOW_ID, name = "Breaking Bad", originalName = "Breaking Bad", overview = null,
            posterPath = null, backdropPath = null, firstAirDate = null, tmdbStatus = "Ended",
            numberOfSeasons = 2, numberOfEpisodes = 3, episodeRunTime = null, voteAverage = 8.9,
            genres = emptyList(), creators = emptyList(),
            seasons = listOf(summary(0, 1), summary(1, 2), summary(2, 1)), cast = emptyList(),
        )
    }
}
