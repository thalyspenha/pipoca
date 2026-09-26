package com.thalyspenha.pipoca.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thalyspenha.pipoca.data.local.dao.MovieCacheBundle
import com.thalyspenha.pipoca.data.local.dao.TmdbCacheDao
import com.thalyspenha.pipoca.data.local.dao.TvShowCacheBundle
import com.thalyspenha.pipoca.data.local.entity.CreditMediaType
import com.thalyspenha.pipoca.data.local.entity.TmdbCreditEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbGenreEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbMovieEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbPersonEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbSeasonEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbTvShowEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TmdbCacheDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: TmdbCacheDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.tmdbCacheDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun movie(fetchedAt: Long = 1L) = TmdbMovieEntity(
        id = 603, title = "Matrix", originalTitle = "The Matrix", overview = null,
        posterPath = "/p.jpg", backdropPath = null, releaseDate = LocalDate.of(1999, 3, 31),
        runtimeMinutes = 136, voteAverage = 8.2, directors = listOf("Lana Wachowski", "Lilly Wachowski"),
        fetchedAt = fetchedAt,
    )

    private fun show(fetchedAt: Long = 1L) = TmdbTvShowEntity(
        id = 1396, name = "Breaking Bad", originalName = "Breaking Bad", overview = null,
        posterPath = null, backdropPath = null, firstAirDate = LocalDate.of(2008, 1, 20), tmdbStatus = "Ended",
        numberOfSeasons = 5, numberOfEpisodes = 62, episodeRunTime = null, voteAverage = 9.0,
        creators = listOf("Vince Gilligan"), fetchedAt = fetchedAt,
    )

    private fun season(id: Long, number: Int) = TmdbSeasonEntity(
        id = id, showId = 1396, seasonNumber = number, name = "Temporada $number", overview = null,
        posterPath = null, airDate = null, episodeCount = 7, fetchedAt = 1L,
    )

    @Test
    fun salvaELeFilmeComGenerosEElenco() = runBlocking {
        dao.saveMovie(
            MovieCacheBundle(
                movie = movie(),
                genres = listOf(TmdbGenreEntity(878, "Ficção científica"), TmdbGenreEntity(28, "Ação")),
                persons = listOf(TmdbPersonEntity(6384, "Keanu Reeves", null), TmdbPersonEntity(2975, "Laurence Fishburne", null)),
                credits = listOf(
                    TmdbCreditEntity(CreditMediaType.MOVIE, 603, 2975, "Morpheus", 1),
                    TmdbCreditEntity(CreditMediaType.MOVIE, 603, 6384, "Neo", 0),
                ),
            ),
        )

        assertEquals(movie(), dao.observeMovie(603).first())
        assertEquals(listOf("Ação", "Ficção científica"), dao.getMovieGenres(603).map { it.name })
        assertEquals(listOf("Keanu Reeves", "Laurence Fishburne"), dao.getCast(CreditMediaType.MOVIE, 603).map { it.name })
    }

    @Test
    fun salvarDeNovoSubstituiGenerosEElenco() = runBlocking {
        val person = TmdbPersonEntity(6384, "Keanu Reeves", null)
        dao.saveMovie(
            MovieCacheBundle(
                movie(), listOf(TmdbGenreEntity(28, "Ação")), listOf(person),
                listOf(TmdbCreditEntity(CreditMediaType.MOVIE, 603, 6384, "Neo", 0)),
            ),
        )

        dao.saveMovie(MovieCacheBundle(movie(fetchedAt = 2L), listOf(TmdbGenreEntity(878, "Ficção científica")), emptyList(), emptyList()))

        assertEquals(2L, dao.getMovieFetchedAt(603))
        assertEquals(listOf(878L), dao.getMovieGenres(603).map { it.id })
        assertEquals(emptyList<Any>(), dao.getCast(CreditMediaType.MOVIE, 603))
    }

    @Test
    fun temporadasQueSumiramSaoRemovidas() = runBlocking {
        dao.saveTvShow(TvShowCacheBundle(show(), emptyList(), listOf(season(1, 0), season(2, 1)), emptyList(), emptyList()))

        dao.saveTvShow(TvShowCacheBundle(show(), emptyList(), listOf(season(2, 1), season(3, 2)), emptyList(), emptyList()))

        assertEquals(listOf(1, 2), dao.getSeasons(1396).map { it.seasonNumber })
        assertEquals("Ended", dao.getTvShowCacheInfo(1396)?.tmdbStatus)
    }

    @Test
    fun elencoDeFilmeESerieNaoSeMisturam() = runBlocking {
        val person = TmdbPersonEntity(1, "Pessoa", null)
        dao.saveMovie(MovieCacheBundle(movie(), emptyList(), listOf(person), listOf(TmdbCreditEntity(CreditMediaType.MOVIE, 603, 1, "A", 0))))
        dao.saveTvShow(TvShowCacheBundle(show(), emptyList(), emptyList(), listOf(person), listOf(TmdbCreditEntity(CreditMediaType.TV, 1396, 1, "B", 0))))

        assertEquals("A", dao.getCast(CreditMediaType.MOVIE, 603).single().character)
        assertEquals("B", dao.getCast(CreditMediaType.TV, 1396).single().character)
    }
}
