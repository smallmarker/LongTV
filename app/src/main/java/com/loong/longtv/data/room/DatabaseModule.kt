package com.loong.longtv.data.room

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    private val episodeProgressMigration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE watch_history ADD COLUMN episodeIndex INTEGER NOT NULL DEFAULT -1")
            db.execSQL(
                """
                CREATE TABLE episode_progress (
                    siteKey TEXT NOT NULL, videoId TEXT NOT NULL, sourceName TEXT NOT NULL,
                    episodeIndex INTEGER NOT NULL, episodeName TEXT NOT NULL, episodeUrl TEXT NOT NULL,
                    positionMs INTEGER NOT NULL, durationMs INTEGER NOT NULL, updateTime INTEGER NOT NULL,
                    PRIMARY KEY (siteKey, videoId, sourceName, episodeIndex)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT OR REPLACE INTO episode_progress
                SELECT siteKey, videoId, sourceName, episodeIndex, episodeName, episodeUrl, positionMs, durationMs, updateTime
                FROM watch_history ORDER BY updateTime
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "longtv.db"
        ).addMigrations(episodeProgressMigration).fallbackToDestructiveMigration(true).build()

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
