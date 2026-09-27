package com.thalyspenha.pipoca.presentation.navigation

import kotlinx.serialization.Serializable

// Rotas type-safe. Argumentos futuros (IDs TMDB) entram como propriedades.
@Serializable data object HomeRoute
@Serializable data object SearchRoute
@Serializable data object LibraryRoute
@Serializable data object CollectionRoute
@Serializable data object MoreRoute

// Detalhes: empilhados sobre a aba atual; só o ID TMDB vai na rota.
@Serializable data class MovieDetailsRoute(val id: Long)
@Serializable data class TvShowDetailsRoute(val id: Long)

// Temporada de uma série, empilhada sobre os detalhes.
@Serializable data class SeasonRoute(val showId: Long, val seasonNumber: Int)

/**
 * Formulário de item da coleção. `mediaType` = nome de `CollectionMediaType`;
 * `itemId` 0 = item novo (ids começam em 1).
 */
@Serializable data class CollectionItemFormRoute(val tmdbId: Long, val mediaType: String, val itemId: Long = 0)
