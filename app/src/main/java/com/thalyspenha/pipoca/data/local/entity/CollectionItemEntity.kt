package com.thalyspenha.pipoca.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.MediaFormat
import java.time.LocalDate

/** Item da coleção (DATABASE.md). Sem FK para o cache (D-007); vários itens por título. */
@Entity(tableName = "collection_item", indices = [Index(value = ["media_type", "tmdb_id"])])
data class CollectionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "media_type") val mediaType: CollectionMediaType,
    @ColumnInfo(name = "tmdb_id") val tmdbId: Long,
    val format: MediaFormat,
    val edition: String?,
    val region: String?,
    val quantity: Int,
    val notes: String?,
    @ColumnInfo(name = "acquired_at") val acquiredAt: LocalDate?,
    @ColumnInfo(name = "added_at") val addedAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
