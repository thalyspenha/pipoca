package com.thalyspenha.pipoca.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class WatchMediaType { MOVIE, EPISODE }

/** Evento de visualização (permite reassistir). Base do Histórico e das estatísticas. Sem FK (D-007). */
@Entity(
    tableName = "watch_history",
    indices = [Index("watched_at"), Index("movie_id"), Index("show_id")],
)
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "media_type") val mediaType: WatchMediaType,
    @ColumnInfo(name = "movie_id") val movieId: Long? = null,
    @ColumnInfo(name = "show_id") val showId: Long? = null,
    @ColumnInfo(name = "episode_id") val episodeId: Long? = null,
    @ColumnInfo(name = "watched_at") val watchedAt: Long,
)
