package com.thalyspenha.pipoca.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cache de gêneros do TMDB (ver DATABASE.md). Primeira tabela; as demais chegam nas fases seguintes. */
@Entity(tableName = "tmdb_genre")
data class TmdbGenreEntity(
    @PrimaryKey val id: Long,
    val name: String,
)
