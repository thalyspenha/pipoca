package com.thalyspenha.pipoca.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thalyspenha.pipoca.data.local.dao.CollectionDao
import com.thalyspenha.pipoca.data.local.dao.MovieCacheBundle
import com.thalyspenha.pipoca.data.local.entity.CollectionItemEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbMovieEntity
import com.thalyspenha.pipoca.data.local.entity.UserMovieEntity
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.MediaFormat
import com.thalyspenha.pipoca.domain.model.MovieStatus
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
class CollectionDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: CollectionDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.collectionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun item(tmdbId: Long, type: CollectionMediaType, format: MediaFormat) = CollectionItemEntity(
        mediaType = type, tmdbId = tmdbId, format = format, edition = null, region = null, quantity = 1,
        notes = null, acquiredAt = null, addedAt = 1, updatedAt = 1,
    )

    @Test
    fun crudCompleto() = runBlocking {
        val id = dao.insert(item(603, CollectionMediaType.MOVIE, MediaFormat.UHD_4K_BLURAY))
        val saved = dao.get(id)!!
        assertEquals(MediaFormat.UHD_4K_BLURAY, saved.format)

        dao.update(saved.copy(edition = "Steelbook", region = "A/B", quantity = 2, acquiredAt = LocalDate.of(2024, 5, 1)))
        val edited = dao.get(id)!!
        assertEquals("Steelbook", edited.edition)
        assertEquals(LocalDate.of(2024, 5, 1), edited.acquiredAt)
        assertEquals(2, edited.quantity)

        dao.delete(id)
        assertNull(dao.get(id))
    }

    @Test
    fun variosItensPorTituloESeparadosPorTipo() = runBlocking {
        dao.insert(item(603, CollectionMediaType.MOVIE, MediaFormat.UHD_4K_BLURAY))
        dao.insert(item(603, CollectionMediaType.MOVIE, MediaFormat.DVD))
        dao.insert(item(603, CollectionMediaType.TV_SHOW, MediaFormat.BLURAY))

        assertEquals(2, dao.observeFor(603, CollectionMediaType.MOVIE).first().size)
        assertEquals(1, dao.observeFor(603, CollectionMediaType.TV_SHOW).first().size)
    }

    @Test
    fun tituloEPosterVemDoCacheCertoConformeOTipo() = runBlocking {
        db.tmdbCacheDao().saveMovie(
            MovieCacheBundle(
                TmdbMovieEntity(
                    id = 603, title = "Matrix", originalTitle = "The Matrix", overview = null, posterPath = "/m.jpg",
                    backdropPath = null, releaseDate = null, runtimeMinutes = null, voteAverage = null,
                    directors = emptyList(), fetchedAt = 1,
                ),
                genres = emptyList(), persons = emptyList(), credits = emptyList(),
            ),
        )
        dao.insert(item(603, CollectionMediaType.MOVIE, MediaFormat.BLURAY))
        dao.insert(item(603, CollectionMediaType.TV_SHOW, MediaFormat.DVD)) // mesma id, outro tipo: sem cache

        val rows = dao.observeWithCache().first().associateBy { it.item.mediaType }
        assertEquals("Matrix", rows.getValue(CollectionMediaType.MOVIE).title)
        assertEquals("/m.jpg", rows.getValue(CollectionMediaType.MOVIE).posterPath)
        assertNull(rows.getValue(CollectionMediaType.TV_SHOW).title)
    }

    @Test
    fun colecaoNaoMexeNaBiblioteca() = runBlocking {
        db.userLibraryDao().upsertMovie(UserMovieEntity(603, MovieStatus.WANT_TO_WATCH, addedAt = 1, updatedAt = 1))
        val id = dao.insert(item(603, CollectionMediaType.MOVIE, MediaFormat.UHD_4K_BLURAY))

        dao.delete(id)

        assertEquals(MovieStatus.WANT_TO_WATCH, db.userLibraryDao().getMovie(603)?.status)
    }
}
