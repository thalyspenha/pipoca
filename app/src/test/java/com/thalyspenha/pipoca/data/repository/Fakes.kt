package com.thalyspenha.pipoca.data.repository

import com.thalyspenha.pipoca.data.local.dao.TmdbCacheDao
import com.thalyspenha.pipoca.data.local.dao.TvShowCacheInfo
import com.thalyspenha.pipoca.data.local.entity.CastRow
import com.thalyspenha.pipoca.data.local.entity.CreditMediaType
import com.thalyspenha.pipoca.data.local.entity.TmdbCreditEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbGenreEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbMovieEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbMovieGenreCrossRef
import com.thalyspenha.pipoca.data.local.entity.TmdbPersonEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbSeasonEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbTvShowEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbTvShowGenreCrossRef
import com.thalyspenha.pipoca.data.remote.TmdbApi
import com.thalyspenha.pipoca.data.remote.TmdbJson
import com.thalyspenha.pipoca.data.remote.dto.MovieDetailsDto
import com.thalyspenha.pipoca.data.remote.dto.MovieSummaryDto
import com.thalyspenha.pipoca.data.remote.dto.PagedResponseDto
import com.thalyspenha.pipoca.data.remote.dto.TvShowDetailsDto
import com.thalyspenha.pipoca.data.remote.dto.TvShowSummaryDto
import com.thalyspenha.pipoca.data.remote.fixture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.IOException

/** API fake baseada nas fixtures; conta chamadas e pode simular falha de rede. */
class FakeTmdbApi : TmdbApi {
    var calls = 0
    var failWithNetworkError = false
    var tvShowStatus: String? = null

    private fun hit() {
        calls++
        if (failWithNetworkError) throw IOException("offline")
    }

    override suspend fun searchMovies(query: String, page: Int, includeAdult: Boolean): PagedResponseDto<MovieSummaryDto> {
        hit()
        return TmdbJson.decodeFromString(fixture("search_movie.json"))
    }

    override suspend fun searchTvShows(query: String, page: Int, includeAdult: Boolean): PagedResponseDto<TvShowSummaryDto> {
        hit()
        return TmdbJson.decodeFromString(fixture("search_tv.json"))
    }

    override suspend fun getMovieDetails(id: Long, appendToResponse: String): MovieDetailsDto {
        hit()
        return TmdbJson.decodeFromString(fixture("movie_details.json"))
    }

    override suspend fun getTvShowDetails(id: Long, appendToResponse: String): TvShowDetailsDto {
        hit()
        val dto = TmdbJson.decodeFromString<TvShowDetailsDto>(fixture("tv_details.json"))
        return tvShowStatus?.let { dto.copy(status = it) } ?: dto
    }
}

/** DAO em memória. Herda as transações (`saveMovie`/`saveTvShow`) do DAO real. */
class FakeTmdbCacheDao : TmdbCacheDao() {
    private val movies = MutableStateFlow<Map<Long, TmdbMovieEntity>>(emptyMap())
    private val shows = MutableStateFlow<Map<Long, TmdbTvShowEntity>>(emptyMap())
    private val genres = mutableMapOf<Long, TmdbGenreEntity>()
    private val movieGenres = mutableSetOf<TmdbMovieGenreCrossRef>()
    private val showGenres = mutableSetOf<TmdbTvShowGenreCrossRef>()
    private val seasons = mutableMapOf<Long, TmdbSeasonEntity>()
    private val persons = mutableMapOf<Long, TmdbPersonEntity>()
    private val credits = mutableListOf<TmdbCreditEntity>()


    override fun observeMovie(id: Long): Flow<TmdbMovieEntity?> = movies.map { it[id] }
    override suspend fun getMovieFetchedAt(id: Long): Long? = movies.value[id]?.fetchedAt
    override suspend fun getMovieGenres(movieId: Long) =
        movieGenres.filter { it.movieId == movieId }.mapNotNull { genres[it.genreId] }.sortedBy { it.name }

    override suspend fun upsertMovie(movie: TmdbMovieEntity) {
        movies.value = movies.value + (movie.id to movie)
    }
    override suspend fun deleteMovieGenres(movieId: Long) {
        movieGenres.removeAll { it.movieId == movieId }
    }
    override suspend fun insertMovieGenres(refs: List<TmdbMovieGenreCrossRef>) {
        movieGenres += refs
    }

    override fun observeTvShow(id: Long): Flow<TmdbTvShowEntity?> = shows.map { it[id] }
    override suspend fun getTvShowCacheInfo(id: Long) = shows.value[id]?.let { TvShowCacheInfo(it.fetchedAt, it.tmdbStatus) }
    override suspend fun getTvShowGenres(showId: Long) =
        showGenres.filter { it.showId == showId }.mapNotNull { genres[it.genreId] }.sortedBy { it.name }
    override suspend fun getSeasons(showId: Long) = seasons.values.filter { it.showId == showId }.sortedBy { it.seasonNumber }

    override suspend fun upsertTvShow(show: TmdbTvShowEntity) {
        shows.value = shows.value + (show.id to show)
    }
    override suspend fun deleteTvShowGenres(showId: Long) {
        showGenres.removeAll { it.showId == showId }
    }
    override suspend fun insertTvShowGenres(refs: List<TmdbTvShowGenreCrossRef>) {
        showGenres += refs
    }
    override suspend fun deleteSeasonsNotIn(showId: Long, keepIds: List<Long>) {
        seasons.values.removeAll { it.showId == showId && it.id !in keepIds }
    }
    override suspend fun upsertSeasons(seasons: List<TmdbSeasonEntity>) {
        seasons.forEach { this.seasons[it.id] = it }
    }

    override suspend fun getCast(mediaType: CreditMediaType, mediaId: Long) =
        credits.filter { it.mediaType == mediaType && it.mediaId == mediaId }
            .sortedBy { it.order }
            .map { c ->
                val p = persons.getValue(c.personId)
                CastRow(p.id, p.name, p.profilePath, c.character, c.order)
            }

    override suspend fun upsertGenres(genres: List<TmdbGenreEntity>) {
        genres.forEach { this.genres[it.id] = it }
    }
    override suspend fun upsertPersons(persons: List<TmdbPersonEntity>) {
        persons.forEach { this.persons[it.id] = it }
    }
    override suspend fun deleteCredits(mediaType: CreditMediaType, mediaId: Long) {
        credits.removeAll { it.mediaType == mediaType && it.mediaId == mediaId }
    }
    override suspend fun insertCredits(credits: List<TmdbCreditEntity>) {
        this.credits += credits
    }
}
