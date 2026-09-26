package com.thalyspenha.pipoca.data.mapper

import com.thalyspenha.pipoca.data.remote.TmdbJson
import com.thalyspenha.pipoca.data.remote.dto.CastDto
import com.thalyspenha.pipoca.data.remote.dto.CreditsDto
import com.thalyspenha.pipoca.data.remote.dto.MovieDetailsDto
import com.thalyspenha.pipoca.data.remote.dto.MovieSummaryDto
import com.thalyspenha.pipoca.data.remote.dto.PagedResponseDto
import com.thalyspenha.pipoca.data.remote.dto.TvShowDetailsDto
import com.thalyspenha.pipoca.data.remote.fixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TmdbMappersTest {

    @Test
    fun `busca de filmes vira SearchPage com datas convertidas`() {
        val dto = TmdbJson.decodeFromString<PagedResponseDto<MovieSummaryDto>>(fixture("search_movie.json"))

        val page = dto.toSearchPage { it.toDomain() }

        assertEquals(92, page.totalResults)
        assertEquals(true, page.hasNextPage)
        val matrix = page.items.first()
        assertEquals(603L, matrix.id)
        assertEquals(LocalDate.of(1999, 3, 31), matrix.releaseDate)
    }

    @Test
    fun `detalhes do filme trazem diretores, generos e elenco ordenado`() {
        val movie = TmdbJson.decodeFromString<MovieDetailsDto>(fixture("movie_details.json")).toDomain()

        assertEquals("Matrix", movie.title)
        assertEquals(136, movie.runtimeMinutes)
        assertEquals(listOf("Lana Wachowski", "Lilly Wachowski"), movie.directors)
        assertEquals("Keanu Reeves", movie.cast.first().name)
        assertEquals(movie.cast.sortedBy { it.order }, movie.cast)
        assertFalse(movie.genres.isEmpty())
    }

    @Test
    fun `detalhes da serie trazem status, temporadas e criadores`() {
        val show = TmdbJson.decodeFromString<TvShowDetailsDto>(fixture("tv_details.json")).toDomain()

        assertEquals("Ended", show.tmdbStatus)
        assertEquals(LocalDate.of(2008, 1, 20), show.firstAirDate)
        assertEquals(listOf(0, 1), show.seasons.map { it.seasonNumber })
        assertFalse(show.creators.isEmpty())
        // episode_run_time vem vazio para Breaking Bad
        assertNull(show.episodeRunTime)
    }

    @Test
    fun `episode_run_time vira media`() {
        val show = TvShowDetailsDto(id = 1, episodeRunTime = listOf(40, 50, 0)).toDomain()

        assertEquals(45, show.episodeRunTime)
    }

    @Test
    fun `strings vazias e datas invalidas viram null`() {
        val movie = MovieSummaryDto(
            id = 1,
            overview = "",
            posterPath = " ",
            releaseDate = "",
        ).toDomain()

        assertNull(movie.overview)
        assertNull(movie.posterPath)
        assertNull(movie.releaseDate)
        assertNull("2024-13-45".toLocalDateOrNull())
    }

    @Test
    fun `runtime zero vira null`() {
        assertNull(MovieDetailsDto(id = 1, runtime = 0).toDomain().runtimeMinutes)
    }

    @Test
    fun `elenco limitado ao principal`() {
        val cast = (0 until 40).map { CastDto(id = it.toLong(), name = "Ator $it", order = 39 - it) }

        val movie = MovieDetailsDto(id = 1, credits = CreditsDto(cast = cast)).toDomain()

        assertEquals(MAX_CAST, movie.cast.size)
        assertEquals(0, movie.cast.first().order)
    }
}
