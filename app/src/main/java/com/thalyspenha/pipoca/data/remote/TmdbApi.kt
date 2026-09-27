package com.thalyspenha.pipoca.data.remote

import com.thalyspenha.pipoca.data.remote.dto.MovieDetailsDto
import com.thalyspenha.pipoca.data.remote.dto.MovieSummaryDto
import com.thalyspenha.pipoca.data.remote.dto.PagedResponseDto
import com.thalyspenha.pipoca.data.remote.dto.SeasonDetailsDto
import com.thalyspenha.pipoca.data.remote.dto.TvShowDetailsDto
import com.thalyspenha.pipoca.data.remote.dto.TvShowSummaryDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints do TMDB (ver TMDB.md). Autenticação e `language` entram pelos interceptors.
 * Erros HTTP chegam como `HttpException`; a conversão para erro de domínio fica no RemoteDataSource.
 */
interface TmdbApi {

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): PagedResponseDto<MovieSummaryDto>

    @GET("search/tv")
    suspend fun searchTvShows(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): PagedResponseDto<TvShowSummaryDto>

    @GET("movie/{id}")
    suspend fun getMovieDetails(
        @Path("id") id: Long,
        @Query("append_to_response") appendToResponse: String = CREDITS,
    ): MovieDetailsDto

    @GET("tv/{id}")
    suspend fun getTvShowDetails(
        @Path("id") id: Long,
        @Query("append_to_response") appendToResponse: String = CREDITS,
    ): TvShowDetailsDto

    @GET("tv/{id}/season/{seasonNumber}")
    suspend fun getSeasonDetails(
        @Path("id") showId: Long,
        @Path("seasonNumber") seasonNumber: Int,
    ): SeasonDetailsDto

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val CREDITS = "credits"
    }
}
