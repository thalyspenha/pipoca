package com.thalyspenha.pipoca.domain.model

/** Tema escolhido nas Configurações (D-057). */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Versão do app, vinda do `BuildConfig` (injetada pelo `AppModule`). */
data class AppInfo(val versionName: String, val versionCode: Int)

/** Resumo do banco local mostrado nas Configurações. */
data class DatabaseInfo(
    val schemaVersion: Int,
    val movies: Int,
    val tvShows: Int,
    val watchedEpisodes: Int,
    val collectionItems: Int,
    val historyEntries: Int,
    val cachedMovies: Int,
    val cachedTvShows: Int,
    val cachedEpisodes: Int,
    /** Banco + WAL, em bytes. */
    val fileSizeBytes: Long,
)

/** O que "Limpar cache" removeu: títulos do cache TMDB fora da biblioteca, coleção e histórico. */
data class CacheClearResult(val movies: Int, val tvShows: Int)
