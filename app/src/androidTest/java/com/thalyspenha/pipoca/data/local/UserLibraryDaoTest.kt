package com.thalyspenha.pipoca.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thalyspenha.pipoca.data.local.dao.UserLibraryDao
import com.thalyspenha.pipoca.data.local.dao.MovieCacheBundle
import com.thalyspenha.pipoca.data.local.entity.TmdbMovieEntity
import com.thalyspenha.pipoca.data.local.entity.UserEpisodeEntity
import com.thalyspenha.pipoca.data.local.entity.UserMovieEntity
import com.thalyspenha.pipoca.data.local.entity.UserTvShowEntity
import com.thalyspenha.pipoca.data.local.entity.WatchHistoryEntity
import com.thalyspenha.pipoca.data.local.entity.WatchMediaType
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class UserLibraryDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: UserLibraryDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.userLibraryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun movie(id: Long, updatedAt: Long = 1L) =
        UserMovieEntity(movieId = id, status = MovieStatus.WANT_TO_WATCH, addedAt = 1L, updatedAt = updatedAt)

    @Test
    fun adicionaAtualizaERemoveFilme() = runBlocking {
        dao.upsertMovie(movie(603))
        dao.upsertMovie(movie(603).copy(status = MovieStatus.WATCHED, isFavorite = true, rating = 9, notes = "Clássico"))

        val saved = dao.getMovie(603)!!
        assertEquals(MovieStatus.WATCHED, saved.status)
        assertEquals(true, saved.isFavorite)
        assertEquals(9, saved.rating)
        assertEquals("Clássico", saved.notes)

        dao.deleteMovie(603)
        assertNull(dao.getMovie(603))
    }

    @Test
    fun listaFilmesPorAtualizacaoMaisRecente() = runBlocking {
        dao.upsertMovie(movie(1, updatedAt = 10))
        dao.upsertMovie(movie(2, updatedAt = 30))
        dao.upsertMovie(movie(3, updatedAt = 20))

        assertEquals(listOf(2L, 3L, 1L), dao.observeMovies().first().map { it.movieId })
    }

    @Test
    fun adicionaAtualizaERemoveSerie() = runBlocking {
        val show = UserTvShowEntity(showId = 1396, status = TvShowStatus.WANT_TO_WATCH, addedAt = 1L, updatedAt = 1L)
        dao.upsertTvShow(show)
        dao.upsertTvShow(show.copy(status = TvShowStatus.COMPLETED))

        assertEquals(TvShowStatus.COMPLETED, dao.observeTvShow(1396).first()!!.status)

        dao.deleteTvShow(1396)
        assertNull(dao.getTvShow(1396))
    }

    @Test
    fun historicoDoFilmeGuardaReassistidasERemoveTudo() = runBlocking {
        dao.insertWatch(WatchHistoryEntity(mediaType = WatchMediaType.MOVIE, movieId = 603, watchedAt = 100))
        dao.insertWatch(WatchHistoryEntity(mediaType = WatchMediaType.MOVIE, movieId = 603, watchedAt = 200))
        dao.insertWatch(WatchHistoryEntity(mediaType = WatchMediaType.MOVIE, movieId = 1, watchedAt = 150))

        assertEquals(listOf(200L, 100L), dao.observeMovieHistory(603).first().map { it.watchedAt })

        dao.deleteMovieHistory(603)
        assertEquals(emptyList<WatchHistoryEntity>(), dao.observeMovieHistory(603).first())
        assertEquals(1, dao.observeMovieHistory(1).first().size)
    }

    @Test
    fun gravaFilmeEVisualizacaoJuntosERemoveComHistorico() = runBlocking {
        val watched = movie(603).copy(status = MovieStatus.WATCHED)
        dao.upsertMovieWithWatch(watched, WatchHistoryEntity(mediaType = WatchMediaType.MOVIE, movieId = 603, watchedAt = 100))

        assertEquals(watched, dao.getMovie(603))
        assertEquals(1, dao.observeMovieHistory(603).first().size)

        dao.deleteMovieWithHistory(603)
        assertNull(dao.getMovie(603))
        assertEquals(emptyList<WatchHistoryEntity>(), dao.observeMovieHistory(603).first())
    }

    @Test
    fun filmesComCacheTrazemTituloEFilmesSemCacheVemNulos() = runBlocking {
        dao.upsertMovie(movie(603, updatedAt = 2))
        dao.upsertMovie(movie(1, updatedAt = 1))
        db.tmdbCacheDao().saveMovie(
            MovieCacheBundle(
                TmdbMovieEntity(
                    id = 603, title = "Matrix", originalTitle = "The Matrix", overview = null, posterPath = "/p.jpg",
                    backdropPath = null, releaseDate = LocalDate.of(1999, 3, 31), runtimeMinutes = null,
                    voteAverage = null, directors = emptyList(), fetchedAt = 1,
                ),
                genres = emptyList(), persons = emptyList(), credits = emptyList(),
            ),
        )

        val rows = dao.observeMoviesWithCache().first()
        assertEquals(listOf(603L, 1L), rows.map { it.movie.movieId })
        assertEquals("Matrix", rows[0].title)
        assertEquals(LocalDate.of(1999, 3, 31), rows[0].releaseDate)
        assertNull(rows[1].title)
    }

    private suspend fun historyCount(): Int =
        db.query("SELECT COUNT(*) FROM watch_history", null).use { it.moveToFirst(); it.getInt(0) }

    @Test
    fun marcaEDesmarcaEpisodiosComHistorico() = runBlocking {
        val episodes = listOf(UserEpisodeEntity(1, 1396, 1, 1, 100), UserEpisodeEntity(2, 1396, 1, 2, 100))
        dao.insertWatchedEpisodesWithHistory(
            episodes,
            episodes.map { WatchHistoryEntity(mediaType = WatchMediaType.EPISODE, showId = 1396, episodeId = it.episodeId, watchedAt = 100) },
        )
        assertEquals(2, historyCount())

        dao.deleteWatchedEpisodesWithHistory(listOf(1))

        assertEquals(listOf(2L), dao.observeWatchedEpisodes(1396).first().map { it.episodeId })
        assertEquals(1, historyCount())
    }

    @Test
    fun removerSerieApagaEpisodiosEHistoricoDela() = runBlocking {
        dao.upsertTvShow(UserTvShowEntity(showId = 1396, status = TvShowStatus.WATCHING, addedAt = 1, updatedAt = 1))
        dao.insertWatchedEpisodesWithHistory(
            listOf(UserEpisodeEntity(1, 1396, 1, 1, 100), UserEpisodeEntity(9, 7, 1, 1, 100)),
            listOf(
                WatchHistoryEntity(mediaType = WatchMediaType.EPISODE, showId = 1396, episodeId = 1, watchedAt = 100),
                WatchHistoryEntity(mediaType = WatchMediaType.EPISODE, showId = 7, episodeId = 9, watchedAt = 100),
            ),
        )

        dao.deleteTvShowWithEpisodes(1396)

        assertNull(dao.getTvShow(1396))
        assertEquals(emptyList<UserEpisodeEntity>(), dao.observeWatchedEpisodes(1396).first())
        assertEquals(1, dao.observeWatchedEpisodes(7).first().size)
        assertEquals(1, historyCount())
    }
}
