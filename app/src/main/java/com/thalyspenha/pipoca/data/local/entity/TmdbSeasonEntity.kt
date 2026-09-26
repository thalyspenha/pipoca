package com.thalyspenha.pipoca.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Resumo da temporada vindo dos detalhes da série. Episódios chegam em fase futura. */
@Entity(
    tableName = "tmdb_season",
    foreignKeys = [
        ForeignKey(
            entity = TmdbTvShowEntity::class,
            parentColumns = ["id"],
            childColumns = ["show_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["show_id", "season_number"], unique = true)],
)
data class TmdbSeasonEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "show_id") val showId: Long,
    @ColumnInfo(name = "season_number") val seasonNumber: Int,
    val name: String,
    val overview: String?,
    @ColumnInfo(name = "poster_path") val posterPath: String?,
    @ColumnInfo(name = "air_date") val airDate: LocalDate?,
    @ColumnInfo(name = "episode_count") val episodeCount: Int,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
)
