package com.thalyspenha.pipoca.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.thalyspenha.pipoca.data.local.entity.CastRow
import com.thalyspenha.pipoca.data.local.entity.CreditMediaType
import com.thalyspenha.pipoca.data.local.entity.TmdbCreditEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbGenreEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbMovieEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbMovieGenreCrossRef
import com.thalyspenha.pipoca.data.local.entity.TmdbPersonEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbSeasonEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbTvShowEntity
import com.thalyspenha.pipoca.data.local.entity.TmdbTvShowGenreCrossRef
import kotlinx.coroutines.flow.Flow

/** Tudo que o filme precisa para ser gravado de uma vez. */
data class MovieCacheBundle(
    val movie: TmdbMovieEntity,
    val genres: List<TmdbGenreEntity>,
    val persons: List<TmdbPersonEntity>,
    val credits: List<TmdbCreditEntity>,
)

data class TvShowCacheBundle(
    val show: TmdbTvShowEntity,
    val genres: List<TmdbGenreEntity>,
    val seasons: List<TmdbSeasonEntity>,
    val persons: List<TmdbPersonEntity>,
    val credits: List<TmdbCreditEntity>,
)

data class TvShowCacheInfo(
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
    @ColumnInfo(name = "tmdb_status") val tmdbStatus: String?,
)

/**
 * Cache TMDB (DATABASE.md). Nunca toca em dados pessoais.
 * Upsert (e não REPLACE) para não disparar CASCADE nas tabelas filhas.
 */
@Dao
abstract class TmdbCacheDao {

    // ---- Filmes

    @Query("SELECT * FROM tmdb_movie WHERE id = :id")
    abstract fun observeMovie(id: Long): Flow<TmdbMovieEntity?>

    @Query("SELECT fetched_at FROM tmdb_movie WHERE id = :id")
    abstract suspend fun getMovieFetchedAt(id: Long): Long?

    @Query(
        """SELECT g.* FROM tmdb_genre g
        INNER JOIN tmdb_movie_genre mg ON mg.genre_id = g.id
        WHERE mg.movie_id = :movieId ORDER BY g.name""",
    )
    abstract suspend fun getMovieGenres(movieId: Long): List<TmdbGenreEntity>

    @Transaction
    open suspend fun saveMovie(bundle: MovieCacheBundle) {
        upsertMovie(bundle.movie)
        upsertGenres(bundle.genres)
        deleteMovieGenres(bundle.movie.id)
        insertMovieGenres(bundle.genres.map { TmdbMovieGenreCrossRef(bundle.movie.id, it.id) })
        replaceCredits(CreditMediaType.MOVIE, bundle.movie.id, bundle.persons, bundle.credits)
    }

    @Upsert
    protected abstract suspend fun upsertMovie(movie: TmdbMovieEntity)

    @Query("DELETE FROM tmdb_movie_genre WHERE movie_id = :movieId")
    protected abstract suspend fun deleteMovieGenres(movieId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertMovieGenres(refs: List<TmdbMovieGenreCrossRef>)

    // ---- Séries

    @Query("SELECT * FROM tmdb_tv_show WHERE id = :id")
    abstract fun observeTvShow(id: Long): Flow<TmdbTvShowEntity?>

    @Query("SELECT fetched_at, tmdb_status FROM tmdb_tv_show WHERE id = :id")
    abstract suspend fun getTvShowCacheInfo(id: Long): TvShowCacheInfo?

    @Query(
        """SELECT g.* FROM tmdb_genre g
        INNER JOIN tmdb_tv_show_genre sg ON sg.genre_id = g.id
        WHERE sg.show_id = :showId ORDER BY g.name""",
    )
    abstract suspend fun getTvShowGenres(showId: Long): List<TmdbGenreEntity>

    @Query("SELECT * FROM tmdb_season WHERE show_id = :showId ORDER BY season_number")
    abstract suspend fun getSeasons(showId: Long): List<TmdbSeasonEntity>

    @Transaction
    open suspend fun saveTvShow(bundle: TvShowCacheBundle) {
        val showId = bundle.show.id
        upsertTvShow(bundle.show)
        upsertGenres(bundle.genres)
        deleteTvShowGenres(showId)
        insertTvShowGenres(bundle.genres.map { TmdbTvShowGenreCrossRef(showId, it.id) })
        // Remove temporadas que sumiram antes do upsert, evitando conflito no índice (show_id, season_number).
        deleteSeasonsNotIn(showId, bundle.seasons.map { it.id })
        upsertSeasons(bundle.seasons)
        replaceCredits(CreditMediaType.TV, showId, bundle.persons, bundle.credits)
    }

    @Upsert
    protected abstract suspend fun upsertTvShow(show: TmdbTvShowEntity)

    @Query("DELETE FROM tmdb_tv_show_genre WHERE show_id = :showId")
    protected abstract suspend fun deleteTvShowGenres(showId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertTvShowGenres(refs: List<TmdbTvShowGenreCrossRef>)

    @Query("DELETE FROM tmdb_season WHERE show_id = :showId AND id NOT IN (:keepIds)")
    protected abstract suspend fun deleteSeasonsNotIn(showId: Long, keepIds: List<Long>)

    @Upsert
    protected abstract suspend fun upsertSeasons(seasons: List<TmdbSeasonEntity>)

    // ---- Compartilhado

    @Query(
        """SELECT p.id AS id, p.name AS name, p.profile_path AS profilePath,
        c.character AS character, c.`order` AS `order`
        FROM tmdb_credit c INNER JOIN tmdb_person p ON p.id = c.person_id
        WHERE c.media_type = :mediaType AND c.media_id = :mediaId
        ORDER BY c.`order`""",
    )
    abstract suspend fun getCast(mediaType: CreditMediaType, mediaId: Long): List<CastRow>

    @Upsert
    protected abstract suspend fun upsertGenres(genres: List<TmdbGenreEntity>)

    @Upsert
    protected abstract suspend fun upsertPersons(persons: List<TmdbPersonEntity>)

    @Query("DELETE FROM tmdb_credit WHERE media_type = :mediaType AND media_id = :mediaId")
    protected abstract suspend fun deleteCredits(mediaType: CreditMediaType, mediaId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertCredits(credits: List<TmdbCreditEntity>)

    private suspend fun replaceCredits(
        mediaType: CreditMediaType,
        mediaId: Long,
        persons: List<TmdbPersonEntity>,
        credits: List<TmdbCreditEntity>,
    ) {
        upsertPersons(persons)
        deleteCredits(mediaType, mediaId)
        insertCredits(credits)
    }
}
