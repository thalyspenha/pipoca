package com.thalyspenha.pipoca.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.thalyspenha.pipoca.data.local.entity.UserMovieEntity
import com.thalyspenha.pipoca.data.local.entity.UserTvShowEntity
import com.thalyspenha.pipoca.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Dados pessoais (DATABASE.md). Nunca apagados pelo cache TMDB.
 * Regras (status, validação de nota, histórico ao assistir) ficam no repositório/use cases.
 */
@Dao
interface UserLibraryDao {

    // ---- Filmes

    @Query("SELECT * FROM user_movie ORDER BY updated_at DESC")
    fun observeMovies(): Flow<List<UserMovieEntity>>

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
}
