package com.thalyspenha.pipoca.presentation.screens.search

import com.thalyspenha.pipoca.presentation.navigation.MovieDetailsRoute
import com.thalyspenha.pipoca.presentation.navigation.TvShowDetailsRoute

/** Rota de detalhes para um resultado da busca. */
fun SearchResultItem.detailsRoute(): Any = when (type) {
    SearchType.MOVIES -> MovieDetailsRoute(id)
    SearchType.TV_SHOWS -> TvShowDetailsRoute(id)
}
