package com.thalyspenha.pipoca.data.repository

import com.thalyspenha.pipoca.data.remote.TmdbRemoteDataSource
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class TvShowRepositoryImplTest {
    private val api = FakeTmdbApi()
    private val dao = FakeTmdbCacheDao()
    private val clock = MutableClock()
    private val repository = TvShowRepositoryImpl(TmdbRemoteDataSource(api, TmdbConfig("token")), dao, clock)

    @Test
    fun `grava e le serie com temporadas e elenco`() = runBlocking {
        assertEquals(DataResult.Success(Unit), repository.refreshTvShowDetails(1396))

        val show = repository.observeTvShowDetails(1396).first()!!
        assertEquals("Breaking Bad", show.name)
        assertEquals(listOf(0, 1), show.seasons.map { it.seasonNumber })
        assertEquals(show.cast.sortedBy { it.order }, show.cast)
    }

    @Test
    fun `serie encerrada fica 30 dias sem nova chamada`() = runBlocking {
        repository.refreshTvShowDetails(1396) // fixture: status "Ended"
        clock.advance(Duration.ofDays(20))

        repository.refreshTvShowDetails(1396)

        assertEquals(1, api.calls)
    }

    @Test
    fun `serie em exibicao vence em 1 dia`() = runBlocking {
        api.tvShowStatus = "Returning Series"
        repository.refreshTvShowDetails(1396)
        clock.advance(Duration.ofDays(2))

        repository.refreshTvShowDetails(1396)

        assertEquals(2, api.calls)
    }
}
