package com.thalyspenha.pipoca.presentation.screens.home

import com.thalyspenha.pipoca.domain.model.CollectionEntry
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.LibraryMovieItem
import com.thalyspenha.pipoca.domain.model.LibraryTvShowItem
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.usecase.episodes.WatchingShow
import com.thalyspenha.pipoca.presentation.navigation.MovieDetailsRoute
import com.thalyspenha.pipoca.presentation.navigation.TvShowDetailsRoute
import java.time.Instant

/** Conteúdo da Home: painel pessoal (D-030, D-044). */
data class HomeContent(
    val tmdbConfigured: Boolean,
    /** Próximo episódio de cada série assistindo, da vista mais recentemente para a mais antiga. */
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    /** Séries assistindo com progresso. */
    val inProgress: List<InProgressItem> = emptyList(),
    /** Filmes e séries em quero assistir. */
    val wantToWatch: List<HomeItem> = emptyList(),
    val favorites: List<HomeItem> = emptyList(),
    /** Filmes assistidos e séries concluídas, mais recentes primeiro. */
    val recentlyWatched: List<HomeItem> = emptyList(),
    /** Títulos adicionados recentemente à coleção (um por título, D-039). */
    val recentCollection: List<HomeItem> = emptyList(),
    val stats: HomeStats = HomeStats(),
) {
    val isLibraryEmpty: Boolean get() = stats.total == 0 && recentCollection.isEmpty()
}

data class ContinueWatchingItem(
    val showId: Long,
    val showName: String?,
    val posterPath: String?,
    val episode: Episode,
)

data class InProgressItem(
    val showId: Long,
    val name: String?,
    val posterPath: String?,
    val watched: Int,
    val available: Int,
    val percent: Int,
    val isCaughtUp: Boolean,
    /** Temporadas ainda não baixadas: números parciais. */
    val isComplete: Boolean,
)

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
    watching: List<WatchingShow> = emptyList(),
    collection: List<CollectionEntry> = emptyList(),
): HomeContent {
    val movieItems = movies.map { it to it.toHomeItem() }
    val showItems = tvShows.map { it to it.toHomeItem() }

    fun List<HomeItem>.recentFirst() = sortedByDescending(HomeItem::updatedAt).take(HOME_SECTION_LIMIT)

    return HomeContent(
        tmdbConfigured = tmdbConfigured,
        continueWatching = watching.mapNotNull { show ->
            show.progress.nextEpisode?.let {
                ContinueWatchingItem(show.item.show.showId, show.item.name, show.item.posterPath, it)
            }
        }.take(HOME_SECTION_LIMIT),
        inProgress = watching.map { it.toInProgressItem() }.take(HOME_SECTION_LIMIT),
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
        recentCollection = collection
            .sortedByDescending { it.item.addedAt }
            .distinctBy { it.item.mediaType to it.item.tmdbId }
            .take(HOME_SECTION_LIMIT)
            .map { it.toHomeItem() },
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

private fun WatchingShow.toInProgressItem() = InProgressItem(
    showId = item.show.showId,
    name = item.name,
    posterPath = item.posterPath,
    watched = progress.watched,
    available = progress.available,
    percent = progress.percent,
    isCaughtUp = progress.isCaughtUp,
    isComplete = progress.isComplete,
)

private fun CollectionEntry.toHomeItem() = HomeItem(
    id = item.tmdbId,
    type = if (item.mediaType == CollectionMediaType.MOVIE) HomeItemType.MOVIE else HomeItemType.TV_SHOW,
    title = title,
    posterPath = posterPath,
    year = null,
    updatedAt = item.addedAt,
)
