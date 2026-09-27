package com.thalyspenha.pipoca.presentation.screens.home

import com.thalyspenha.pipoca.domain.model.LibraryMovieItem
import com.thalyspenha.pipoca.domain.model.LibraryTvShowItem
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.presentation.navigation.MovieDetailsRoute
import com.thalyspenha.pipoca.presentation.navigation.TvShowDetailsRoute
import java.time.Instant

/** Conteúdo da Home, montado a partir da biblioteca pessoal (D-030). */
data class HomeContent(
    val tmdbConfigured: Boolean,
    /** Séries com status assistindo. */
    val watching: List<HomeItem> = emptyList(),
    /** Filmes e séries em quero assistir. */
    val wantToWatch: List<HomeItem> = emptyList(),
    val favorites: List<HomeItem> = emptyList(),
    /** Filmes assistidos e séries concluídas, mais recentes primeiro. */
    val recentlyWatched: List<HomeItem> = emptyList(),
    val stats: HomeStats = HomeStats(),
) {
    val isLibraryEmpty: Boolean get() = stats.total == 0
}

data class HomeStats(
    val moviesWatched: Int = 0,
    val showsCompleted: Int = 0,
    val total: Int = 0,
)

enum class HomeItemType { MOVIE, TV_SHOW }

/** Pôster na Home. `title` nulo = ainda sem dados do TMDB em cache. */
data class HomeItem(
    val id: Long,
    val type: HomeItemType,
    val title: String?,
    val posterPath: String?,
    val year: Int?,
    val updatedAt: Instant,
) {
    val key: String get() = "$type-$id"
}

fun HomeItem.detailsRoute(): Any = when (type) {
    HomeItemType.MOVIE -> MovieDetailsRoute(id)
    HomeItemType.TV_SHOW -> TvShowDetailsRoute(id)
}

/** Máximo de pôsteres por seção. */
const val HOME_SECTION_LIMIT = 20

fun buildHomeContent(
    tmdbConfigured: Boolean,
    movies: List<LibraryMovieItem>,
    tvShows: List<LibraryTvShowItem>,
): HomeContent {
    val movieItems = movies.map { it to it.toHomeItem() }
    val showItems = tvShows.map { it to it.toHomeItem() }

    fun List<HomeItem>.recentFirst() = sortedByDescending(HomeItem::updatedAt).take(HOME_SECTION_LIMIT)

    return HomeContent(
        tmdbConfigured = tmdbConfigured,
        watching = showItems.filter { (show, _) -> show.show.status == TvShowStatus.WATCHING }
            .map { it.second }.recentFirst(),
        wantToWatch = (
            movieItems.filter { (movie, _) -> movie.movie.status == MovieStatus.WANT_TO_WATCH }.map { it.second } +
                showItems.filter { (show, _) -> show.show.status == TvShowStatus.WANT_TO_WATCH }.map { it.second }
            ).recentFirst(),
        favorites = (
            movieItems.filter { (movie, _) -> movie.movie.isFavorite }.map { it.second } +
                showItems.filter { (show, _) -> show.show.isFavorite }.map { it.second }
            ).recentFirst(),
        recentlyWatched = (
            movieItems.filter { (movie, _) -> movie.movie.status == MovieStatus.WATCHED }.map { it.second } +
                showItems.filter { (show, _) -> show.show.status == TvShowStatus.COMPLETED }.map { it.second }
            ).recentFirst(),
        stats = HomeStats(
            moviesWatched = movies.count { it.movie.status == MovieStatus.WATCHED },
            showsCompleted = tvShows.count { it.show.status == TvShowStatus.COMPLETED },
            total = movies.size + tvShows.size,
        ),
    )
}

private fun LibraryMovieItem.toHomeItem() = HomeItem(
    id = movie.movieId,
    type = HomeItemType.MOVIE,
    title = title,
    posterPath = posterPath,
    year = year,
    updatedAt = movie.updatedAt,
)

private fun LibraryTvShowItem.toHomeItem() = HomeItem(
    id = show.showId,
    type = HomeItemType.TV_SHOW,
    title = name,
    posterPath = posterPath,
    year = year,
    updatedAt = show.updatedAt,
)
