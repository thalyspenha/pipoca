package com.thalyspenha.pipoca.presentation.screens.home

import com.thalyspenha.pipoca.domain.model.LibraryMovie
import com.thalyspenha.pipoca.domain.model.LibraryMovieItem
import com.thalyspenha.pipoca.domain.model.LibraryTvShow
import com.thalyspenha.pipoca.domain.model.LibraryTvShowItem
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.presentation.navigation.MovieDetailsRoute
import com.thalyspenha.pipoca.presentation.navigation.TvShowDetailsRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import com.thalyspenha.pipoca.domain.usecase.episodes.WatchingShow
import com.thalyspenha.pipoca.domain.progress.ShowProgress
import com.thalyspenha.pipoca.domain.model.MediaFormat
import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.CollectionItem
import com.thalyspenha.pipoca.domain.model.CollectionEntry
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class HomeContentTest {

    private fun movie(id: Long, status: MovieStatus, updated: Long, favorite: Boolean = false) = LibraryMovieItem(
        LibraryMovie(id, status, isFavorite = favorite, addedAt = Instant.EPOCH, updatedAt = Instant.ofEpochSecond(updated)),
        title = "Filme $id", posterPath = null, year = 2000,
    )

    private fun show(id: Long, status: TvShowStatus, updated: Long, favorite: Boolean = false) = LibraryTvShowItem(
        LibraryTvShow(id, status, isFavorite = favorite, addedAt = Instant.EPOCH, updatedAt = Instant.ofEpochSecond(updated)),
        name = "Série $id", posterPath = null, year = 2010,
    )

    private fun List<HomeItem>.keys() = map { it.key }

    @Test
    fun `biblioteca vazia`() {
        val content = buildHomeContent(tmdbConfigured = true, movies = emptyList(), tvShows = emptyList())

        assertTrue(content.isLibraryEmpty)
    }

    @Test
    fun `separa secoes por status e favorito, mais recentes primeiro`() {
        val content = buildHomeContent(
            tmdbConfigured = true,
            movies = listOf(
                movie(1, MovieStatus.WANT_TO_WATCH, updated = 10),
                movie(2, MovieStatus.WATCHED, updated = 20, favorite = true),
            ),
            tvShows = listOf(
                show(10, TvShowStatus.WANT_TO_WATCH, updated = 30),
                show(11, TvShowStatus.WATCHING, updated = 5),
                show(12, TvShowStatus.COMPLETED, updated = 40, favorite = true),
            ),
        )

        assertEquals(listOf("TV_SHOW-10", "MOVIE-1"), content.wantToWatch.keys())
        assertEquals(listOf("TV_SHOW-12", "MOVIE-2"), content.favorites.keys())
        assertEquals(listOf("TV_SHOW-12", "MOVIE-2"), content.recentlyWatched.keys())
        assertEquals(HomeStats(moviesWatched = 1, showsCompleted = 1, total = 5), content.stats)
        assertEquals(emptyList<InProgressItem>(), content.inProgress)
    }

    @Test
    fun `secao limitada a HOME_SECTION_LIMIT`() {
        val movies = (1..HOME_SECTION_LIMIT + 5L).map { movie(it, MovieStatus.WANT_TO_WATCH, updated = it) }

        val content = buildHomeContent(tmdbConfigured = true, movies = movies, tvShows = emptyList())

        assertEquals(HOME_SECTION_LIMIT, content.wantToWatch.size)
        assertEquals(HOME_SECTION_LIMIT + 5, content.stats.total)
    }

    @Test
    fun `item leva para a rota de detalhes do tipo certo`() {
        val content = buildHomeContent(
            tmdbConfigured = true,
            movies = listOf(movie(603, MovieStatus.WANT_TO_WATCH, 1)),
            tvShows = listOf(show(1396, TvShowStatus.WANT_TO_WATCH, 2)),
        )

        assertEquals(listOf(TvShowDetailsRoute(1396), MovieDetailsRoute(603)), content.wantToWatch.map { it.detailsRoute() })
    }

    private fun ep(season: Int, number: Int) = Episode(
        id = season * 100L + number, showId = 11, seasonNumber = season, episodeNumber = number, name = "E$number",
        overview = null, stillPath = null, airDate = null, runtimeMinutes = null,
    )

    @Test
    fun `continuar assistindo so com proximo episodio, andamento com todas as series assistindo`() {
        fun watching(id: Long, next: Episode?, caughtUp: Boolean) = WatchingShow(
            item = show(id, TvShowStatus.WATCHING, updated = id),
            progress = ShowProgress(
                watched = if (caughtUp) 2 else 1, available = 2, nextEpisode = next, upcomingEpisode = null,
                isComplete = true, isShowFinished = false,
            ),
            lastWatchedAt = Instant.ofEpochSecond(id),
        )

        val content = buildHomeContent(
            tmdbConfigured = true,
            movies = emptyList(),
            tvShows = emptyList(),
            watching = listOf(watching(11, ep(1, 2), caughtUp = false), watching(12, null, caughtUp = true)),
        )

        assertEquals(listOf(11L), content.continueWatching.map { it.showId })
        assertEquals(102L, content.continueWatching[0].episode.id)
        assertEquals(listOf(50, 100), content.inProgress.map { it.percent })
        assertEquals(listOf(false, true), content.inProgress.map { it.isCaughtUp })
    }

    @Test
    fun `colecao recente, um por titulo, mais recente primeiro, e conta para nao estar vazia`() {
        fun entry(id: Long, tmdbId: Long, added: Long) = CollectionEntry(
            CollectionItem(
                id = id, tmdbId = tmdbId, mediaType = CollectionMediaType.MOVIE, format = MediaFormat.DVD,
                addedAt = Instant.ofEpochSecond(added), updatedAt = Instant.ofEpochSecond(added),
            ),
            title = "Filme $tmdbId", posterPath = null,
        )

        val content = buildHomeContent(
            tmdbConfigured = true, movies = emptyList(), tvShows = emptyList(),
            collection = listOf(entry(1, 603, 10), entry(2, 603, 30), entry(3, 550, 20)),
        )

        assertEquals(listOf("MOVIE-603", "MOVIE-550"), content.recentCollection.keys())
        assertFalse(content.isLibraryEmpty)
    }
}
