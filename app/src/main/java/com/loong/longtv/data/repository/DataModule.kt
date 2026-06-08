package com.loong.longtv.data.repository

import com.loong.longtv.data.favorite.FavoriteRepository
import com.loong.longtv.data.favorite.FavoriteRepositoryImpl
import com.loong.longtv.data.history.WatchHistoryRepository
import com.loong.longtv.data.history.WatchHistoryRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideWatchHistoryRepository(impl: WatchHistoryRepositoryImpl): WatchHistoryRepository = impl

    @Provides
    @Singleton
    fun provideFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository = impl
}
