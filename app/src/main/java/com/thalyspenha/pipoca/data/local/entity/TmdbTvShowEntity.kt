package com.thalyspenha.pipoca.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "tmdb_tv_show")
data class TmdbTvShowEntity(
    @PrimaryKey val id: Long,
    val name: String,
    @ColumnInfo(name = "original_name") val originalName: String,
    val overview: String?,
    @ColumnInfo(name = "poster_path") val posterPath: String?,
    @ColumnInfo(name = "backdrop_path") val backdropPath: String?,
    @ColumnInfo(name = "first_air_date") val firstAirDate: LocalDate?,
    @ColumnInfo(name = "tmdb_status") val tmdbStatus: String?,
    @ColumnInfo(name = "number_of_seasons") val numberOfSeasons: Int?,
    @ColumnInfo(name = "number_of_episodes") val numberOfEpisodes: Int?,
    @ColumnInfo(name = "episode_run_time") val episodeRunTime: Int?,
    @ColumnInfo(name = "vote_average") val voteAverage: Double?,
    val creators: List<String>,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
)
