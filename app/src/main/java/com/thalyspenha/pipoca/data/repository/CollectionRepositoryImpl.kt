package com.thalyspenha.pipoca.data.repository

import com.thalyspenha.pipoca.data.local.dao.CollectionDao
import com.thalyspenha.pipoca.data.mapper.toDomain
import com.thalyspenha.pipoca.data.mapper.toEntity
import com.thalyspenha.pipoca.domain.model.CollectionEntry
import com.thalyspenha.pipoca.domain.model.CollectionItem
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CollectionRepositoryImpl @Inject constructor(
    private val dao: CollectionDao,
) : CollectionRepository {

    override fun observeEntries(): Flow<List<CollectionEntry>> =
        dao.observeWithCache().map { list -> list.map { it.toDomain() } }

    override fun observeItemsFor(tmdbId: Long, mediaType: CollectionMediaType): Flow<List<CollectionItem>> =
        dao.observeFor(tmdbId, mediaType).map { list -> list.map { it.toDomain() } }

    override suspend fun getItem(id: Long): CollectionItem? = dao.get(id)?.toDomain()

    override suspend fun insert(item: CollectionItem): Long = dao.insert(item.toEntity().copy(id = 0))

    override suspend fun update(item: CollectionItem) = dao.update(item.toEntity())

    override suspend fun delete(id: Long) = dao.delete(id)
}
