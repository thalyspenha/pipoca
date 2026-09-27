package com.thalyspenha.pipoca.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Episódio assistido (linha existe = assistido; desmarcar = apagar). Sem FK para o cache (D-007).
 * Temporada e número são redundantes de propósito: progresso funciona mesmo sem cache.
 */
@Entity(tableName = "user_episode", indices = [Index("show_id")])
data class UserEpisodeEntity(
    @PrimaryKey @ColumnInfo(name = "episode_id") val episodeId: Long,
    @ColumnInfo(name = "show_id") val showId: Long,
    @ColumnInfo(name = "season_number") val seasonNumber: Int,
    @ColumnInfo(name = "episode_number") val episodeNumber: Int,
    @ColumnInfo(name = "watched_at") val watchedAt: Long,
)
