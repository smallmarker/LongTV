package com.loong.longtv.ui.home.repository

import com.loong.longtv.data.favorite.FavoriteRepository
import com.loong.longtv.data.favorite.FavoriteRepositoryImpl
import com.loong.longtv.data.history.WatchHistoryRepository
import com.loong.longtv.data.history.WatchHistoryRepositoryImpl
import com.loong.longtv.data.site.SiteRepository
import com.loong.longtv.data.site.SiteRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * @description: 数据模块
 * @author zl
 * @Date 2025/10/9
 **/
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideSpiderRepository(impl: SpiderRepositoryImpl): SpiderRepository = impl

    @Provides
    @Singleton
    fun provideSiteRepository(impl: SiteRepositoryImpl): SiteRepository = impl

    @Provides
    @Singleton
    fun provideWatchHistoryRepository(impl: WatchHistoryRepositoryImpl): WatchHistoryRepository = impl

    @Provides
    @Singleton
    fun provideFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository = impl
}
