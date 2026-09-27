package com.thalyspenha.pipoca.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.thalyspenha.pipoca.domain.model.TvShowStatus

/** Série na biblioteca do usuário. Sem FK para `tmdb_tv_show` (D-007); favorito e nota como colunas (D-009). */
@Entity(tableName = "user_tv_show")
data class UserTvShowEntity(
    @PrimaryKey @ColumnInfo(name = "show_id") val showId: Long,
    val status: TvShowStatus,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    /** Nota pessoal 1–10; validada no use case. */
    val rating: Int? = null,
    val notes: String? = null,
    @ColumnInfo(name = "added_at") val addedAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
