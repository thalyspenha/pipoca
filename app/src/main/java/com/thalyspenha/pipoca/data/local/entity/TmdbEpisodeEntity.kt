package com.thalyspenha.pipoca.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Episódio (cache TMDB, `tv/{id}/season/{n}`). Apagado junto com a temporada (CASCADE). */
@Entity(
    tableName = "tmdb_episode",
    foreignKeys = [
        ForeignKey(
            entity = TmdbSeasonEntity::class,
            parentColumns = ["id"],
            childColumns = ["season_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["show_id", "season_number", "episode_number"], unique = true),
        Index("season_id"),
    ],
)
data class TmdbEpisodeEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "show_id") val showId: Long,
    @ColumnInfo(name = "season_id") val seasonId: Long,
    @ColumnInfo(name = "season_number") val seasonNumber: Int,
    @ColumnInfo(name = "episode_number") val episodeNumber: Int,
    val name: String,
    val overview: String?,
    @ColumnInfo(name = "still_path") val stillPath: String?,
    /** Nula para episódio sem data anunciada; base de "episódio disponível" (DATABASE.md). */
    @ColumnInfo(name = "air_date") val airDate: LocalDate?,
    @ColumnInfo(name = "runtime_minutes") val runtimeMinutes: Int?,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
)
