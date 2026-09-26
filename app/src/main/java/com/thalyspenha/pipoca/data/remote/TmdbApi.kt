package com.thalyspenha.pipoca.data.remote

/**
 * Endpoints do TMDB (ver TMDB.md). Vazia por enquanto: a busca e os detalhes
 * entram na fase de integração TMDB.
 */
interface TmdbApi {
    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
    }
}
