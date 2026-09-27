package com.thalyspenha.pipoca.domain.usecase.collection

import com.thalyspenha.pipoca.data.repository.MutableClock
import com.thalyspenha.pipoca.domain.model.CollectionItemDraft
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.MediaFormat
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.usecase.library.FakeLibraryRepository
import com.thalyspenha.pipoca.domain.usecase.library.SetMovieStatusUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

class CollectionUseCasesTest {
    private val repository = FakeCollectionRepository()
    private val clock = MutableClock() // 2026-09-26T12:00Z
    private val add = AddCollectionItemUseCase(repository, clock)
    private val update = UpdateCollectionItemUseCase(repository, clock)
    private val remove = RemoveCollectionItemUseCase(repository)

    private fun draft(
        format: MediaFormat = MediaFormat.UHD_4K_BLURAY,
        quantity: Int = 1,
        acquiredAt: LocalDate? = null,
        edition: String? = null,
    ) = CollectionItemDraft(
        tmdbId = 603, mediaType = CollectionMediaType.MOVIE, format = format,
        edition = edition, quantity = quantity, acquiredAt = acquiredAt,
    )

    @Test
    fun `adicionar grava item com datas`() = runTest {
        val id = add(draft(edition = "  Steelbook  ", acquiredAt = LocalDate.of(2024, 5, 1)))

        val item = repository.getItem(id)!!
        assertEquals(MediaFormat.UHD_4K_BLURAY, item.format)
        assertEquals("Steelbook", item.edition)
        assertEquals(LocalDate.of(2024, 5, 1), item.acquiredAt)
        assertEquals(clock.now, item.addedAt)
        assertEquals(clock.now, item.updatedAt)
    }

    @Test
    fun `mesmo titulo pode ter varios itens`() = runTest {
        add(draft(MediaFormat.UHD_4K_BLURAY))
        add(draft(MediaFormat.DVD))

        assertEquals(2, repository.observeItemsFor(603, CollectionMediaType.MOVIE).first().size)
    }

    @Test
    fun `editar mantem titulo, tipo e addedAt e limpa texto em branco`() = runTest {
        val id = add(draft(edition = "Steelbook"))
        val addedAt = clock.now
        clock.advance(Duration.ofDays(2))

        update(id, draft(format = MediaFormat.BLURAY, quantity = 3, edition = "   ").copy(tmdbId = 999))

        val item = repository.getItem(id)!!
        assertEquals(MediaFormat.BLURAY, item.format)
        assertEquals(3, item.quantity)
        assertNull(item.edition)
        assertEquals(603L, item.tmdbId)
        assertEquals(addedAt, item.addedAt)
        assertEquals(clock.now, item.updatedAt)
    }

    @Test
    fun `editar item inexistente nao cria nada`() = runTest {
        update(42, draft())

        assertEquals(emptyMap<Long, Any>(), repository.items.value)
    }

    @Test
    fun `remover apaga so aquele item`() = runTest {
        val first = add(draft(MediaFormat.UHD_4K_BLURAY))
        val second = add(draft(MediaFormat.DVD))

        remove(first)

        assertNull(repository.getItem(first))
        assertEquals(MediaFormat.DVD, repository.getItem(second)?.format)
    }

    @Test
    fun `quantidade fora de 1 a 99 e rejeitada`() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { add(draft(quantity = 0)) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { add(draft(quantity = 100)) } }
    }

    @Test
    fun `data de aquisicao no futuro e rejeitada, hoje e aceita`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { add(draft(acquiredAt = LocalDate.of(2026, 9, 27))) }
        }
        add(draft(acquiredAt = LocalDate.of(2026, 9, 26)))
    }

    @Test
    fun `colecao e status assistido sao independentes`() = runTest {
        val library = FakeLibraryRepository()
        val setStatus = SetMovieStatusUseCase(library, clock)
        setStatus(603, MovieStatus.WANT_TO_WATCH)
        val id = add(draft(MediaFormat.UHD_4K_BLURAY))

        setStatus(603, MovieStatus.WATCHED)
        assertEquals(MediaFormat.UHD_4K_BLURAY, repository.getItem(id)?.format)

        remove(id)
        assertEquals(MovieStatus.WATCHED, library.getMovie(603)?.status)
    }
}
