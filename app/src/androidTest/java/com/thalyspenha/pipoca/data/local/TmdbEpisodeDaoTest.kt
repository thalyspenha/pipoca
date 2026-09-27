package com.thalyspenha.pipoca.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thalyspenha.pipoca.data.local.dao.TmdbEpisodeDao
import com.thalyspenha.pipoca.data.local.dao.TvShowCacheBundle
import com.thalyspenha.pipoca.data.local.entity.TmdbEpisodeEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbSeasonEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbTvShowEntity
import com.thalyspenha.pipoca.data.local.entity.UserEpisodeEntity
import com.thalyspenha.pipoca.data.local.entity.UserTvShowEntity
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
class TmdbEpisodeDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: TmdbEpisodeDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.tmdbEpisodeDao()
        runBlocking {
            db.tmdbCacheDao().saveTvShow(
                TvShowCacheBundle(
                    show = TmdbTvShowEntity(
                        id = 1396, name = "Breaking Bad", originalName = "Breaking Bad", overview = null,
                        posterPath = null, backdropPath = null, firstAirDate = null, tmdbStatus = "Ended",
                        numberOfSeasons = 5, numberOfEpisodes = 62, episodeRunTime = null, voteAverage = null,
                        creators = emptyList(), fetchedAt = 1,
                    ),
                    genres = emptyList(), seasons = listOf(season()), persons = emptyList(), credits = emptyList(),
                ),
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun season() = TmdbSeasonEntity(
        id = 3572, showId = 1396, seasonNumber = 1, name = "Temporada 1", overview = null,
        posterPath = null, airDate = null, episodeCount = 7, fetchedAt = 1,
    )

    private fun episode(id: Long, number: Int, fetchedAt: Long = 10) = TmdbEpisodeEntity(
        id = id, showId = 1396, seasonId = 3572, seasonNumber = 1, episodeNumber = number, name = "E$number",
        overview = null, stillPath = null, airDate = LocalDate.of(2008, 1, number), runtimeMinutes = 47,
        fetchedAt = fetchedAt,
    )

    @Test
    fun salvaTemporadaSubstituindoEpisodiosQueSumiram() = runBlocking {
        dao.saveSeason(season(), listOf(episode(1, 1), episode(2, 2), episode(3, 3)))
        dao.saveSeason(season(), listOf(episode(2, 2, fetchedAt = 20), episode(4, 3, fetchedAt = 20)))

        assertEquals(listOf(2L, 4L), dao.observeSeasonEpisodes(1396, 1).first().map { it.id })
        assertEquals(20L, dao.getSeasonEpisodesFetchedAt(1396, 1))
        assertNull(dao.getSeasonEpisodesFetchedAt(1396, 2))
    }

    @Test
    fun cacheDaSerieSemATemporadaApagaEpisodiosMasNaoOsAssistidos() = runBlocking {
        dao.saveSeason(season(), listOf(episode(1, 1)))
        db.userLibraryDao().upsertWatchedEpisodes(listOf(UserEpisodeEntity(1, 1396, 1, 1, watchedAt = 100)))

        // Série regravada sem a temporada 1: CASCADE apaga os episódios do cache.
        val show = db.tmdbCacheDao()
        show.saveTvShow(
            TvShowCacheBundle(
                show = TmdbTvShowEntity(
                    id = 1396, name = "Breaking Bad", originalName = "Breaking Bad", overview = null,
                    posterPath = null, backdropPath = null, firstAirDate = null, tmdbStatus = "Ended",
                    numberOfSeasons = 5, numberOfEpisodes = 62, episodeRunTime = null, voteAverage = null,
                    creators = emptyList(), fetchedAt = 2,
                ),
                genres = emptyList(), seasons = emptyList(), persons = emptyList(), credits = emptyList(),
            ),
        )

        assertEquals(emptyList<TmdbEpisodeEntity>(), dao.observeShowEpisodes(1396).first())
        assertEquals(listOf(1L), db.userLibraryDao().observeWatchedEpisodes(1396).first().map { it.episodeId })
    }

    @Test
    fun marcaEDesmarcaEpisodiosAssistidos() = runBlocking {
        val library = db.userLibraryDao()
        library.upsertWatchedEpisodes(
            listOf(UserEpisodeEntity(1, 1396, 1, 1, 100), UserEpisodeEntity(2, 1396, 1, 2, 200)),
        )

        library.deleteWatchedEpisodes(listOf(1))

        assertEquals(listOf(2L), library.observeWatchedEpisodes(1396).first().map { it.episodeId })
    }

    @Test
    fun consultasAgregadasSoTrazemSeriesAssistindo() = runBlocking {
        dao.saveSeason(season(), listOf(episode(1, 1), episode(2, 2)))
        val library = db.userLibraryDao()
        library.upsertWatchedEpisodes(listOf(UserEpisodeEntity(1, 1396, 1, 1, watchedAt = 100)))

        library.upsertTvShow(UserTvShowEntity(1396, TvShowStatus.WANT_TO_WATCH, addedAt = 1, updatedAt = 1))
        assertEquals(emptyList<TmdbEpisodeEntity>(), dao.observeWatchingShowsEpisodes().first())
        assertEquals(emptyList<UserEpisodeEntity>(), library.observeWatchingShowsWatchedEpisodes().first())

        library.upsertTvShow(UserTvShowEntity(1396, TvShowStatus.WATCHING, addedAt = 1, updatedAt = 2))
        assertEquals(listOf(1L, 2L), dao.observeWatchingShowsEpisodes().first().map { it.id })
        assertEquals(listOf(3572L), dao.observeWatchingShowsSeasons().first().map { it.id })
        assertEquals(listOf(1L), library.observeWatchingShowsWatchedEpisodes().first().map { it.episodeId })
    }
}
