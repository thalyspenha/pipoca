package com.thalyspenha.pipoca.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.thalyspenha.pipoca.domain.model.MovieStatus

/**
 * Filme na biblioteca do usuário. Sem FK para `tmdb_movie` (D-007): limpar o cache nunca apaga dado pessoal.
 * Favorito e nota são colunas, não tabelas (D-009).
 */
@Entity(tableName = "user_movie")
data class UserMovieEntity(
    @PrimaryKey @ColumnInfo(name = "movie_id") val movieId: Long,
    val status: MovieStatus,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    /** Nota pessoal 1–10; validada no use case. */
    val rating: Int? = null,
    val notes: String? = null,
    @ColumnInfo(name = "added_at") val addedAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
