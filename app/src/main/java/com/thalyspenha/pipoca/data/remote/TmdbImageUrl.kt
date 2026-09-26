package com.thalyspenha.pipoca.data.remote

/** Monta URLs de imagem do TMDB. No banco guardamos só o `path` (ex.: `/abc.jpg`). */
object TmdbImageUrl {
    private const val BASE = "https://image.tmdb.org/t/p/"

    const val POSTER = "w500"
    const val POSTER_SMALL = "w342"
    const val BACKDROP = "w780"
    const val PROFILE = "w185"
    const val STILL = "w300"

    fun build(path: String?, size: String): String? =
        path?.takeIf { it.isNotBlank() }?.let { "$BASE$size$it" }
}
