package com.thalyspenha.pipoca.di

import com.thalyspenha.pipoca.data.repository.CollectionRepositoryImpl
import com.thalyspenha.pipoca.data.repository.LibraryRepositoryImpl
import com.thalyspenha.pipoca.data.repository.MovieRepositoryImpl
import com.thalyspenha.pipoca.data.repository.SearchRepositoryImpl
import com.thalyspenha.pipoca.data.repository.SeasonRepositoryImpl
import com.thalyspenha.pipoca.data.repository.TvShowRepositoryImpl
import com.thalyspenha.pipoca.domain.repository.CollectionRepository
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import com.thalyspenha.pipoca.domain.repository.MovieRepository
import com.thalyspenha.pipoca.domain.repository.SearchRepository
import com.thalyspenha.pipoca.domain.repository.SeasonRepository
import com.thalyspenha.pipoca.domain.repository.TvShowRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    @Binds
    @Singleton
    abstract fun bindTvShowRepository(impl: TvShowRepositoryImpl): TvShowRepository

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindSeasonRepository(impl: SeasonRepositoryImpl): SeasonRepository

    @Binds
    @Singleton
    abstract fun bindCollectionRepository(impl: CollectionRepositoryImpl): CollectionRepository
}
