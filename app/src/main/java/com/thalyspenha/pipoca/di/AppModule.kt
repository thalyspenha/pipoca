package com.thalyspenha.pipoca.di

import com.thalyspenha.pipoca.BuildConfig
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Único ponto que lê BuildConfig; o resto do app recebe TmdbConfig injetado.
    @Provides
    @Singleton
    fun provideTmdbConfig(): TmdbConfig = TmdbConfig(apiToken = BuildConfig.TMDB_API_TOKEN)

    /** Relógio injetável: validade do cache testável sem depender da hora real. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}
