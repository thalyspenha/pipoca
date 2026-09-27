package com.thalyspenha.pipoca.presentation.screens.collection.form

import com.thalyspenha.pipoca.domain.model.COLLECTION_QUANTITY_RANGE
import com.thalyspenha.pipoca.domain.model.CollectionItem
import com.thalyspenha.pipoca.domain.model.CollectionItemDraft
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.MediaFormat
import java.time.LocalDate

/** Estado do formulário; campos de texto guardam exatamente o que foi digitado. */
data class CollectionItemFormState(
    val tmdbId: Long,
    val mediaType: CollectionMediaType,
    val isEditing: Boolean,
    val title: String? = null,
    val format: MediaFormat = MediaFormat.BLURAY,
    val edition: String = "",
    val region: String = "",
    val quantity: String = "1",
    val acquiredAt: LocalDate? = null,
    val notes: String = "",
    /** Mostra erros só depois da primeira tentativa de salvar. */
    val showErrors: Boolean = false,
    val isLoading: Boolean = false,
    /** Salvou ou removeu: a tela volta. */
    val isDone: Boolean = false,
) {
    val quantityError: String?
        get() = if (quantity.toIntOrNull()?.let { it in COLLECTION_QUANTITY_RANGE } == true) {
            null
        } else {
            "Informe de ${COLLECTION_QUANTITY_RANGE.first} a ${COLLECTION_QUANTITY_RANGE.last}"
        }

    fun dateError(today: LocalDate): String? =
        if (acquiredAt != null && acquiredAt.isAfter(today)) "A data não pode ser no futuro" else null

    fun isValid(today: LocalDate): Boolean = quantityError == null && dateError(today) == null

    fun toDraft() = CollectionItemDraft(
        tmdbId = tmdbId,
        mediaType = mediaType,
        format = format,
        edition = edition,
        region = region,
        quantity = checkNotNull(quantity.toIntOrNull()),
        acquiredAt = acquiredAt,
        notes = notes,
    )
}

fun CollectionItemFormState.withItem(item: CollectionItem) = copy(
    format = item.format,
    edition = item.edition.orEmpty(),
    region = item.region.orEmpty(),
    quantity = item.quantity.toString(),
    acquiredAt = item.acquiredAt,
    notes = item.notes.orEmpty(),
)
