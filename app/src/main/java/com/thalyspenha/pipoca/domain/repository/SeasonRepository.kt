package com.thalyspenha.pipoca.domain.repository

import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.domain.model.SeasonSummary
import kotlinx.coroutines.flow.Flow

/** Episódios por temporada, com cache no Room (D-003, D-036). */
interface SeasonRepository {
    /** Episódios em cache da temporada, em ordem. Lista vazia enquanto não houver cache. */
    fun observeSeasonEpisodes(showId: Long, seasonNumber: Int): Flow<List<Episode>>

    /** Todos os episódios em cache da série (todas as temporadas já baixadas). */
    fun observeShowEpisodes(showId: Long): Flow<List<Episode>>

    /** Episódios em cache de todas as séries `WATCHING`, numa consulta (D-044). */
    fun observeWatchingShowsEpisodes(): Flow<List<Episode>>

    /** Temporadas (resumo) das séries `WATCHING`, por série. */
    fun observeWatchingShowsSeasons(): Flow<Map<Long, List<SeasonSummary>>>

    /** Episódios em cache de todas as séries da biblioteca (D-050). */
    fun observeLibraryShowsEpisodes(): Flow<List<Episode>>

    fun observeLibraryShowsSeasons(): Flow<Map<Long, List<SeasonSummary>>>

    /** Busca `tv/{id}/season/{n}` se não houver cache válido (ou se [force]) e grava no Room. */
    suspend fun refreshSeason(showId: Long, seasonNumber: Int, force: Boolean = false): DataResult<Unit>
}
