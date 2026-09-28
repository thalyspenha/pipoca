package com.thalyspenha.pipoca.di

import com.thalyspenha.pipoca.BuildConfig
import com.thalyspenha.pipoca.domain.model.AppInfo
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Único ponto que lê BuildConfig; o resto do app recebe TmdbConfig injetado.
    @Provides
    @Singleton
    fun provideTmdbConfig(): TmdbConfig = TmdbConfig(apiToken = BuildConfig.TMDB_API_TOKEN)

    @Provides
    @Singleton
    fun provideAppInfo(): AppInfo = AppInfo(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

    /** Relógio injetável: validade do cache testável sem depender da hora real. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    /** Fuso do aparelho para agrupar por dia/mês/ano (estatísticas). */
    @Provides
    fun provideZoneId(): ZoneId = ZoneId.systemDefault()
}
