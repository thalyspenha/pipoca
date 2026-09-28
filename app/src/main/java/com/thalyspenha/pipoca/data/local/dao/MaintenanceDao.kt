package com.thalyspenha.pipoca.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class DatabaseCounts(
    val movies: Int,
    @ColumnInfo(name = "tv_shows") val tvShows: Int,
    @ColumnInfo(name = "watched_episodes") val watchedEpisodes: Int,
    @ColumnInfo(name = "collection_items") val collectionItems: Int,
    @ColumnInfo(name = "history_entries") val historyEntries: Int,
    @ColumnInfo(name = "cached_movies") val cachedMovies: Int,
    @ColumnInfo(name = "cached_tv_shows") val cachedTvShows: Int,
    @ColumnInfo(name = "cached_episodes") val cachedEpisodes: Int,
)

/**
 * Informações do banco e limpeza do cache TMDB (D-057). Um título é "usado" se estiver na biblioteca,
 * na coleção ou no histórico (série também por episódio marcado); só os não usados saem.
 */
@Dao
abstract class MaintenanceDao {

    @Query(
        """SELECT
        (SELECT COUNT(*) FROM user_movie) AS movies,
        (SELECT COUNT(*) FROM user_tv_show) AS tv_shows,
        (SELECT COUNT(*) FROM user_episode) AS watched_episodes,
        (SELECT COUNT(*) FROM collection_item) AS collection_items,
        (SELECT COUNT(*) FROM watch_history) AS history_entries,
        (SELECT COUNT(*) FROM tmdb_movie) AS cached_movies,
        (SELECT COUNT(*) FROM tmdb_tv_show) AS cached_tv_shows,
        (SELECT COUNT(*) FROM tmdb_episode) AS cached_episodes""",
    )
    abstract fun observeCounts(): Flow<DatabaseCounts>

    /** Remove o cache de títulos não usados; devolve (filmes, séries) removidos. */
    @Transaction
    open suspend fun clearUnusedCache(): Pair<Int, Int> {
        val movies = deleteUnusedMovies()
        // Temporadas, episódios e gêneros da série saem em CASCADE.
        val shows = deleteUnusedTvShows()
        deleteOrphanCredits()
        deleteOrphanPersons()
        return movies to shows
    }

    @Query(
        """DELETE FROM tmdb_movie WHERE id NOT IN (SELECT movie_id FROM user_movie)
        AND id NOT IN (SELECT tmdb_id FROM collection_item WHERE media_type = 'MOVIE')
        AND id NOT IN (SELECT movie_id FROM watch_history WHERE movie_id IS NOT NULL)""",
    )
    protected abstract suspend fun deleteUnusedMovies(): Int

    @Query(
        """DELETE FROM tmdb_tv_show WHERE id NOT IN (SELECT show_id FROM user_tv_show)
        AND id NOT IN (SELECT tmdb_id FROM collection_item WHERE media_type = 'TV_SHOW')
        AND id NOT IN (SELECT show_id FROM watch_history WHERE show_id IS NOT NULL)
        AND id NOT IN (SELECT show_id FROM user_episode)""",
    )
    protected abstract suspend fun deleteUnusedTvShows(): Int

    @Query(
        """DELETE FROM tmdb_credit WHERE
        (media_type = 'MOVIE' AND media_id NOT IN (SELECT id FROM tmdb_movie))
        OR (media_type = 'TV' AND media_id NOT IN (SELECT id FROM tmdb_tv_show))""",
    )
    protected abstract suspend fun deleteOrphanCredits()

    @Query("DELETE FROM tmdb_person WHERE id NOT IN (SELECT person_id FROM tmdb_credit)")
    protected abstract suspend fun deleteOrphanPersons()
}
