package com.thalyspenha.pipoca.domain.usecase.collection

import com.thalyspenha.pipoca.domain.model.CollectionEntry
import com.thalyspenha.pipoca.domain.model.CollectionItem
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Coleção em memória; `titles` simula o cache TMDB por (tipo, id). */
class FakeCollectionRepository : CollectionRepository {
    val items = MutableStateFlow<Map<Long, CollectionItem>>(emptyMap())
    val titles = mutableMapOf<Pair<CollectionMediaType, Long>, String>()
    private var nextId = 1L

    override fun observeEntries(): Flow<List<CollectionEntry>> =
        items.map { all -> all.values.map { CollectionEntry(it, titles[it.mediaType to it.tmdbId], posterPath = null) } }

    override fun observeItemsFor(tmdbId: Long, mediaType: CollectionMediaType): Flow<List<CollectionItem>> =
        items.map { all -> all.values.filter { it.tmdbId == tmdbId && it.mediaType == mediaType } }

    override suspend fun getItem(id: Long): CollectionItem? = items.value[id]

    override suspend fun insert(item: CollectionItem): Long {
        val id = nextId++
        items.value += id to item.copy(id = id)
        return id
    }

    override suspend fun update(item: CollectionItem) {
        items.value += item.id to item
    }

    override suspend fun delete(id: Long) {
        items.value -= id
    }
}
