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

        assertEquals(listOf("TV_SHOW-11"), content.watching.keys())
        assertEquals(listOf("TV_SHOW-10", "MOVIE-1"), content.wantToWatch.keys())
        assertEquals(listOf("TV_SHOW-12", "MOVIE-2"), content.favorites.keys())
        assertEquals(listOf("TV_SHOW-12", "MOVIE-2"), content.recentlyWatched.keys())
        assertEquals(HomeStats(moviesWatched = 1, showsCompleted = 1, total = 5), content.stats)
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
}
