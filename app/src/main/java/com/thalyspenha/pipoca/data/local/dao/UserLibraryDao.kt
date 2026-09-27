package com.thalyspenha.pipoca.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.thalyspenha.pipoca.data.local.entity.UserEpisodeEntity
import com.thalyspenha.pipoca.data.local.entity.UserMovieEntity
import com.thalyspenha.pipoca.data.local.entity.UserTvShowEntity
import com.thalyspenha.pipoca.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Filme da biblioteca com o que o cache TMDB tiver (LEFT JOIN: colunas nulas se não houver cache). */
data class UserMovieWithCache(
    @Embedded val movie: UserMovieEntity,
    val title: String?,
    @ColumnInfo(name = "poster_path") val posterPath: String?,
    @ColumnInfo(name = "release_date") val releaseDate: LocalDate?,
)

data class UserTvShowWithCache(
    @Embedded val show: UserTvShowEntity,
    val name: String?,
    @ColumnInfo(name = "poster_path") val posterPath: String?,
    @ColumnInfo(name = "first_air_date") val firstAirDate: LocalDate?,
    @ColumnInfo(name = "tmdb_status") val tmdbStatus: String?,
)

/**
 * Dados pessoais (DATABASE.md). Nunca apagados pelo cache TMDB.
 * Regras (status, validação de nota, histórico ao assistir) ficam no repositório/use cases.
 */
@Dao
interface UserLibraryDao {

    // ---- Filmes

    @Query("SELECT * FROM user_movie ORDER BY updated_at DESC")
    fun observeMovies(): Flow<List<UserMovieEntity>>

    @Query(
        """SELECT u.*, m.title, m.poster_path, m.release_date FROM user_movie u
        LEFT JOIN tmdb_movie m ON m.id = u.movie_id
        ORDER BY u.updated_at DESC""",
    )
    fun observeMoviesWithCache(): Flow<List<UserMovieWithCache>>

    @Query("SELECT * FROM user_movie WHERE movie_id = :movieId")
    fun observeMovie(movieId: Long): Flow<UserMovieEntity?>

    @Query("SELECT * FROM user_movie WHERE movie_id = :movieId")
    suspend fun getMovie(movieId: Long): UserMovieEntity?

    @Upsert
    suspend fun upsertMovie(movie: UserMovieEntity)

    @Query("DELETE FROM user_movie WHERE movie_id = :movieId")
    suspend fun deleteMovie(movieId: Long)

    /** Grava o filme e registra a visualização juntos. */
    @Transaction
    suspend fun upsertMovieWithWatch(movie: UserMovieEntity, event: WatchHistoryEntity) {
        upsertMovie(movie)
        insertWatch(event)
    }

    /** Remove o filme e todo o histórico dele juntos (D-029). */
    @Transaction
    suspend fun deleteMovieWithHistory(movieId: Long) {
        deleteMovie(movieId)
        deleteMovieHistory(movieId)
    }

    // ---- Séries

    @Query("SELECT * FROM user_tv_show ORDER BY updated_at DESC")
    fun observeTvShows(): Flow<List<UserTvShowEntity>>

    @Query(
        """SELECT u.*, t.name, t.poster_path, t.first_air_date, t.tmdb_status FROM user_tv_show u
        LEFT JOIN tmdb_tv_show t ON t.id = u.show_id
        ORDER BY u.updated_at DESC""",
    )
    fun observeTvShowsWithCache(): Flow<List<UserTvShowWithCache>>

    @Query("SELECT * FROM user_tv_show WHERE show_id = :showId")
    fun observeTvShow(showId: Long): Flow<UserTvShowEntity?>

    @Query("SELECT * FROM user_tv_show WHERE show_id = :showId")
    suspend fun getTvShow(showId: Long): UserTvShowEntity?

    @Upsert
    suspend fun upsertTvShow(show: UserTvShowEntity)

    @Query("DELETE FROM user_tv_show WHERE show_id = :showId")
    suspend fun deleteTvShow(showId: Long)

    // ---- Histórico

    @Insert
    suspend fun insertWatch(event: WatchHistoryEntity): Long

    @Query("SELECT * FROM watch_history WHERE movie_id = :movieId ORDER BY watched_at DESC")
    fun observeMovieHistory(movieId: Long): Flow<List<WatchHistoryEntity>>

    @Query("DELETE FROM watch_history WHERE movie_id = :movieId")
    suspend fun deleteMovieHistory(movieId: Long)

    // ---- Episódios assistidos

    @Query("SELECT * FROM user_episode WHERE show_id = :showId ORDER BY season_number, episode_number")
    fun observeWatchedEpisodes(showId: Long): Flow<List<UserEpisodeEntity>>

    @Upsert
    suspend fun upsertWatchedEpisodes(episodes: List<UserEpisodeEntity>)

    @Query("DELETE FROM user_episode WHERE episode_id IN (:episodeIds)")
    suspend fun deleteWatchedEpisodes(episodeIds: List<Long>)

    /** Assistidos de todas as séries `WATCHING` numa consulta só (Home, D-044). */
    @Query(
        """SELECT e.* FROM user_episode e
        JOIN user_tv_show u ON u.show_id = e.show_id
        WHERE u.status = 'WATCHING'""",
    )
    fun observeWatchingShowsWatchedEpisodes(): Flow<List<UserEpisodeEntity>>

    @Insert
    suspend fun insertWatches(events: List<WatchHistoryEntity>)

    @Query("DELETE FROM watch_history WHERE episode_id IN (:episodeIds)")
    suspend fun deleteEpisodeHistory(episodeIds: List<Long>)

    @Query("DELETE FROM user_episode WHERE show_id = :showId")
    suspend fun deleteShowWatchedEpisodes(showId: Long)

    @Query("DELETE FROM watch_history WHERE show_id = :showId")
    suspend fun deleteShowHistory(showId: Long)

    /** Marca episódios e registra um evento de histórico para cada, juntos. */
    @Transaction
    suspend fun insertWatchedEpisodesWithHistory(episodes: List<UserEpisodeEntity>, events: List<WatchHistoryEntity>) {
        upsertWatchedEpisodes(episodes)
        insertWatches(events)
    }

    /** Desmarcar remove também o evento correspondente (DATABASE.md). */
    @Transaction
    suspend fun deleteWatchedEpisodesWithHistory(episodeIds: List<Long>) {
        deleteWatchedEpisodes(episodeIds)
        deleteEpisodeHistory(episodeIds)
    }

    /** Remove a série, os episódios assistidos e o histórico dela juntos (D-037, como filmes em D-029). */
    @Transaction
    suspend fun deleteTvShowWithEpisodes(showId: Long) {
        deleteTvShow(showId)
        deleteShowWatchedEpisodes(showId)
        deleteShowHistory(showId)
    }
}
