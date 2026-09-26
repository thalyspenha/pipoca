package com.thalyspenha.pipoca.util

/**
 * Monta URLs de imagem do TMDB. No banco guardamos só o `path` (ex.: `/abc.jpg`).
 * Fica em `util` porque a UI também usa (presentation não depende de `data`).
 */
object TmdbImageUrl {
    private const val BASE = "https://image.tmdb.org/t/p/"

    const val POSTER = "w500"
    const val POSTER_SMALL = "w342"
    /** Miniatura para listas. */
    const val POSTER_THUMB = "w154"
    const val BACKDROP = "w780"
    const val PROFILE = "w185"
    const val STILL = "w300"

    fun build(path: String?, size: String): String? =
        path?.takeIf { it.isNotBlank() }?.let { "$BASE$size$it" }
}
