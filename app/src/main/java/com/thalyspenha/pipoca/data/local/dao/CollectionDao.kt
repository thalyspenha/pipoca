package com.thalyspenha.pipoca.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.thalyspenha.pipoca.data.local.entity.CollectionItemEntity
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import kotlinx.coroutines.flow.Flow

/** Item com título/pôster do cache de filme ou série, conforme o tipo (LEFT JOIN: nulos sem cache). */
data class CollectionItemWithCache(
    @Embedded val item: CollectionItemEntity,
    val title: String?,
    @ColumnInfo(name = "poster_path") val posterPath: String?,
)

@Dao
interface CollectionDao {

    @Query(
        """SELECT c.*, COALESCE(m.title, t.name) AS title, COALESCE(m.poster_path, t.poster_path) AS poster_path
        FROM collection_item c
        LEFT JOIN tmdb_movie m ON c.media_type = 'MOVIE' AND m.id = c.tmdb_id
        LEFT JOIN tmdb_tv_show t ON c.media_type = 'TV_SHOW' AND t.id = c.tmdb_id""",
    )
    fun observeWithCache(): Flow<List<CollectionItemWithCache>>

    @Query("SELECT * FROM collection_item WHERE tmdb_id = :tmdbId AND media_type = :mediaType ORDER BY added_at")
    fun observeFor(tmdbId: Long, mediaType: CollectionMediaType): Flow<List<CollectionItemEntity>>

    @Query("SELECT * FROM collection_item WHERE id = :id")
    suspend fun get(id: Long): CollectionItemEntity?

    @Insert
    suspend fun insert(item: CollectionItemEntity): Long

    @Update
    suspend fun update(item: CollectionItemEntity)

    @Query("DELETE FROM collection_item WHERE id = :id")
    suspend fun delete(id: Long)
}
