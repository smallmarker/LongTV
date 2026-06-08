package com.loong.longtv.di

import android.content.Context
import com.loong.longtv.engine.TvBoxEngine
import com.loong.longtv.engine.config.ConfigManager
import com.loong.longtv.engine.player.PlayerResolver
import com.loong.longtv.engine.source.SourceRepository
import com.loong.longtv.engine.spider.SpiderManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideTvBoxEngine(@ApplicationContext context: Context): TvBoxEngine {
        return TvBoxEngine(context)
    }

    @Provides
    @Singleton
    fun provideConfigManager(engine: TvBoxEngine): ConfigManager = engine.configManager

    @Provides
    @Singleton
    fun provideSpiderManager(engine: TvBoxEngine): SpiderManager = engine.spiderManager

    @Provides
    @Singleton
    fun provideSourceRepository(engine: TvBoxEngine): SourceRepository = engine.sourceRepository

    @Provides
    @Singleton
    fun providePlayerResolver(engine: TvBoxEngine): PlayerResolver = engine.playerResolver
}
