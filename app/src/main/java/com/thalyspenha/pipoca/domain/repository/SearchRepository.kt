package com.thalyspenha.pipoca.domain.repository

import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.MovieSummary
import com.thalyspenha.pipoca.domain.model.SearchPage
import com.thalyspenha.pipoca.domain.model.TvShowSummary

/** Busca vai direto à rede e não é persistida (D-008). */
interface SearchRepository {
    suspend fun searchMovies(query: String, page: Int = 1): DataResult<SearchPage<MovieSummary>>
    suspend fun searchTvShows(query: String, page: Int = 1): DataResult<SearchPage<TvShowSummary>>

    companion object {
        /** Consultas menores que isso devolvem página vazia sem chamar a rede (TMDB.md). */
        const val MIN_QUERY_LENGTH = 2
    }
}
