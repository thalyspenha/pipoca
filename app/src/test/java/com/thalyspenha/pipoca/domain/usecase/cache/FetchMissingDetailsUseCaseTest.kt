package com.thalyspenha.pipoca.domain.usecase.cache

import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.MovieDetails
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import com.thalyspenha.pipoca.domain.repository.MovieRepository
import com.thalyspenha.pipoca.domain.usecase.episodes.FakeSeasonRepository
import com.thalyspenha.pipoca.domain.usecase.episodes.FakeTvShowRepository
import com.thalyspenha.pipoca.domain.usecase.episodes.RefreshShowEpisodesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FetchMissingDetailsUseCaseTest {
    private val tvShows = FakeTvShowRepository()

    private class CountingMovies : MovieRepository {
        val calls = mutableListOf<Long>()
        var result: DataResult<Unit> = DataResult.Success(Unit)
        override fun observeMovieDetails(id: Long): Flow<MovieDetails?> = emptyFlow()
        override suspend fun refreshMovieDetails(id: Long, force: Boolean): DataResult<Unit> {
            calls += id
            return result
        }
    }

    private fun useCase(movies: MovieRepository, token: String = "token") =
        FetchMissingDetailsUseCase(TmdbConfig(token), movies, tvShows, RefreshShowEpisodesUseCase(tvShows, FakeSeasonRepository()))

    private fun TestScope.requestTwice(fetch: FetchMissingDetailsUseCase) {
        fetch.request(this, movieIds = listOf(603L))
        advanceUntilIdle()
        fetch.request(this, movieIds = listOf(603L))
        advanceUntilIdle()
    }

    @Test
    fun `busca cada item uma vez`() = runTest {
        val movies = CountingMovies()
        requestTwice(useCase(movies))
        assertEquals(listOf(603L), movies.calls)
    }

    @Test
    fun `falha de rede libera o item para tentar de novo`() = runTest {
        val movies = CountingMovies().apply { result = DataResult.Failure(DataError.Network) }
        requestTwice(useCase(movies))
        assertEquals(listOf(603L, 603L), movies.calls)
    }

    @Test
    fun `outras falhas nao repetem`() = runTest {
        val movies = CountingMovies().apply { result = DataResult.Failure(DataError.NotFound) }
        requestTwice(useCase(movies))
        assertEquals(listOf(603L), movies.calls)
    }

    @Test
    fun `sem token nao busca`() = runTest {
        val movies = CountingMovies()
        requestTwice(useCase(movies, token = ""))
        assertEquals(emptyList<Long>(), movies.calls)
    }
}
