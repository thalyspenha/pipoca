package com.thalyspenha.pipoca.domain.model

import java.time.Instant
import java.time.LocalDate

/** Formato da mídia na coleção (ordem = ordem de exibição). */
enum class MediaFormat { UHD_4K_BLURAY, BLURAY, DVD, DIGITAL, OTHER }

enum class CollectionMediaType { MOVIE, TV_SHOW }

/**
 * Item da coleção física/digital. Independente da biblioteca (status assistido, favorito): um
 * título pode ter vários itens (ex.: 4K e DVD) e estar em qualquer status (D-039, D-040).
 */
data class CollectionItem(
    val id: Long = 0,
    val tmdbId: Long,
    val mediaType: CollectionMediaType,
    val format: MediaFormat,
    val edition: String? = null,
    val region: String? = null,
    val quantity: Int = 1,
    val acquiredAt: LocalDate? = null,
    val notes: String? = null,
    val addedAt: Instant,
    val updatedAt: Instant,
)

/** Item com título/pôster do cache TMDB para exibir; nulos quando ainda não há cache. */
data class CollectionEntry(
    val item: CollectionItem,
    val title: String?,
    val posterPath: String?,
)

/** Dados editáveis de um item (formulário). Texto em branco vira nulo na gravação. */
data class CollectionItemDraft(
    val tmdbId: Long,
    val mediaType: CollectionMediaType,
    val format: MediaFormat,
    val edition: String? = null,
    val region: String? = null,
    val quantity: Int = 1,
    val acquiredAt: LocalDate? = null,
    val notes: String? = null,
)

/** Quantidade aceita por item. */
val COLLECTION_QUANTITY_RANGE = 1..99
