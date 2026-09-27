package com.thalyspenha.pipoca.domain.usecase.collection

import com.thalyspenha.pipoca.domain.model.COLLECTION_QUANTITY_RANGE
import com.thalyspenha.pipoca.domain.model.CollectionItem
import com.thalyspenha.pipoca.domain.model.CollectionItemDraft
import com.thalyspenha.pipoca.domain.repository.CollectionRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Valida o rascunho (D-040): quantidade em 1–99, data de aquisição não futura; textos aparados,
 * em branco viram nulo. Lança [IllegalArgumentException]; a UI valida antes de salvar.
 */
private fun CollectionItemDraft.normalized(today: LocalDate): CollectionItemDraft {
    require(quantity in COLLECTION_QUANTITY_RANGE) { "Quantidade fora de $COLLECTION_QUANTITY_RANGE: $quantity" }
    require(acquiredAt == null || !acquiredAt.isAfter(today)) { "Data de aquisição no futuro: $acquiredAt" }
    return copy(
        edition = edition.cleaned(),
        region = region.cleaned(),
        notes = notes.cleaned(),
    )
}

private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/** Adiciona um item. Não toca na biblioteca: coleção e status assistido são independentes. */
class AddCollectionItemUseCase @Inject constructor(
    private val repository: CollectionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(draft: CollectionItemDraft): Long {
        val clean = draft.normalized(LocalDate.now(clock))
        val now = clock.instant()
        return repository.insert(
            CollectionItem(
                tmdbId = clean.tmdbId,
                mediaType = clean.mediaType,
                format = clean.format,
                edition = clean.edition,
                region = clean.region,
                quantity = clean.quantity,
                acquiredAt = clean.acquiredAt,
                notes = clean.notes,
                addedAt = now,
                updatedAt = now,
            ),
        )
    }
}

/** Edita um item existente (título e tipo não mudam). Item inexistente: nada acontece. */
class UpdateCollectionItemUseCase @Inject constructor(
    private val repository: CollectionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(id: Long, draft: CollectionItemDraft) {
        val current = repository.getItem(id) ?: return
        val clean = draft.normalized(LocalDate.now(clock))
        repository.update(
            current.copy(
                format = clean.format,
                edition = clean.edition,
                region = clean.region,
                quantity = clean.quantity,
                acquiredAt = clean.acquiredAt,
                notes = clean.notes,
                updatedAt = clock.instant(),
            ),
        )
    }
}

class RemoveCollectionItemUseCase @Inject constructor(
    private val repository: CollectionRepository,
) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
