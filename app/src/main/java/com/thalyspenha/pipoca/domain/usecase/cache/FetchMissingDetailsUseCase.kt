package com.thalyspenha.pipoca.domain.usecase.cache

import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import com.thalyspenha.pipoca.domain.repository.MovieRepository
import com.thalyspenha.pipoca.domain.repository.TvShowRepository
import com.thalyspenha.pipoca.domain.usecase.episodes.RefreshShowEpisodesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Busca no TMDB o que falta no cache para exibir listas (título/pôster, temporadas), uma vez por
 * item enquanto a tela vive (D-030, D-055). Falha de rede libera o item: a próxima emissão da lista
 * (qualquer mudança no banco) tenta de novo; outras falhas (404, token) não repetem.
 * Sem token, não faz nada. Uma instância por ViewModel (sem escopo no Hilt); usar na thread principal.
 */
class FetchMissingDetailsUseCase @Inject constructor(
    private val tmdbConfig: TmdbConfig,
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository,
    private val refreshShowEpisodes: RefreshShowEpisodesUseCase,
) {
    private enum class Kind { MOVIE, TV_SHOW, EPISODES }

    private val requested = mutableSetOf<Pair<Kind, Long>>()

    fun request(
        scope: CoroutineScope,
        movieIds: Collection<Long> = emptyList(),
        tvShowIds: Collection<Long> = emptyList(),
        episodesOfShowIds: Collection<Long> = emptyList(),
    ) {
        if (!tmdbConfig.isConfigured) return
        launchNew(scope, Kind.MOVIE, movieIds) { movieRepository.refreshMovieDetails(it) }
        launchNew(scope, Kind.TV_SHOW, tvShowIds) { tvShowRepository.refreshTvShowDetails(it) }
        launchNew(scope, Kind.EPISODES, episodesOfShowIds) { refreshShowEpisodes(it) }
    }

    private fun launchNew(
        scope: CoroutineScope,
        kind: Kind,
        ids: Collection<Long>,
        fetch: suspend (Long) -> DataResult<Unit>,
    ) {
        ids.filter { requested.add(kind to it) }.forEach { id ->
            scope.launch {
                val result = fetch(id)
                if ((result as? DataResult.Failure)?.error == DataError.Network) requested.remove(kind to id)
            }
        }
    }
}
