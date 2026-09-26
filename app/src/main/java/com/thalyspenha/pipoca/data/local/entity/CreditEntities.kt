package com.thalyspenha.pipoca.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tmdb_person")
data class TmdbPersonEntity(
    @PrimaryKey val id: Long,
    val name: String,
    @ColumnInfo(name = "profile_path") val profilePath: String?,
)

enum class CreditMediaType { MOVIE, TV }

/** Elenco principal de um filme ou série. Sem FK: `media_id` aponta para tabelas diferentes conforme o tipo. */
@Entity(
    tableName = "tmdb_credit",
    primaryKeys = ["media_type", "media_id", "person_id"],
    indices = [Index("person_id")],
)
data class TmdbCreditEntity(
    @ColumnInfo(name = "media_type") val mediaType: CreditMediaType,
    @ColumnInfo(name = "media_id") val mediaId: Long,
    @ColumnInfo(name = "person_id") val personId: Long,
    val character: String?,
    val order: Int,
)

/** Linha de elenco já com os dados da pessoa (JOIN). */
data class CastRow(
    val id: Long,
    val name: String,
    val profilePath: String?,
    val character: String?,
    val order: Int,
)
