package com.thalyspenha.pipoca.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thalyspenha.pipoca.data.local.dao.MaintenanceDao
import com.thalyspenha.pipoca.data.local.dao.MovieCacheBundle
import com.thalyspenha.pipoca.data.local.dao.TvShowCacheBundle
import com.thalyspenha.pipoca.data.local.entity.CollectionItemEntity
import com.thalyspenha.pipoca.data.local.entity.CreditMediaType
import com.thalyspenha.pipoca.data.local.entity.TmdbCreditEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbEpisodeEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbMovieEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbPersonEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbSeasonEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbTvShowEntity
import com.thalyspenha.pipoca.data.local.entity.UserEpisodeEntity
import com.thalyspenha.pipoca.data.local.entity.UserMovieEntity
import com.thalyspenha.pipoca.data.local.entity.UserTvShowEntity
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.MediaFormat
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaintenanceDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: MaintenanceDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.maintenanceDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun cacheMovie(id: Long, personId: Long) = db.tmdbCacheDao().saveMovie(
        MovieCacheBundle(
            TmdbMovieEntity(
                id = id, title = "Filme $id", originalTitle = "", overview = null, posterPath = null, backdropPath = null,
                releaseDate = null, runtimeMinutes = 100, voteAverage = null, directors = emptyList(), fetchedAt = 1,
            ),
            genres = emptyList(),
            persons = listOf(TmdbPersonEntity(personId, "Pessoa $personId", null)),
            credits = listOf(TmdbCreditEntity(CreditMediaType.MOVIE, id, personId, null, 0)),
        ),
    )

    private suspend fun cacheShow(id: Long) {
        val season = TmdbSeasonEntity(id * 10, id, 1, "T1", null, null, null, 1, 1)
        db.tmdbCacheDao().saveTvShow(
            TvShowCacheBundle(
                show = TmdbTvShowEntity(
                    id = id, name = "Série $id", originalName = "", overview = null, posterPath = null, backdropPath = null,
                    firstAirDate = null, tmdbStatus = null, numberOfSeasons = 1, numberOfEpisodes = 1, episodeRunTime = 45,
                    voteAverage = null, creators = emptyList(), fetchedAt = 1,
                ),
                genres = emptyList(), seasons = listOf(season), persons = emptyList(), credits = emptyList(),
            ),
        )
        db.tmdbEpisodeDao().saveSeason(
            season,
            listOf(TmdbEpisodeEntity(id * 100, id, season.id, 1, 1, "E1", null, null, null, runtimeMinutes = 45, fetchedAt = 1)),
        )
    }

    @Test
    fun limpaSoTitulosForaDaBibliotecaColecaoEHistorico() = runBlocking {
        cacheMovie(1, personId = 900) // biblioteca
        cacheMovie(2, personId = 901) // coleção
        cacheMovie(3, personId = 900) // solto; pessoa 900 continua (filme 1)
        cacheMovie(4, personId = 902) // solto; pessoa 902 sai
        cacheShow(10) // biblioteca
        cacheShow(11) // só episódio marcado
        cacheShow(12) // solto
        val library = db.userLibraryDao()
        library.upsertMovie(UserMovieEntity(1, MovieStatus.WATCHED, addedAt = 1, updatedAt = 1))
        library.upsertTvShow(UserTvShowEntity(10, TvShowStatus.WATCHING, addedAt = 1, updatedAt = 1))
        library.upsertWatchedEpisodes(listOf(UserEpisodeEntity(1100, 11, 1, 1, watchedAt = 1)))
        db.collectionDao().insert(
            CollectionItemEntity(
                mediaType = CollectionMediaType.MOVIE, tmdbId = 2, format = MediaFormat.BLURAY, edition = null,
                region = null, quantity = 1, notes = null, acquiredAt = null, addedAt = 1, updatedAt = 1,
            ),
        )

        assertEquals(2 to 1, dao.clearUnusedCache())

        val counts = dao.observeCounts().first()
        assertEquals(2, counts.cachedMovies)
        assertEquals(2, counts.cachedTvShows)
        assertEquals(2, counts.cachedEpisodes) // episódio da série 12 saiu em CASCADE
        assertEquals(1, counts.movies)
        assertEquals(1, counts.tvShows)
        assertEquals(1, counts.watchedEpisodes)
        assertEquals(1, counts.collectionItems)
        assertEquals(1, db.tmdbCacheDao().getCast(CreditMediaType.MOVIE, 1).size)
        assertEquals(emptyList<Any>(), db.tmdbCacheDao().getCast(CreditMediaType.MOVIE, 4))
        assertEquals(emptyList<Any>(), db.tmdbCacheDao().getSeasons(12))
    }

    @Test
    fun bancoVazioDaZeros() = runBlocking {
        assertEquals(0 to 0, dao.clearUnusedCache())
        assertEquals(0, dao.observeCounts().first().cachedMovies)
    }
}
