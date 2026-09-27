package com.thalyspenha.pipoca.domain.repository

import com.thalyspenha.pipoca.domain.model.CollectionEntry
import com.thalyspenha.pipoca.domain.model.CollectionItem
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import kotlinx.coroutines.flow.Flow

/** Coleção física/digital (dados pessoais; nunca apagados pelo cache). Regras nos use cases. */
interface CollectionRepository {
    /** Todos os itens com título/pôster do cache (ordem livre; a tela filtra e ordena). */
    fun observeEntries(): Flow<List<CollectionEntry>>

    /** Itens de um título, para os detalhes do filme/série. */
    fun observeItemsFor(tmdbId: Long, mediaType: CollectionMediaType): Flow<List<CollectionItem>>

    suspend fun getItem(id: Long): CollectionItem?

    /** Grava item novo e devolve o id gerado. */
    suspend fun insert(item: CollectionItem): Long

    suspend fun update(item: CollectionItem)

    suspend fun delete(id: Long)
}
