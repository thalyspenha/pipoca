package com.thalyspenha.pipoca.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thalyspenha.pipoca.data.local.dao.MovieCacheBundle
import com.thalyspenha.pipoca.data.local.dao.StatsDao
import com.thalyspenha.pipoca.data.local.dao.TvShowCacheBundle
import com.thalyspenha.pipoca.data.local.entity.CollectionItemEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbEpisodeEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbGenreEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbMovieEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbSeasonEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbTvShowEntity
import com.thalyspenha.pipoca.data.local.entity.UserEpisodeEntity
import com.thalyspenha.pipoca.data.local.entity.UserMovieEntity
import com.thalyspenha.pipoca.data.local.entity.UserTvShowEntity
import com.thalyspenha.pipoca.data.local.entity.WatchHistoryEntity
import com.thalyspenha.pipoca.data.local.entity.WatchMediaType
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
class StatsDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: StatsDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.statsDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun movieCache(id: Long, runtime: Int?, genres: List<TmdbGenreEntity>) = MovieCacheBundle(
        TmdbMovieEntity(
            id = id, title = "Filme $id", originalTitle = "", overview = null, posterPath = null, backdropPath = null,
            releaseDate = null, runtimeMinutes = runtime, voteAverage = null, directors = emptyList(), fetchedAt = 1,
        ),
        genres = genres, persons = emptyList(), credits = emptyList(),
    )

    private val drama = TmdbGenreEntity(18, "Drama")
    private val action = TmdbGenreEntity(28, "Ação")

    @Test
    fun bancoVazioDaZeros() = runBlocking {
        assertEquals(0, dao.observeMovieCounts().first().watched)
        assertEquals(0, dao.observeTvShowCounts().first().total)
        assertEquals(0, dao.observeEpisodeCounts(0, 0).first().total)
        assertEquals(0L, dao.observeWatchTime().first().minutes)
        assertEquals(emptyList<Any>(), dao.observeGenreCounts().first())
    }

    @Test
    fun contagensDeFilmesSeriesEpisodiosEColecao() = runBlocking {
        val library = db.userLibraryDao()
        library.upsertMovie(UserMovieEntity(1, MovieStatus.WATCHED, isFavorite = true, rating = 8, addedAt = 1, updatedAt = 1))
        library.upsertMovie(UserMovieEntity(2, MovieStatus.WANT_TO_WATCH, rating = 8, addedAt = 1, updatedAt = 1))
        library.upsertTvShow(UserTvShowEntity(10, TvShowStatus.WATCHING, rating = 10, addedAt = 1, updatedAt = 1))
        library.upsertTvShow(UserTvShowEntity(11, TvShowStatus.COMPLETED, addedAt = 1, updatedAt = 1))
        library.upsertTvShow(UserTvShowEntity(12, TvShowStatus.WANT_TO_WATCH, addedAt = 1, updatedAt = 1))
        library.upsertWatchedEpisodes(
            listOf(UserEpisodeEntity(100, 10, 1, 1, watchedAt = 50), UserEpisodeEntity(101, 10, 1, 2, watchedAt = 150),
                UserEpisodeEntity(102, 10, 1, 3, watchedAt = 250)),
        )
        val collection = db.collectionDao()
        fun item(tmdbId: Long, format: MediaFormat, type: CollectionMediaType = CollectionMediaType.MOVIE) = CollectionItemEntity(
            mediaType = type, tmdbId = tmdbId, format = format, edition = null, region = null, quantity = 1, notes = null,
            acquiredAt = null, addedAt = 1, updatedAt = 1,
        )
        collection.insert(item(1, MediaFormat.UHD_4K_BLURAY))
        collection.insert(item(1, MediaFormat.DVD)) // mesmo filme, outro formato: conta 1 filme
        collection.insert(item(10, MediaFormat.BLURAY, CollectionMediaType.TV_SHOW))

        val movies = dao.observeMovieCounts().first()
        assertEquals(1, movies.watched)
        assertEquals(1, movies.wantToWatch)
        assertEquals(1, movies.favorites)
        assertEquals(1, movies.inCollection)

        val shows = dao.observeTvShowCounts().first()
        assertEquals(3, shows.total)
        assertEquals(1, shows.watching)
        assertEquals(1, shows.completed)
        assertEquals(1, shows.wantToWatch)

        val episodes = dao.observeEpisodeCounts(monthStart = 200, yearStart = 100).first()
        assertEquals(3, episodes.total)
        assertEquals(1, episodes.thisMonth)
        assertEquals(2, episodes.thisYear)

        assertEquals(mapOf(8 to 2, 10 to 1), dao.observeRatingCounts().first().associate { it.rating to it.count })
        assertEquals(
            mapOf(MediaFormat.UHD_4K_BLURAY to 1, MediaFormat.DVD to 1, MediaFormat.BLURAY to 1),
            dao.observeFormatCounts().first().associate { it.format to it.count },
        )
    }

    @Test
    fun tempoSomaVisualizacoesComFallbackDeDuracao() = runBlocking {
        val cache = db.tmdbCacheDao()
        cache.saveMovie(movieCache(1, runtime = 120, genres = emptyList()))
        cache.saveMovie(movieCache(2, runtime = null, genres = emptyList()))
        cache.saveTvShow(
            TvShowCacheBundle(
                show = TmdbTvShowEntity(
                    id = 10, name = "Série", originalName = "", overview = null, posterPath = null, backdropPath = null,
                    firstAirDate = null, tmdbStatus = null, numberOfSeasons = 1, numberOfEpisodes = 2, episodeRunTime = 45,
                    voteAverage = null, creators = emptyList(), fetchedAt = 1,
                ),
                genres = emptyList(),
                seasons = listOf(TmdbSeasonEntity(7, 10, 1, "T1", null, null, null, 2, 1)),
                persons = emptyList(), credits = emptyList(),
            ),
        )
        db.tmdbEpisodeDao().saveSeason(
            TmdbSeasonEntity(7, 10, 1, "T1", null, null, null, 2, 1),
            listOf(
                TmdbEpisodeEntity(100, 10, 7, 1, 1, "E1", null, null, null, runtimeMinutes = 50, fetchedAt = 1),
                TmdbEpisodeEntity(101, 10, 7, 1, 2, "E2", null, null, null, runtimeMinutes = null, fetchedAt = 1),
            ),
        )
        val library = db.userLibraryDao()
        suspend fun watch(event: WatchHistoryEntity) = library.insertWatch(event)
        watch(WatchHistoryEntity(mediaType = WatchMediaType.MOVIE, movieId = 1, watchedAt = 1))
        watch(WatchHistoryEntity(mediaType = WatchMediaType.MOVIE, movieId = 1, watchedAt = 2)) // reassistiu
        watch(WatchHistoryEntity(mediaType = WatchMediaType.MOVIE, movieId = 2, watchedAt = 3)) // sem duração
        watch(WatchHistoryEntity(mediaType = WatchMediaType.MOVIE, movieId = 3, watchedAt = 4)) // sem cache
        watch(WatchHistoryEntity(mediaType = WatchMediaType.EPISODE, showId = 10, episodeId = 100, watchedAt = 5))
        watch(WatchHistoryEntity(mediaType = WatchMediaType.EPISODE, showId = 10, episodeId = 101, watchedAt = 6)) // média da série

        val time = dao.observeWatchTime().first()
        assertEquals(6, time.views)
        assertEquals(120L + 120L + 50L + 45L, time.minutes)
        assertEquals(2, time.withoutRuntime)
    }

    @Test
    fun generosSoDoQueFoiAssistidoCadaTituloUmaVez() = runBlocking {
        val cache = db.tmdbCacheDao()
        cache.saveMovie(movieCache(1, 100, listOf(drama, action)))
        cache.saveMovie(movieCache(2, 100, listOf(drama)))
        cache.saveMovie(movieCache(3, 100, listOf(action))) // quero assistir: fora
        fun show(id: Long) = TvShowCacheBundle(
            show = TmdbTvShowEntity(
                id = id, name = "S$id", originalName = "", overview = null, posterPath = null, backdropPath = null,
                firstAirDate = null, tmdbStatus = null, numberOfSeasons = null, numberOfEpisodes = null, episodeRunTime = null,
                voteAverage = null, creators = emptyList(), fetchedAt = 1,
            ),
            genres = listOf(drama), seasons = emptyList(), persons = emptyList(), credits = emptyList(),
        )
        cache.saveTvShow(show(10))
        cache.saveTvShow(show(11))
        val library = db.userLibraryDao()
        library.upsertMovie(UserMovieEntity(1, MovieStatus.WATCHED, addedAt = 1, updatedAt = 1))
        library.upsertMovie(UserMovieEntity(2, MovieStatus.WATCHED, addedAt = 1, updatedAt = 1))
        library.upsertMovie(UserMovieEntity(3, MovieStatus.WANT_TO_WATCH, addedAt = 1, updatedAt = 1))
        library.upsertTvShow(UserTvShowEntity(10, TvShowStatus.WATCHING, addedAt = 1, updatedAt = 1))
        library.upsertWatchedEpisodes(listOf(UserEpisodeEntity(100, 10, 1, 1, 1))) // assistindo com episódio: conta
        library.upsertTvShow(UserTvShowEntity(11, TvShowStatus.WATCHING, addedAt = 1, updatedAt = 1)) // sem episódio: fora

        val genres = dao.observeGenreCounts().first()
        assertEquals(listOf("Drama" to 3, "Ação" to 1), genres.map { it.name to it.count })
    }
}
