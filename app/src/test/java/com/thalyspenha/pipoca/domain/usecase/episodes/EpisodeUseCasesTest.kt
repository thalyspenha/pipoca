package com.thalyspenha.pipoca.domain.usecase.episodes

import com.thalyspenha.pipoca.data.repository.MutableClock
import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.SeasonSummary
import com.thalyspenha.pipoca.domain.model.TvShowDetails
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.usecase.library.FakeLibraryRepository
import com.thalyspenha.pipoca.domain.usecase.library.RemoveTvShowFromLibraryUseCase
import com.thalyspenha.pipoca.domain.usecase.library.SetTvShowStatusUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

class EpisodeUseCasesTest {
    private val clock = MutableClock() // 2026-09-26
    private val library = FakeLibraryRepository()
    private val shows = FakeTvShowRepository()
    private val seasons = FakeSeasonRepository()
    private val progress = ObserveShowProgressUseCase(shows, seasons, library, clock)
    private val sync = SyncShowStatusUseCase(library, progress, SetTvShowStatusUseCase(library, clock))
    private val mark = MarkEpisodeWatchedUseCase(library, sync, clock)
    private val unmark = UnmarkEpisodeUseCase(library, sync)
    private val markSeason = MarkSeasonWatchedUseCase(library, seasons, sync, clock)
    private val unmarkSeason = UnmarkSeasonUseCase(library, sync)

    private fun ep(season: Int, number: Int, airDate: LocalDate? = LocalDate.of(2020, season.coerceAtLeast(1), number)) =
        Episode(
            id = season * 100L + number, showId = SHOW, seasonNumber = season, episodeNumber = number,
            name = "S${season}E$number", overview = null, stillPath = null, airDate = airDate, runtimeMinutes = 45,
        )

    /** Série com especial (S0E1), temporada 1 (2 episódios) e temporada 2 (1 exibido + 1 futuro). */
    private fun setUpShow(status: String) {
        val all = listOf(ep(0, 1), ep(1, 1), ep(1, 2), ep(2, 1), ep(2, 2, airDate = LocalDate.of(2030, 1, 1)))
        seasons.episodes.value = all
        shows.details.value = show(status, seasons = listOf(summary(0, 1), summary(1, 2), summary(2, 2)))
    }

    private fun status() = library.tvShows.value[SHOW]?.status
    private fun watchedIds() = library.watchedEpisodes.value.keys

    @Test
    fun `marcar episodio de serie fora da biblioteca adiciona como assistindo`() = runTest {
        setUpShow("Returning Series")

        mark(ep(1, 1))

        assertEquals(TvShowStatus.WATCHING, status())
        assertEquals(setOf(101L), watchedIds())
        assertEquals(clock.now, library.watchedEpisodes.value.getValue(101).watchedAt)
        assertEquals(1, library.episodeWatches[101])
    }

    @Test
    fun `quero ver vira assistindo ao marcar`() = runTest {
        setUpShow("Returning Series")
        SetTvShowStatusUseCase(library, clock)(SHOW, TvShowStatus.WANT_TO_WATCH)

        mark(ep(1, 1))

        assertEquals(TvShowStatus.WATCHING, status())
    }

    @Test
    fun `episodio futuro ou sem data nao e marcado`() = runTest {
        setUpShow("Returning Series")

        mark(ep(2, 2, airDate = LocalDate.of(2030, 1, 1)))
        mark(ep(2, 3, airDate = null))

        assertEquals(emptySet<Long>(), watchedIds())
        assertNull(status())
    }

    @Test
    fun `marcar de novo nao duplica historico nem muda a data`() = runTest {
        setUpShow("Returning Series")
        mark(ep(1, 1))
        val first = clock.now
        clock.advance(Duration.ofDays(1))

        mark(ep(1, 1))

        assertEquals(1, library.episodeWatches[101])
        assertEquals(first, library.watchedEpisodes.value.getValue(101).watchedAt)
    }

    @Test
    fun `ultimo episodio exibido de serie finalizada conclui`() = runTest {
        setUpShow("Ended")
        markSeason(SHOW, 1)
        assertEquals(TvShowStatus.WATCHING, status())

        mark(ep(2, 1))

        assertEquals(TvShowStatus.COMPLETED, status())
    }

    @Test
    fun `serie no ar com tudo exibido assistido fica assistindo`() = runTest {
        setUpShow("Returning Series")

        markSeason(SHOW, 1)
        markSeason(SHOW, 2)

        assertEquals(TvShowStatus.WATCHING, status())
        assertEquals(true, progress.current(SHOW)?.isCaughtUp)
    }

