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

/** Evento do histórico com o que o cache tiver (título/pôster/episódio); nulos sem cache (D-046). */
data class WatchHistoryWithCache(
    @Embedded val event: WatchHistoryEntity,
    @ColumnInfo(name = "movie_title") val movieTitle: String?,
    @ColumnInfo(name = "show_name") val showName: String?,
    @ColumnInfo(name = "poster_path") val posterPath: String?,
    @ColumnInfo(name = "episode_name") val episodeName: String?,
    @ColumnInfo(name = "ep_season_number") val seasonNumber: Int?,
    @ColumnInfo(name = "ep_episode_number") val episodeNumber: Int?,
)

data class UserTvShowWithCache(
    @Embedded val show: UserTvShowEntity,
    val name: String?,
    @ColumnInfo(name = "poster_path") val posterPath: String?,
    @ColumnInfo(name = "first_air_date") val firstAirDate: LocalDate?,
    @ColumnInfo(name = "tmdb_status") val tmdbStatus: String?,
    @ColumnInfo(name = "last_watched_at") val lastWatchedAt: Long? = null,
)

data class MovieLibraryCountsRow(
    val all: Int,
    @ColumnInfo(name = "want_to_watch") val wantToWatch: Int,
    val watched: Int,
    val favorites: Int,
)

