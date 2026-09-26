package com.thalyspenha.pipoca.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "tmdb_movie_genre",
    primaryKeys = ["movie_id", "genre_id"],
    foreignKeys = [
        ForeignKey(
            entity = TmdbMovieEntity::class,
            parentColumns = ["id"],
            childColumns = ["movie_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TmdbGenreEntity::class,
            parentColumns = ["id"],
            childColumns = ["genre_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("genre_id")],
)
data class TmdbMovieGenreCrossRef(
    @ColumnInfo(name = "movie_id") val movieId: Long,
    @ColumnInfo(name = "genre_id") val genreId: Long,
)

@Entity(
    tableName = "tmdb_tv_show_genre",
    primaryKeys = ["show_id", "genre_id"],
    foreignKeys = [
        ForeignKey(
            entity = TmdbTvShowEntity::class,
            parentColumns = ["id"],
            childColumns = ["show_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TmdbGenreEntity::class,
            parentColumns = ["id"],
            childColumns = ["genre_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("genre_id")],
)
data class TmdbTvShowGenreCrossRef(
    @ColumnInfo(name = "show_id") val showId: Long,
    @ColumnInfo(name = "genre_id") val genreId: Long,
)
