package com.loong.longtv.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.loong.longtv.data.room.dao.FavoriteDao
import com.loong.longtv.data.room.dao.SearchHistoryDao
import com.loong.longtv.data.room.dao.WatchHistoryDao
import com.loong.longtv.data.room.entity.FavoriteEntity
import com.loong.longtv.data.room.entity.SearchHistoryEntity
import com.loong.longtv.data.room.entity.WatchHistoryEntity

/**
 * @author zl
 * @Date 2025/10/21
 **/
@Database(
    entities = [SearchHistoryEntity::class, WatchHistoryEntity::class, FavoriteEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase(){
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun favoriteDao(): FavoriteDao
}
