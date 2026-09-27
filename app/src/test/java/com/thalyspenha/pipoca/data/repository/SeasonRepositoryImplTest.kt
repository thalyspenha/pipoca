package com.thalyspenha.pipoca.data.repository

import com.thalyspenha.pipoca.data.remote.TmdbRemoteDataSource
import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

class SeasonRepositoryImplTest {
    private val api = FakeTmdbApi()
    private val cacheDao = FakeTmdbCacheDao()
    private val episodeDao = FakeTmdbEpisodeDao()
    private val clock = MutableClock()
    private val remote = TmdbRemoteDataSource(api, TmdbConfig("token"))
    private val tvShows = TvShowRepositoryImpl(remote, cacheDao, clock)
    private val repository = SeasonRepositoryImpl(remote, cacheDao, episodeDao, tvShows, clock)

    @Test
    fun `sem a serie em cache busca a serie antes da temporada`() = runBlocking {
        assertEquals(DataResult.Success(Unit), repository.refreshSeason(1396, 1))

        assertEquals(2, api.calls) // tv/1396 + tv/1396/season/1
        val episodes = repository.observeSeasonEpisodes(1396, 1).first()
        assertEquals(listOf(1, 2, 3), episodes.map { it.episodeNumber })
        assertEquals(LocalDate.of(2008, 1, 20), episodes[0].airDate)
        assertEquals(59, episodes[0].runtimeMinutes)
        assertNull(episodes[2].airDate)
        assertNull(episodes[2].runtimeMinutes)
        assertNull(episodes[2].overview)
    }

    @Test
    fun `cache valido nao chama a rede de novo`() = runBlocking {
        repository.refreshSeason(1396, 1)
        clock.advance(Duration.ofDays(20)) // série finalizada: 30 dias

        repository.refreshSeason(1396, 1)

        assertEquals(1, api.seasonCalls)
    }

    @Test
    fun `cache vencido ou force busca de novo`() = runBlocking {
        repository.refreshSeason(1396, 1)
        repository.refreshSeason(1396, 1, force = true)
        clock.advance(Duration.ofDays(31))
        repository.refreshSeason(1396, 1)

        assertEquals(3, api.seasonCalls)
    }

    @Test
    fun `falha de rede devolve erro e nao grava`() = runBlocking {
        api.failWithNetworkError = true

        assertEquals(DataResult.Failure(DataError.Network), repository.refreshSeason(1396, 1))
        assertEquals(emptyList<Any>(), repository.observeSeasonEpisodes(1396, 1).first())
    }

    @Test
    fun `episodios da serie inteira em ordem`() = runBlocking {
        repository.refreshSeason(1396, 1)

        assertEquals(listOf(1 to 1, 1 to 2, 1 to 3), repository.observeShowEpisodes(1396).first().map { it.seasonNumber to it.episodeNumber })
    }
}
