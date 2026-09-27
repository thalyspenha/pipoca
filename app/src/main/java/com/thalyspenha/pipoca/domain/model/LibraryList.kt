package com.thalyspenha.pipoca.domain.model

import java.text.Normalizer

/** Filtros de filmes da tela Biblioteca (D-050). */
enum class MovieLibraryFilter(val status: MovieStatus?, val favoritesOnly: Boolean = false) {
    ALL(null),
    WANT_TO_WATCH(MovieStatus.WANT_TO_WATCH),
    WATCHED(MovieStatus.WATCHED),
    FAVORITES(null, favoritesOnly = true),
}

/** Filtros de séries: exatamente os status do banco + favoritos. */
enum class TvShowLibraryFilter(val status: TvShowStatus?, val favoritesOnly: Boolean = false) {
    ALL(null),
    WANT_TO_WATCH(TvShowStatus.WANT_TO_WATCH),
    WATCHING(TvShowStatus.WATCHING),
    COMPLETED(TvShowStatus.COMPLETED),
    PAUSED(TvShowStatus.PAUSED),
    DROPPED(TvShowStatus.DROPPED),
    FAVORITES(null, favoritesOnly = true),
}

/**
 * Ordenação. Todas no SQL, exceto [PROGRESS] (calculado, em memória). [PROGRESS] e
 * [LAST_EPISODE] só existem para séries.
 */
enum class LibrarySort(val forMovies: Boolean = true) {
    RECENTLY_ADDED,
    TITLE_ASC,
    TITLE_DESC,
    RELEASE_YEAR,
    LAST_ACTIVITY,
    RATING,
    PROGRESS(forMovies = false),
    LAST_EPISODE(forMovies = false),
}

data class MovieLibraryCounts(val all: Int, val wantToWatch: Int, val watched: Int, val favorites: Int) {
    fun of(filter: MovieLibraryFilter): Int = when (filter) {
        MovieLibraryFilter.ALL -> all
        MovieLibraryFilter.WANT_TO_WATCH -> wantToWatch
        MovieLibraryFilter.WATCHED -> watched
        MovieLibraryFilter.FAVORITES -> favorites
    }
}

data class TvShowLibraryCounts(
    val all: Int,
    val wantToWatch: Int,
    val watching: Int,
    val completed: Int,
    val paused: Int,
    val dropped: Int,
    val favorites: Int,
) {
    fun of(filter: TvShowLibraryFilter): Int = when (filter) {
        TvShowLibraryFilter.ALL -> all
        TvShowLibraryFilter.WANT_TO_WATCH -> wantToWatch
        TvShowLibraryFilter.WATCHING -> watching
        TvShowLibraryFilter.COMPLETED -> completed
        TvShowLibraryFilter.PAUSED -> paused
        TvShowLibraryFilter.DROPPED -> dropped
        TvShowLibraryFilter.FAVORITES -> favorites
    }
}

/** Minúsculas e sem acento: "Árvore" e "arvore" batem. Base da pesquisa local (D-050). */
fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(COMBINING_MARKS, "").lowercase().trim()

private val COMBINING_MARKS = "\\p{Mn}+".toRegex()

/** Título contém a busca, ignorando acento e caixa. Busca em branco aceita tudo; sem título, só busca vazia. */
fun matchesSearch(title: String?, query: String): Boolean {
    val q = query.normalizedForSearch()
    if (q.isEmpty()) return true
    return title?.normalizedForSearch()?.contains(q) == true
}
