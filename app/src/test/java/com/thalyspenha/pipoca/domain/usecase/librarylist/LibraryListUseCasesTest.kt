package com.thalyspenha.pipoca.domain.usecase.librarylist

import com.thalyspenha.pipoca.data.repository.MutableClock
import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.LibraryMovie
import com.thalyspenha.pipoca.domain.model.LibrarySort
import com.thalyspenha.pipoca.domain.model.LibraryTvShow
import com.thalyspenha.pipoca.domain.model.MovieLibraryFilter
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.model.SeasonSummary
import com.thalyspenha.pipoca.domain.model.TvShowLibraryFilter
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.model.matchesSearch
import com.thalyspenha.pipoca.domain.model.normalizedForSearch
import com.thalyspenha.pipoca.domain.usecase.episodes.FakeSeasonRepository
import com.thalyspenha.pipoca.domain.usecase.library.FakeLibraryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class LibraryListUseCasesTest {
    private val library = FakeLibraryRepository()
    private val seasons = FakeSeasonRepository()
    private val clock = MutableClock()
    private val movies = ObserveLibraryMoviesUseCase(library)
    private val shows = ObserveLibraryTvShowsUseCase(library, seasons, clock)

    private suspend fun movie(id: Long, title: String?, status: MovieStatus, favorite: Boolean = false) {
        library.saveMovie(LibraryMovie(id, status, isFavorite = favorite, addedAt = Instant.EPOCH, updatedAt = Instant.ofEpochSecond(id)))
        title?.let { library.movieTitles.value += id to it }
    }

    private suspend fun show(id: Long, name: String, status: TvShowStatus, favorite: Boolean = false) {
        library.saveTvShow(LibraryTvShow(id, status, isFavorite = favorite, addedAt = Instant.EPOCH, updatedAt = Instant.ofEpochSecond(id)))
        library.tvShowNames.value += id to name
    }

    private fun ep(showId: Long, number: Int) = Episode(
        id = showId * 100 + number, showId = showId, seasonNumber = 1, episodeNumber = number, name = "E$number",
        overview = null, stillPath = null, airDate = LocalDate.of(2010, 1, number), runtimeMinutes = 45,
    )

    @Test
    fun `pesquisa ignora acento e caixa`() {
        assertEquals("arvore da vida", "Árvore da Vida".normalizedForSearch())
        assertTrue(matchesSearch("Predator 2", "predator"))
        assertTrue(matchesSearch("Árvore", "ARVORE"))
        assertFalse(matchesSearch("Alien", "predator"))
        assertTrue(matchesSearch(null, "  "))
        assertFalse(matchesSearch(null, "x"))
    }

    @Test
    fun `filmes filtrados por status e favoritos e pesquisa local`() = runTest {
        movie(1, "Predator", MovieStatus.WATCHED, favorite = true)
        movie(2, "Predator 2", MovieStatus.WANT_TO_WATCH)
        movie(3, "Alien", MovieStatus.WATCHED)

        assertEquals(setOf(1L, 3L), movies(MovieLibraryFilter.WATCHED, LibrarySort.RECENTLY_ADDED, "").first().map { it.movie.movieId }.toSet())
        assertEquals(listOf(1L), movies(MovieLibraryFilter.FAVORITES, LibrarySort.RECENTLY_ADDED, "").first().map { it.movie.movieId })
        assertEquals(setOf(1L, 2L), movies(MovieLibraryFilter.ALL, LibrarySort.TITLE_ASC, "predator").first().map { it.movie.movieId }.toSet())
    }

    @Test
    fun `contagens por filtro`() = runTest {
        movie(1, "A", MovieStatus.WATCHED, favorite = true)
        movie(2, "B", MovieStatus.WANT_TO_WATCH)
        show(10, "S", TvShowStatus.PAUSED)
        show(11, "T", TvShowStatus.DROPPED, favorite = true)

        val movieCounts = ObserveMovieLibraryCountsUseCase(library)().first()
        assertEquals(2, movieCounts.of(MovieLibraryFilter.ALL))
        assertEquals(1, movieCounts.of(MovieLibraryFilter.WATCHED))
        assertEquals(1, movieCounts.of(MovieLibraryFilter.FAVORITES))

        val showCounts = ObserveTvShowLibraryCountsUseCase(library)().first()
        assertEquals(2, showCounts.of(TvShowLibraryFilter.ALL))
        assertEquals(1, showCounts.of(TvShowLibraryFilter.PAUSED))
        assertEquals(1, showCounts.of(TvShowLibraryFilter.DROPPED))
        assertEquals(0, showCounts.of(TvShowLibraryFilter.WATCHING))
        assertEquals(1, showCounts.of(TvShowLibraryFilter.FAVORITES))
    }

    @Test
    fun `series com progresso de qualquer status e sem progresso sem dados`() = runTest {
        show(10, "Breaking Bad", TvShowStatus.COMPLETED)
        show(11, "Nova", TvShowStatus.WANT_TO_WATCH)
        seasons.episodes.value = listOf(ep(10, 1), ep(10, 2), ep(10, 3), ep(10, 4))
        seasons.seasons.value = mapOf(10L to listOf(SeasonSummary(1, 1, "T1", null, null, null, 4)))
        library.markEpisodesWatched(listOf(ep(10, 1), ep(10, 2), ep(10, 3)), Instant.EPOCH)

        val entries = shows(TvShowLibraryFilter.ALL, LibrarySort.RECENTLY_ADDED, "").first().associateBy { it.item.show.showId }

        assertEquals(3, entries.getValue(10).progress?.watched)
        assertEquals(4, entries.getValue(10).progress?.available)
        assertEquals(75, entries.getValue(10).progress?.percent)
        assertNull(entries.getValue(11).progress)
    }

    @Test
    fun `ordenar por progresso maior primeiro e sem progresso no fim`() = runTest {
        show(10, "Metade", TvShowStatus.WATCHING)
        show(11, "Tudo", TvShowStatus.WATCHING)
        show(12, "Nada", TvShowStatus.WANT_TO_WATCH)
        seasons.episodes.value = listOf(ep(10, 1), ep(10, 2), ep(11, 1))
        library.markEpisodesWatched(listOf(ep(10, 1), ep(11, 1)), Instant.EPOCH)

        val order = shows(TvShowLibraryFilter.ALL, LibrarySort.PROGRESS, "").first().map { it.item.show.showId }

        assertEquals(listOf(11L, 10L, 12L), order)
    }

    @Test
    fun `filtro de series pausadas e pesquisa`() = runTest {
        show(10, "Lost", TvShowStatus.PAUSED)
        show(11, "Lost Girl", TvShowStatus.WATCHING)

        assertEquals(listOf(10L), shows(TvShowLibraryFilter.PAUSED, LibrarySort.RECENTLY_ADDED, "").first().map { it.item.show.showId })
        assertEquals(setOf(10L, 11L), shows(TvShowLibraryFilter.ALL, LibrarySort.RECENTLY_ADDED, "LOST").first().map { it.item.show.showId }.toSet())
    }

    @Test
    fun `atualiza sozinha quando o status muda`() = runTest {
        movie(1, "Predator", MovieStatus.WANT_TO_WATCH)
        val flow = movies(MovieLibraryFilter.WATCHED, LibrarySort.RECENTLY_ADDED, "")
        assertEquals(emptyList<Long>(), flow.first().map { it.movie.movieId })

        movie(1, "Predator", MovieStatus.WATCHED)

        assertEquals(listOf(1L), flow.first().map { it.movie.movieId })
    }
}
