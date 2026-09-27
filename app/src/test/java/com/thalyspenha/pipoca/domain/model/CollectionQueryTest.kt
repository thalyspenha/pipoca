package com.thalyspenha.pipoca.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CollectionQueryTest {

    private fun entry(
        id: Long,
        title: String?,
        format: MediaFormat,
        added: Long,
        acquired: LocalDate? = null,
    ) = CollectionEntry(
        item = CollectionItem(
            id = id, tmdbId = id, mediaType = CollectionMediaType.MOVIE, format = format, acquiredAt = acquired,
            addedAt = Instant.ofEpochSecond(added), updatedAt = Instant.ofEpochSecond(added),
        ),
        title = title,
        posterPath = null,
    )

    private val entries = listOf(
        entry(1, "Matrix", MediaFormat.UHD_4K_BLURAY, added = 10, acquired = LocalDate.of(2020, 1, 1)),
        entry(2, "Árvore da Vida", MediaFormat.BLURAY, added = 30),
        entry(3, "alien", MediaFormat.DVD, added = 20, acquired = LocalDate.of(2023, 6, 1)),
        entry(4, null, MediaFormat.DIGITAL, added = 40, acquired = LocalDate.of(2021, 1, 1)),
        entry(5, "Zodíaco", MediaFormat.OTHER, added = 5),
    )

    private fun List<CollectionEntry>.ids() = map { it.item.id }

    @Test
    fun `titulo ignora acento e maiuscula, sem titulo no fim`() {
        assertEquals(listOf(3L, 2L, 1L, 5L, 4L), entries.filterAndSort(CollectionFilter.ALL, CollectionSort.TITLE).ids())
    }

    @Test
    fun `adicionado recentemente primeiro`() {
        assertEquals(
            listOf(4L, 2L, 3L, 1L, 5L),
            entries.filterAndSort(CollectionFilter.ALL, CollectionSort.RECENTLY_ADDED).ids(),
        )
    }

    @Test
    fun `data de aquisicao mais recente primeiro, sem data no fim por adicionado`() {
        assertEquals(
            listOf(3L, 4L, 1L, 2L, 5L),
            entries.filterAndSort(CollectionFilter.ALL, CollectionSort.ACQUISITION_DATE).ids(),
        )
    }

    @Test
    fun `filtros por formato, outro so em todos`() {
        assertEquals(listOf(1L), entries.filterAndSort(CollectionFilter.UHD_4K, CollectionSort.TITLE).ids())
        assertEquals(listOf(2L), entries.filterAndSort(CollectionFilter.BLURAY, CollectionSort.TITLE).ids())
        assertEquals(listOf(3L), entries.filterAndSort(CollectionFilter.DVD, CollectionSort.TITLE).ids())
        assertEquals(listOf(4L), entries.filterAndSort(CollectionFilter.DIGITAL, CollectionSort.TITLE).ids())
        assertEquals(5, entries.filterAndSort(CollectionFilter.ALL, CollectionSort.TITLE).size)
    }
}