data class TvShowLibraryCountsRow(
    val all: Int,
    @ColumnInfo(name = "want_to_watch") val wantToWatch: Int,
    val watching: Int,
    val completed: Int,
    val paused: Int,
    val dropped: Int,
    val favorites: Int,
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
        """SELECT u.*, t.name, t.poster_path, t.first_air_date, t.tmdb_status, NULL AS last_watched_at FROM user_tv_show u
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

    /**
     * Histórico completo, mais recente primeiro. Temporada/número do episódio vêm do cache ou,
     * sem cache, de `user_episode` (redundância de propósito, DATABASE.md).
     */
    @Query(
        """SELECT h.*,
            m.title AS movie_title,
            t.name AS show_name,
            COALESCE(m.poster_path, t.poster_path) AS poster_path,
            e.name AS episode_name,
            COALESCE(e.season_number, ue.season_number) AS ep_season_number,
            COALESCE(e.episode_number, ue.episode_number) AS ep_episode_number
        FROM watch_history h
        LEFT JOIN tmdb_movie m ON h.media_type = 'MOVIE' AND m.id = h.movie_id
        LEFT JOIN tmdb_tv_show t ON h.media_type = 'EPISODE' AND t.id = h.show_id
        LEFT JOIN tmdb_episode e ON e.id = h.episode_id
        LEFT JOIN user_episode ue ON ue.episode_id = h.episode_id
        ORDER BY h.watched_at DESC, h.id DESC""",
    )
    fun observeHistory(): Flow<List<WatchHistoryWithCache>>

    @Query("DELETE FROM watch_history WHERE movie_id = :movieId")
    suspend fun deleteMovieHistory(movieId: Long)

    // ---- Episódios assistidos

    @Query("SELECT * FROM user_episode WHERE show_id = :showId ORDER BY season_number, episode_number")
    fun observeWatchedEpisodes(showId: Long): Flow<List<UserEpisodeEntity>>

    @Upsert
    suspend fun upsertWatchedEpisodes(episodes: List<UserEpisodeEntity>)

    @Query("DELETE FROM user_episode WHERE episode_id IN (:episodeIds)")
    suspend fun deleteWatchedEpisodes(episodeIds: List<Long>)

    // ---- Biblioteca (D-050): filtro e ordenação no SQL

    /**
     * Filmes filtrados e ordenados no banco. `status` nulo = todos. Título ordena sem diferenciar
     * caixa (acentos seguem a ordem binária do SQLite, limitação documentada). Sem cache, título/ano
     * nulos vão para o fim.
     */
    @Query(
        """SELECT u.*, m.title, m.poster_path, m.release_date FROM user_movie u
        LEFT JOIN tmdb_movie m ON m.id = u.movie_id
        WHERE (:status IS NULL OR u.status = :status) AND (:favoritesOnly = 0 OR u.is_favorite = 1)
        ORDER BY
            CASE WHEN :sort = 'RECENTLY_ADDED' THEN u.added_at END DESC,
            CASE WHEN :sort IN ('TITLE_ASC', 'TITLE_DESC') THEN m.title IS NULL END,
            CASE WHEN :sort = 'TITLE_ASC' THEN m.title END COLLATE NOCASE ASC,
            CASE WHEN :sort = 'TITLE_DESC' THEN m.title END COLLATE NOCASE DESC,
            CASE WHEN :sort = 'RELEASE_YEAR' THEN m.release_date IS NULL END,
            CASE WHEN :sort = 'RELEASE_YEAR' THEN m.release_date END DESC,
            CASE WHEN :sort = 'LAST_ACTIVITY' THEN u.updated_at END DESC,
            CASE WHEN :sort = 'RATING' THEN u.rating IS NULL END,
            CASE WHEN :sort = 'RATING' THEN u.rating END DESC,
            u.updated_at DESC""",
    )
    fun observeMovieList(status: String?, favoritesOnly: Boolean, sort: String): Flow<List<UserMovieWithCache>>

    /** Séries filtradas e ordenadas no banco; `last_watched_at` = último episódio marcado. */
    @Query(
        """SELECT u.*, t.name, t.poster_path, t.first_air_date, t.tmdb_status,
            (SELECT MAX(ue.watched_at) FROM user_episode ue WHERE ue.show_id = u.show_id) AS last_watched_at
        FROM user_tv_show u
        LEFT JOIN tmdb_tv_show t ON t.id = u.show_id
        WHERE (:status IS NULL OR u.status = :status) AND (:favoritesOnly = 0 OR u.is_favorite = 1)
        ORDER BY
            CASE WHEN :sort = 'RECENTLY_ADDED' THEN u.added_at END DESC,
            CASE WHEN :sort IN ('TITLE_ASC', 'TITLE_DESC') THEN t.name IS NULL END,
            CASE WHEN :sort = 'TITLE_ASC' THEN t.name END COLLATE NOCASE ASC,
            CASE WHEN :sort = 'TITLE_DESC' THEN t.name END COLLATE NOCASE DESC,
            CASE WHEN :sort = 'RELEASE_YEAR' THEN t.first_air_date IS NULL END,
            CASE WHEN :sort = 'RELEASE_YEAR' THEN t.first_air_date END DESC,
            CASE WHEN :sort = 'LAST_ACTIVITY' THEN u.updated_at END DESC,
            CASE WHEN :sort = 'RATING' THEN u.rating IS NULL END,
            CASE WHEN :sort = 'RATING' THEN u.rating END DESC,
            CASE WHEN :sort = 'LAST_EPISODE' THEN last_watched_at IS NULL END,
            CASE WHEN :sort = 'LAST_EPISODE' THEN last_watched_at END DESC,
            u.updated_at DESC""",
    )
    fun observeTvShowList(status: String?, favoritesOnly: Boolean, sort: String): Flow<List<UserTvShowWithCache>>

    @Query(
        """SELECT COUNT(*) AS `all`,
            COALESCE(SUM(status = 'WANT_TO_WATCH'), 0) AS want_to_watch,
            COALESCE(SUM(status = 'WATCHED'), 0) AS watched,
            COALESCE(SUM(is_favorite), 0) AS favorites
        FROM user_movie""",
    )
    fun observeMovieLibraryCounts(): Flow<MovieLibraryCountsRow>

    @Query(
        """SELECT COUNT(*) AS `all`,
            COALESCE(SUM(status = 'WANT_TO_WATCH'), 0) AS want_to_watch,
            COALESCE(SUM(status = 'WATCHING'), 0) AS watching,
            COALESCE(SUM(status = 'COMPLETED'), 0) AS completed,
            COALESCE(SUM(status = 'PAUSED'), 0) AS paused,
            COALESCE(SUM(status = 'DROPPED'), 0) AS dropped,
            COALESCE(SUM(is_favorite), 0) AS favorites
        FROM user_tv_show""",
    )
    fun observeTvShowLibraryCounts(): Flow<TvShowLibraryCountsRow>

    /** Assistidos de todas as séries da biblioteca (progresso nos cards, D-050). */
    @Query(
        """SELECT e.* FROM user_episode e
        JOIN user_tv_show u ON u.show_id = e.show_id""",
    )
    fun observeLibraryShowsWatchedEpisodes(): Flow<List<UserEpisodeEntity>>

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

    @Query("SELECT episode_id FROM user_episode WHERE episode_id IN (:episodeIds)")
    suspend fun getWatchedEpisodeIds(episodeIds: List<Long>): List<Long>

    /**
     * Marca episódios e registra um evento de histórico para cada, juntos. Os já marcados são ignorados
     * dentro da transação: toque duplo não duplica o histórico nem troca a data original.
     */
    @Transaction
    suspend fun insertWatchedEpisodesWithHistory(episodes: List<UserEpisodeEntity>, events: List<WatchHistoryEntity>) {
        val already = getWatchedEpisodeIds(episodes.map { it.episodeId }).toSet()
        if (already.size == episodes.size) return
        upsertWatchedEpisodes(episodes.filter { it.episodeId !in already })
        insertWatches(events.filter { it.episodeId !in already })
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
