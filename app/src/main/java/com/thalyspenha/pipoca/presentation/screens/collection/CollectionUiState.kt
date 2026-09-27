package com.thalyspenha.pipoca.presentation.screens.collection

import com.thalyspenha.pipoca.domain.model.CollectionEntry
import com.thalyspenha.pipoca.domain.model.CollectionFilter
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.CollectionSort
import com.thalyspenha.pipoca.domain.model.MediaFormat
import com.thalyspenha.pipoca.presentation.navigation.MovieDetailsRoute
import com.thalyspenha.pipoca.presentation.navigation.TvShowDetailsRoute

data class CollectionUiState(
    val filter: CollectionFilter = CollectionFilter.ALL,
    val sort: CollectionSort = CollectionSort.TITLE,
    val items: List<CollectionListItem> = emptyList(),
    /** Total sem filtro: distingue "coleção vazia" de "nenhum item neste filtro". */
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
) {
    val isCollectionEmpty: Boolean get() = !isLoading && totalCount == 0
}

/** Linha da lista. `title` nulo = ainda sem dados do TMDB em cache. */
data class CollectionListItem(
    val id: Long,
    val tmdbId: Long,
    val mediaType: CollectionMediaType,
    val title: String?,
    val posterPath: String?,
    val format: MediaFormat,
    val edition: String?,
    val quantity: Int,
)

fun CollectionEntry.toListItem() = CollectionListItem(
    id = item.id,
    tmdbId = item.tmdbId,
    mediaType = item.mediaType,
    title = title,
    posterPath = posterPath,
    format = item.format,
    edition = item.edition,
    quantity = item.quantity,
)

fun CollectionListItem.detailsRoute(): Any = when (mediaType) {
    CollectionMediaType.MOVIE -> MovieDetailsRoute(tmdbId)
    CollectionMediaType.TV_SHOW -> TvShowDetailsRoute(tmdbId)
}
