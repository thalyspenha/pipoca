package com.thalyspenha.pipoca.data.remote

import com.thalyspenha.pipoca.data.remote.dto.MovieDetailsDto
import com.thalyspenha.pipoca.data.remote.dto.MovieSummaryDto
import com.thalyspenha.pipoca.data.remote.dto.PagedResponseDto
import com.thalyspenha.pipoca.data.remote.dto.TmdbErrorDto
import com.thalyspenha.pipoca.data.remote.dto.TvShowDetailsDto
import com.thalyspenha.pipoca.data.remote.dto.TvShowSummaryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbParsingTest {

    @Test
    fun `search movie`() {
        val page = TmdbJson.decodeFromString<PagedResponseDto<MovieSummaryDto>>(fixture("search_movie.json"))

        assertEquals(1, page.page)
        assertEquals(92, page.totalResults)
        assertEquals(3, page.results.size)
        val matrix = page.results.first()
        assertEquals(603L, matrix.id)
        assertEquals("Matrix", matrix.title)
        assertEquals("The Matrix", matrix.originalTitle)
        assertEquals("1999-03-31", matrix.releaseDate)
        assertEquals("/lDqMDI3xpbB9UQRyeXfei0MXhqb.jpg", matrix.posterPath)
        assertEquals(listOf(28L, 878L), matrix.genreIds)
    }

    @Test
    fun `search tv`() {
        val page = TmdbJson.decodeFromString<PagedResponseDto<TvShowSummaryDto>>(fixture("search_tv.json"))

        val show = page.results.first()
        assertEquals(1396L, show.id)
        assertEquals("Breaking Bad", show.name)
        assertEquals("2008-01-20", show.firstAirDate)
    }

    @Test
    fun `movie details com creditos`() {
        val movie = TmdbJson.decodeFromString<MovieDetailsDto>(fixture("movie_details.json"))

        assertEquals(603L, movie.id)
        assertEquals(136, movie.runtime)
        assertTrue(movie.genres.any { it.id == 28L })
        val credits = requireNotNull(movie.credits)
        assertEquals("Keanu Reeves", credits.cast.first().name)
        assertEquals(0, credits.cast.first().order)
        assertEquals(
            listOf("Lana Wachowski", "Lilly Wachowski"),
            credits.crew.filter { it.job == "Director" }.map { it.name },
        )
    }

    @Test
    fun `tv details com temporadas e creditos`() {
        val show = TmdbJson.decodeFromString<TvShowDetailsDto>(fixture("tv_details.json"))

        assertEquals(1396L, show.id)
        assertEquals("Ended", show.status)
        assertEquals(5, show.numberOfSeasons)
        assertEquals(62, show.numberOfEpisodes)
        assertTrue(show.episodeRunTime.isEmpty())
        assertEquals(listOf(0, 1), show.seasons.map { it.seasonNumber })
        assertTrue(show.createdBy.isNotEmpty())
        assertTrue(requireNotNull(show.credits).cast.isNotEmpty())
    }

    @Test
    fun `corpo de erro`() {
        val error = TmdbJson.decodeFromString<TmdbErrorDto>(fixture("error_not_found.json"))

        assertEquals(34, error.statusCode)
    }

    @Test
    fun `null em campo nao nulo e campos desconhecidos nao quebram`() {
        val json = """{"id": 1, "title": null, "genre_ids": null, "campo_novo": {"x": 1}}"""

        val movie = TmdbJson.decodeFromString<MovieSummaryDto>(json)

        assertEquals("", movie.title)
        assertTrue(movie.genreIds.isEmpty())
        assertNull(movie.posterPath)
    }
}
