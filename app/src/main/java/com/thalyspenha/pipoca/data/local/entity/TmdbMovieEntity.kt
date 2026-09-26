package com.thalyspenha.pipoca.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "tmdb_movie")
data class TmdbMovieEntity(
    @PrimaryKey val id: Long,
    val title: String,
    @ColumnInfo(name = "original_title") val originalTitle: String,
    val overview: String?,
    @ColumnInfo(name = "poster_path") val posterPath: String?,
    @ColumnInfo(name = "backdrop_path") val backdropPath: String?,
    @ColumnInfo(name = "release_date") val releaseDate: LocalDate?,
    @ColumnInfo(name = "runtime_minutes") val runtimeMinutes: Int?,
    @ColumnInfo(name = "vote_average") val voteAverage: Double?,
    val directors: List<String>,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
)
