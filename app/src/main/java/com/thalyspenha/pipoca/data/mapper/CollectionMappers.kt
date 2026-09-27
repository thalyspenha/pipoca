package com.thalyspenha.pipoca.data.mapper

import com.thalyspenha.pipoca.data.local.dao.CollectionItemWithCache
import com.thalyspenha.pipoca.data.local.entity.CollectionItemEntity
import com.thalyspenha.pipoca.domain.model.CollectionEntry
import com.thalyspenha.pipoca.domain.model.CollectionItem
import java.time.Instant

fun CollectionItemEntity.toDomain() = CollectionItem(
    id = id,
    tmdbId = tmdbId,
    mediaType = mediaType,
    format = format,
    edition = edition,
    region = region,
    quantity = quantity,
    acquiredAt = acquiredAt,
    notes = notes,
    addedAt = Instant.ofEpochMilli(addedAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

fun CollectionItem.toEntity() = CollectionItemEntity(
    id = id,
    mediaType = mediaType,
    tmdbId = tmdbId,
    format = format,
    edition = edition,
    region = region,
    quantity = quantity,
    notes = notes,
    acquiredAt = acquiredAt,
    addedAt = addedAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

fun CollectionItemWithCache.toDomain() = CollectionEntry(item = item.toDomain(), title = title, posterPath = posterPath)