    @Test
    fun `especiais nao concluem nem contam`() = runTest {
        setUpShow("Ended")

        markSeason(SHOW, 0)

        assertEquals(TvShowStatus.WATCHING, status())
        assertEquals(0, progress.current(SHOW)?.watched)
    }

    @Test
    fun `desmarcar episodio de serie concluida volta para assistindo`() = runTest {
        setUpShow("Ended")
        markSeason(SHOW, 1)
        markSeason(SHOW, 2)
        assertEquals(TvShowStatus.COMPLETED, status())

        unmark(ep(1, 2))

        assertEquals(TvShowStatus.WATCHING, status())
        assertEquals(setOf(101L, 201L), watchedIds())
        assertNull(library.episodeWatches[102])
    }

    @Test
    fun `marcar temporada so marca exibidos e preserva data dos ja marcados`() = runTest {
        setUpShow("Returning Series")
        mark(ep(2, 1))
        val first = clock.now
        clock.advance(Duration.ofDays(3))

        markSeason(SHOW, 2)

        assertEquals(setOf(201L), watchedIds())
        assertEquals(first, library.watchedEpisodes.value.getValue(201).watchedAt)
    }

    @Test
    fun `desmarcar temporada inteira`() = runTest {
        setUpShow("Returning Series")
        markSeason(SHOW, 1)
        mark(ep(2, 1))

        unmarkSeason(SHOW, 1)

        assertEquals(setOf(201L), watchedIds())
        assertEquals(1, progress.current(SHOW)?.watched)
    }

    @Test
    fun `desmarcar temporada de serie concluida volta para assistindo`() = runTest {
        setUpShow("Ended")
        markSeason(SHOW, 1)
        markSeason(SHOW, 2)

        unmarkSeason(SHOW, 2)

        assertEquals(TvShowStatus.WATCHING, status())
    }

    @Test
    fun `status pausada escolhido pelo usuario nao muda ao marcar`() = runTest {
        setUpShow("Returning Series")
        SetTvShowStatusUseCase(library, clock)(SHOW, TvShowStatus.PAUSED)

        mark(ep(1, 1))

        assertEquals(TvShowStatus.PAUSED, status())
    }

    @Test
    fun `progresso mostra 2 de 3 e percentual`() = runTest {
        setUpShow("Returning Series")
        markSeason(SHOW, 1)

        val p = progress.current(SHOW)!!
        assertEquals(2, p.watched)
        assertEquals(3, p.available)
        assertEquals(66, p.percent)
        assertEquals(201L, p.nextEpisode?.id)
    }

    @Test
    fun `progresso nulo sem a serie em cache`() = runTest {
        assertNull(progress.current(SHOW))
    }

    @Test
    fun `remover serie da biblioteca apaga episodios assistidos`() = runTest {
        setUpShow("Returning Series")
        markSeason(SHOW, 1)

        RemoveTvShowFromLibraryUseCase(library)(SHOW)

        assertEquals(emptySet<Long>(), watchedIds())
        assertNull(status())
    }

    @Test
    fun `refresh de episodios baixa so temporadas regulares com episodios e para na falha`() = runTest {
        shows.details.value = show("Ended", seasons = listOf(summary(0, 1), summary(1, 2), summary(2, 2), summary(3, 0)))
        val refresh = RefreshShowEpisodesUseCase(shows, seasons)

        assertEquals(DataResult.Success(Unit), refresh(SHOW))
        assertEquals(listOf(1, 2), seasons.refreshed)

        seasons.failOn = 2
        assertEquals(DataResult.Failure(DataError.Network), refresh(SHOW))
    }

    private companion object {
        const val SHOW = 1396L

        fun summary(number: Int, count: Int) = SeasonSummary(
            id = number.toLong(), seasonNumber = number, name = "T$number", overview = null,
            posterPath = null, airDate = null, episodeCount = count,
        )

        fun show(status: String, seasons: List<SeasonSummary>) = TvShowDetails(
            id = SHOW, name = "Série", originalName = "Série", overview = null, posterPath = null,
            backdropPath = null, firstAirDate = null, tmdbStatus = status, numberOfSeasons = null,
            numberOfEpisodes = null, episodeRunTime = null, voteAverage = null, genres = emptyList(),
            creators = emptyList(), seasons = seasons, cast = emptyList(),
        )
    }
}
