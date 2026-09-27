package com.thalyspenha.pipoca.domain.model

import java.text.Collator
import java.util.Locale

/** Filtros da tela "Minha Coleção" (`fase7.md`); `OTHER` só aparece em "Todos". */
enum class CollectionFilter(val format: MediaFormat?) {
    ALL(null),
    UHD_4K(MediaFormat.UHD_4K_BLURAY),
    BLURAY(MediaFormat.BLURAY),
    DVD(MediaFormat.DVD),
    DIGITAL(MediaFormat.DIGITAL),
    ;

    fun matches(item: CollectionItem): Boolean = format == null || item.format == format
}

enum class CollectionSort { TITLE, RECENTLY_ADDED, ACQUISITION_DATE }

private val titleCollator: Collator = Collator.getInstance(Locale.forLanguageTag("pt-BR")).apply {
    strength = Collator.PRIMARY // ignora acentos e maiúsculas: "Árvore" junto de "arvore"
}

/**
 * Filtra e ordena (D-040):
 * - título: alfabético pt-BR; sem título (sem cache) no fim;
 * - adicionado recentemente: `addedAt` decrescente;
 * - data de aquisição: mais recente primeiro; sem data no fim.
 * Empates: adicionado mais recente primeiro, depois id (ordem estável).
 */
fun List<CollectionEntry>.filterAndSort(filter: CollectionFilter, sort: CollectionSort): List<CollectionEntry> {
    val tieBreak = compareByDescending<CollectionEntry> { it.item.addedAt }.thenBy { it.item.id }
    val comparator: Comparator<CollectionEntry> = when (sort) {
        CollectionSort.TITLE -> compareBy<CollectionEntry> { it.title == null }
            .then { a, b -> titleCollator.compare(a.title.orEmpty(), b.title.orEmpty()) }
        CollectionSort.RECENTLY_ADDED -> compareByDescending { it.item.addedAt }
        CollectionSort.ACQUISITION_DATE -> compareBy<CollectionEntry> { it.item.acquiredAt == null }
            .thenByDescending { it.item.acquiredAt }
    }
    return filter { filter.matches(it.item) }.sortedWith(comparator.then(tieBreak))
}
