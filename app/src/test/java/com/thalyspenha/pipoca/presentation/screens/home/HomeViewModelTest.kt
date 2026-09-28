package com.thalyspenha.pipoca.presentation.screens.home

import com.thalyspenha.pipoca.data.repository.MutableClock
import com.thalyspenha.pipoca.domain.model.CollectionItemDraft
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.LibraryMovie
import com.thalyspenha.pipoca.domain.model.LibraryTvShow
import com.thalyspenha.pipoca.domain.model.MediaFormat
import com.thalyspenha.pipoca.domain.model.MovieDetails
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.model.SeasonSummary
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.repository.MovieRepository
import com.thalyspenha.pipoca.domain.usecase.collection.AddCollectionItemUseCase
import com.thalyspenha.pipoca.domain.usecase.collection.FakeCollectionRepository
import com.thalyspenha.pipoca.domain.usecase.episodes.FakeSeasonRepository
import com.thalyspenha.pipoca.domain.usecase.episodes.FakeTvShowRepository
import com.thalyspenha.pipoca.domain.usecase.cache.FetchMissingDetailsUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.MarkEpisodeWatchedUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.ObserveShowProgressUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.ObserveWatchingShowsUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.RefreshShowEpisodesUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.SyncShowStatusUseCase
import com.thalyspenha.pipoca.domain.usecase.library.FakeLibraryRepository
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowStatusUseCase
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = MutableClock()
    private val library = FakeLibraryRepository()
    private val collection = FakeCollectionRepository()
    private val movies = FakeMovieRepository()
    private val tvShows = FakeTvShowRepository()
    private val seasons = FakeSeasonRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.started(token: String = "token"): HomeViewModel {
        val progress = ObserveShowProgressUseCase(tvShows, seasons, library, clock)
        val sync = SyncShowStatusUseCase(library, progress, SetTvShowStatusUseCase(library, clock))
        return HomeViewModel(
            tmdbConfig = TmdbConfig(apiToken = token),
            library = library,
            collection = collection,
            observeWatchingShows = ObserveWatchingShowsUseCase(library, seasons, clock),
            fetchMissingDetails = FetchMissingDetailsUseCase(
                TmdbConfig(apiToken = token), movies, tvShows, RefreshShowEpisodesUseCase(tvShows, seasons),
            ),
            markEpisode = MarkEpisodeWatchedUseCase(library, sync, clock),
        ).also { vm -> backgroundScope.launch { vm.uiState.collect {} } }
    }

    private fun HomeViewModel.content() = (uiState.value as UiState.Success).data

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    private fun ep(season: Int, number: Int, showId: Long = SHOW) = Episode(
        id = showId * 1000 + season * 100L + number, showId = showId, seasonNumber = season, episodeNumber = number,
        name = "E$number", overview = null, stillPath = null, airDate = LocalDate.of(2010, 1, number), runtimeMinutes = 47,
    )

    private suspend fun watchingShow(showId: Long, episodes: List<Episode>) {
        library.saveTvShow(LibraryTvShow(showId, TvShowStatus.WATCHING, addedAt = Instant.EPOCH, updatedAt = Instant.EPOCH))
        library.tvShowNames.value += showId to "Série $showId"
        seasons.episodes.value += episodes
        seasons.seasons.value += showId to listOf(
            SeasonSummary(showId, 1, "T1", null, null, null, episodeCount = episodes.size),
        )
    }

    @Test
    fun `biblioteca vazia`() = test {
        val vm = started()
        assertEquals(UiState.Loading, vm.uiState.value)
        advanceUntilIdle()

        assertTrue(vm.content().isLibraryEmpty)
    }

    @Test
    fun `continuar assistindo e andamento a partir das series assistindo`() = test {
        watchingShow(SHOW, listOf(ep(1, 1), ep(1, 2)))
        val vm = started()
        advanceUntilIdle()

        assertEquals(listOf(ep(1, 1).id), vm.content().continueWatching.map { it.episode.id })
        assertEquals(listOf(0), vm.content().inProgress.map { it.watched })
        assertEquals(1, seasons.watchingEpisodesQueries)
    }

    @Test
    fun `assisti avanca o proximo episodio e o andamento sozinho`() = test {
        watchingShow(SHOW, listOf(ep(1, 1), ep(1, 2)))
        val vm = started()
        advanceUntilIdle()

        vm.onMarkWatched(ep(1, 1))
        advanceUntilIdle()

        assertEquals(listOf(ep(1, 2).id), vm.content().continueWatching.map { it.episode.id })
        assertEquals(listOf(1), vm.content().inProgress.map { it.watched })
    }

    @Test
    fun `serie vista mais recentemente vem primeiro`() = test {
        watchingShow(1, listOf(ep(1, 1, showId = 1), ep(1, 2, showId = 1)))
        watchingShow(2, listOf(ep(1, 1, showId = 2), ep(1, 2, showId = 2)))
        library.markEpisodesWatched(listOf(ep(1, 1, showId = 1)), clock.now)
        clock.advance(Duration.ofHours(1))
        library.markEpisodesWatched(listOf(ep(1, 1, showId = 2)), clock.now)
        val vm = started()
        advanceUntilIdle()

        assertEquals(listOf(2L, 1L), vm.content().continueWatching.map { it.showId })
    }

    @Test
    fun `colecao recente aparece e atualiza`() = test {
        library.saveMovie(LibraryMovie(603, MovieStatus.WANT_TO_WATCH, addedAt = Instant.EPOCH, updatedAt = Instant.EPOCH))
        val vm = started()
        advanceUntilIdle()

        AddCollectionItemUseCase(collection, clock)(CollectionItemDraft(603, CollectionMediaType.MOVIE, MediaFormat.UHD_4K_BLURAY))
        advanceUntilIdle()

        assertEquals(listOf("MOVIE-603"), vm.content().recentCollection.map { it.key })
    }

    @Test
    fun `busca uma vez o que falta no cache, so com token`() = test {
        library.saveMovie(LibraryMovie(603, MovieStatus.WATCHED, addedAt = Instant.EPOCH, updatedAt = Instant.EPOCH))
        AddCollectionItemUseCase(collection, clock)(CollectionItemDraft(550, CollectionMediaType.MOVIE, MediaFormat.DVD))
        // Série assistindo sem temporadas no cache: progresso incompleto.
        library.saveTvShow(LibraryTvShow(SHOW, TvShowStatus.WATCHING, addedAt = Instant.EPOCH, updatedAt = Instant.EPOCH))
        tvShows.details.value = null
        started()
        advanceUntilIdle()

        assertEquals(setOf(603L, 550L), movies.refreshed.toSet())

        movies.refreshed.clear()
        started(token = "")
        advanceUntilIdle()
        assertEquals(emptyList<Long>(), movies.refreshed)
    }

    private companion object {
        const val SHOW = 1396L
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
