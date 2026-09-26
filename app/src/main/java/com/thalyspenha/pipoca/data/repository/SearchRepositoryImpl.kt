package com.thalyspenha.pipoca.data.repository

import com.thalyspenha.pipoca.data.mapper.toDomain
import com.thalyspenha.pipoca.data.mapper.toSearchPage
import com.thalyspenha.pipoca.data.remote.TmdbRemoteDataSource
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.MovieSummary
import com.thalyspenha.pipoca.domain.model.SearchPage
import com.thalyspenha.pipoca.domain.model.TvShowSummary
import com.thalyspenha.pipoca.domain.model.map
import com.thalyspenha.pipoca.domain.repository.SearchRepository
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
    private val remote: TmdbRemoteDataSource,
) : SearchRepository {

    override suspend fun searchMovies(query: String, page: Int): DataResult<SearchPage<MovieSummary>> {
        val trimmed = query.trim()
        if (trimmed.length < SearchRepository.MIN_QUERY_LENGTH) return DataResult.Success(emptyPage())
        return remote.searchMovies(trimmed, page).map { dto -> dto.toSearchPage { it.toDomain() } }
    }

    override suspend fun searchTvShows(query: String, page: Int): DataResult<SearchPage<TvShowSummary>> {
        val trimmed = query.trim()
        if (trimmed.length < SearchRepository.MIN_QUERY_LENGTH) return DataResult.Success(emptyPage())
        return remote.searchTvShows(trimmed, page).map { dto -> dto.toSearchPage { it.toDomain() } }
    }

    private fun <T> emptyPage() = SearchPage<T>(page = 1, totalPages = 0, totalResults = 0, items = emptyList())
}
