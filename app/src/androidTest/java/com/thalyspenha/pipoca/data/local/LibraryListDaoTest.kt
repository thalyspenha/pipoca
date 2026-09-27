package com.thalyspenha.pipoca.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thalyspenha.pipoca.data.local.dao.MovieCacheBundle
import com.thalyspenha.pipoca.data.local.dao.UserLibraryDao
import com.thalyspenha.pipoca.data.local.entity.TmdbMovieEntity
import com.thalyspenha.pipoca.data.local.entity.UserEpisodeEntity
import com.thalyspenha.pipoca.data.local.entity.UserMovieEntity
import com.thalyspenha.pipoca.data.local.entity.UserTvShowEntity
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class LibraryListDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: UserLibraryDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.userLibraryDao()
        runBlocking {
            // (id, título, lançamento, status, favorito, nota, adicionado, atualizado)
            movie(1, "predator", LocalDate.of(1987, 6, 12), MovieStatus.WATCHED, true, 9, added = 10, updated = 40)
            movie(2, "Alien", LocalDate.of(1979, 5, 25), MovieStatus.WANT_TO_WATCH, false, null, added = 30, updated = 20)
            movie(3, "Terminator", LocalDate.of(1984, 10, 26), MovieStatus.WATCHED, false, 7, added = 20, updated = 30)
            movie(4, null, null, MovieStatus.WANT_TO_WATCH, false, null, added = 40, updated = 10) // sem cache
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun movie(
        id: Long, title: String?, release: LocalDate?, status: MovieStatus, favorite: Boolean, rating: Int?,
        added: Long, updated: Long,
    ) {
        dao.upsertMovie(UserMovieEntity(id, status, isFavorite = favorite, rating = rating, addedAt = added, updatedAt = updated))
        if (title != null) {
            db.tmdbCacheDao().saveMovie(
                MovieCacheBundle(
                    TmdbMovieEntity(
                        id = id, title = title, originalTitle = title, overview = null, posterPath = null, backdropPath = null,
                        releaseDate = release, runtimeMinutes = null, voteAverage = null, directors = emptyList(), fetchedAt = 1,
                    ),
                    genres = emptyList(), persons = emptyList(), credits = emptyList(),
                ),
            )
        }
    }

    private suspend fun order(sort: String, status: String? = null, favoritesOnly: Boolean = false) =
        dao.observeMovieList(status, favoritesOnly, sort).first().map { it.movie.movieId }

    @Test
    fun ordenacoesNoBanco() = runBlocking {
        assertEquals(listOf(4L, 2L, 3L, 1L), order("RECENTLY_ADDED"))
        assertEquals(listOf(2L, 1L, 3L, 4L), order("TITLE_ASC")) // sem diferenciar caixa; sem título no fim
        assertEquals(listOf(3L, 1L, 2L, 4L), order("TITLE_DESC"))
        assertEquals(listOf(1L, 3L, 2L, 4L), order("RELEASE_YEAR"))
        assertEquals(listOf(1L, 3L, 2L, 4L), order("LAST_ACTIVITY"))
        assertEquals(listOf(1L, 3L, 2L, 4L), order("RATING")) // sem nota no fim, por atualização
    }

    @Test
    fun filtrosEContagens() = runBlocking {
        assertEquals(listOf(1L, 3L), order("LAST_ACTIVITY", status = "WATCHED"))
        assertEquals(listOf(1L), order("LAST_ACTIVITY", favoritesOnly = true))

        val counts = dao.observeMovieLibraryCounts().first()
        assertEquals(4, counts.all)
        assertEquals(2, counts.watched)
        assertEquals(2, counts.wantToWatch)
        assertEquals(1, counts.favorites)
    }

    @Test
    fun seriesComUltimoEpisodioEStatusPausadoEAbandonado() = runBlocking {
        dao.upsertTvShow(UserTvShowEntity(10, TvShowStatus.PAUSED, addedAt = 1, updatedAt = 1))
        dao.upsertTvShow(UserTvShowEntity(11, TvShowStatus.DROPPED, addedAt = 2, updatedAt = 2))
        dao.upsertTvShow(UserTvShowEntity(12, TvShowStatus.WATCHING, addedAt = 3, updatedAt = 3))
        dao.upsertWatchedEpisodes(
            listOf(UserEpisodeEntity(100, 10, 1, 1, watchedAt = 500), UserEpisodeEntity(110, 11, 1, 1, watchedAt = 900)),
        )

        val byLastEpisode = dao.observeTvShowList(null, false, "LAST_EPISODE").first()
        assertEquals(listOf(11L, 10L, 12L), byLastEpisode.map { it.show.showId })
        assertEquals(900L, byLastEpisode[0].lastWatchedAt)
        assertEquals(listOf(10L), dao.observeTvShowList("PAUSED", false, "RECENTLY_ADDED").first().map { it.show.showId })

        val counts = dao.observeTvShowLibraryCounts().first()
        assertEquals(1, counts.paused)
        assertEquals(1, counts.dropped)
        assertEquals(1, counts.watching)
        assertEquals(listOf(100L, 110L), dao.observeLibraryShowsWatchedEpisodes().first().map { it.episodeId }.sorted())
    }
}
