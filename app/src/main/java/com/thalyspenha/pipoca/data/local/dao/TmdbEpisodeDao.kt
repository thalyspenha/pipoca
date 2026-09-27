package com.thalyspenha.pipoca.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.thalyspenha.pipoca.data.local.entity.TmdbEpisodeEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbSeasonEntity
import kotlinx.coroutines.flow.Flow

/** Episódios no cache TMDB. Separado do [TmdbCacheDao] para não inflar os fakes dos testes de detalhes. */
@Dao
interface TmdbEpisodeDao {

    @Query("SELECT * FROM tmdb_episode WHERE show_id = :showId AND season_number = :seasonNumber ORDER BY episode_number")
    fun observeSeasonEpisodes(showId: Long, seasonNumber: Int): Flow<List<TmdbEpisodeEntity>>

    /** Todos os episódios em cache da série (base do progresso). */
    @Query("SELECT * FROM tmdb_episode WHERE show_id = :showId ORDER BY season_number, episode_number")
    fun observeShowEpisodes(showId: Long): Flow<List<TmdbEpisodeEntity>>

    /** Episódios em cache de todas as séries `WATCHING` numa consulta só (Home, D-044). */
    @Query(
        """SELECT e.* FROM tmdb_episode e
        JOIN user_tv_show u ON u.show_id = e.show_id
        WHERE u.status = 'WATCHING'
        ORDER BY e.show_id, e.season_number, e.episode_number""",
    )
    fun observeWatchingShowsEpisodes(): Flow<List<TmdbEpisodeEntity>>

    /** Temporadas das séries `WATCHING` (para saber se o cache de episódios está completo). */
    @Query(
        """SELECT s.* FROM tmdb_season s
        JOIN user_tv_show u ON u.show_id = s.show_id
        WHERE u.status = 'WATCHING'""",
    )
    fun observeWatchingShowsSeasons(): Flow<List<TmdbSeasonEntity>>

    /** Busca mais antiga da temporada; nulo = temporada sem episódios em cache. */
    @Query("SELECT MIN(fetched_at) FROM tmdb_episode WHERE show_id = :showId AND season_number = :seasonNumber")
    suspend fun getSeasonEpisodesFetchedAt(showId: Long, seasonNumber: Int): Long?

    @Upsert
    suspend fun upsertSeason(season: TmdbSeasonEntity)

    @Upsert
    suspend fun upsertEpisodes(episodes: List<TmdbEpisodeEntity>)

    @Query("DELETE FROM tmdb_episode WHERE season_id = :seasonId AND id NOT IN (:keepIds)")
    suspend fun deleteEpisodesNotIn(seasonId: Long, keepIds: List<Long>)

    /**
     * Grava a temporada e substitui seus episódios. Upsert (não REPLACE) na temporada para não
     * disparar o CASCADE; episódios que sumiram saem antes, evitando conflito no índice único.
     */
    @Transaction
    suspend fun saveSeason(season: TmdbSeasonEntity, episodes: List<TmdbEpisodeEntity>) {
        upsertSeason(season)
        deleteEpisodesNotIn(season.id, episodes.map { it.id })
        upsertEpisodes(episodes)
    }
}
