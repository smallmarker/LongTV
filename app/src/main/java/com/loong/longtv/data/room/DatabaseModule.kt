package com.loong.longtv.data.room

import android.content.Context
import androidx.room.Room
import com.loong.longtv.data.room.dao.FavoriteDao
import com.loong.longtv.data.room.dao.SearchHistoryDao
import com.loong.longtv.data.room.dao.WatchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * @author zl
 * @Date 2025/10/21
 **/
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "longtv.db"
        ).fallbackToDestructiveMigration(true).build()

    @Provides
    fun provideSearchHistoryDao(db: AppDatabase): SearchHistoryDao =
        db.searchHistoryDao()

    @Provides
    fun provideWatchHistoryDao(db: AppDatabase): WatchHistoryDao =
        db.watchHistoryDao()

    @Provides
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao =
        db.favoriteDao()
}
