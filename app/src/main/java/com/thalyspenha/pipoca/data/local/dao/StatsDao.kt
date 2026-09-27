package com.thalyspenha.pipoca.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import com.thalyspenha.pipoca.domain.model.MediaFormat
import kotlinx.coroutines.flow.Flow

data class MovieCountsRow(
    val watched: Int,
    @ColumnInfo(name = "want_to_watch") val wantToWatch: Int,
    val favorites: Int,
    @ColumnInfo(name = "in_collection") val inCollection: Int,
)

data class TvShowCountsRow(
    val total: Int,
    val watching: Int,
    val completed: Int,
    @ColumnInfo(name = "want_to_watch") val wantToWatch: Int,
)

data class EpisodeCountsRow(
    val total: Int,
    @ColumnInfo(name = "this_month") val thisMonth: Int,
    @ColumnInfo(name = "this_year") val thisYear: Int,
)

data class WatchTimeRow(
    val views: Int,
    val minutes: Long,
    @ColumnInfo(name = "without_runtime") val withoutRuntime: Int,
)

data class GenreCountRow(val name: String, val count: Int)

data class RatingCountRow(val rating: Int, val count: Int)

data class FormatCountRow(val format: MediaFormat, val count: Int)

/**
 * Estatísticas por consultas agregadas (D-047, D-048): cada bloco é uma consulta, sem tabela nova
 * nem dado duplicado. `COALESCE(SUM(...), 0)` porque `SUM` de tabela vazia é nulo.
 */
@Dao
interface StatsDao {

    @Query(
        """SELECT
            COALESCE(SUM(status = 'WATCHED'), 0) AS watched,
            COALESCE(SUM(status = 'WANT_TO_WATCH'), 0) AS want_to_watch,
            COALESCE(SUM(is_favorite), 0) AS favorites,
            (SELECT COUNT(DISTINCT tmdb_id) FROM collection_item WHERE media_type = 'MOVIE') AS in_collection
        FROM user_movie""",
    )
    fun observeMovieCounts(): Flow<MovieCountsRow>

    @Query(
        """SELECT
            COUNT(*) AS total,
            COALESCE(SUM(status = 'WATCHING'), 0) AS watching,
            COALESCE(SUM(status = 'COMPLETED'), 0) AS completed,
            COALESCE(SUM(status = 'WANT_TO_WATCH'), 0) AS want_to_watch
        FROM user_tv_show""",
    )
    fun observeTvShowCounts(): Flow<TvShowCountsRow>

    /** Episódios distintos assistidos; mês/ano pela data em que foram marcados. */
    @Query(
        """SELECT
            COUNT(*) AS total,
            COALESCE(SUM(watched_at >= :monthStart), 0) AS this_month,
            COALESCE(SUM(watched_at >= :yearStart), 0) AS this_year
        FROM user_episode""",
    )
    fun observeEpisodeCounts(monthStart: Long, yearStart: Long): Flow<EpisodeCountsRow>

    /**
     * Tempo por visualização (`watch_history`: reassistir conta). Filme: `runtime_minutes`;
     * episódio: duração do episódio ou média da série. Sem duração (ou sem cache) conta à parte.
     */
    @Query(
        """SELECT
            COUNT(*) AS views,
            COALESCE(SUM(duration), 0) AS minutes,
            COALESCE(SUM(duration IS NULL), 0) AS without_runtime
        FROM (
            SELECT CASE WHEN h.media_type = 'MOVIE' THEN m.runtime_minutes
                        ELSE COALESCE(e.runtime_minutes, t.episode_run_time) END AS duration
            FROM watch_history h
            LEFT JOIN tmdb_movie m ON h.media_type = 'MOVIE' AND m.id = h.movie_id
            LEFT JOIN tmdb_episode e ON e.id = h.episode_id
            LEFT JOIN tmdb_tv_show t ON h.media_type = 'EPISODE' AND t.id = h.show_id
        )""",
    )
    fun observeWatchTime(): Flow<WatchTimeRow>

    /**
     * Gêneros do que foi assistido (opção a, D-047): filmes `WATCHED` e séries `COMPLETED` ou com
     * ao menos um episódio assistido; cada título conta uma vez por gênero. Sem cache: fica fora.
     */
    @Query(
        """SELECT g.name AS name, COUNT(*) AS count
        FROM (
            SELECT mg.genre_id AS genre_id FROM tmdb_movie_genre mg
            JOIN user_movie u ON u.movie_id = mg.movie_id
            WHERE u.status = 'WATCHED'
            UNION ALL
            SELECT sg.genre_id AS genre_id FROM tmdb_tv_show_genre sg
            JOIN user_tv_show u ON u.show_id = sg.show_id
            WHERE u.status = 'COMPLETED'
                OR EXISTS (SELECT 1 FROM user_episode ue WHERE ue.show_id = u.show_id)
        ) x
        JOIN tmdb_genre g ON g.id = x.genre_id
        GROUP BY g.id
        ORDER BY count DESC, g.name""",
    )
    fun observeGenreCounts(): Flow<List<GenreCountRow>>

    /** Notas pessoais de filmes e séries juntas. */
    @Query(
        """SELECT rating, COUNT(*) AS count FROM (
            SELECT rating FROM user_movie WHERE rating IS NOT NULL
            UNION ALL
            SELECT rating FROM user_tv_show WHERE rating IS NOT NULL
        ) GROUP BY rating""",
    )
    fun observeRatingCounts(): Flow<List<RatingCountRow>>

    /** Itens da coleção por formato. */
    @Query("SELECT format, COUNT(*) AS count FROM collection_item GROUP BY format")
    fun observeFormatCounts(): Flow<List<FormatCountRow>>
}
