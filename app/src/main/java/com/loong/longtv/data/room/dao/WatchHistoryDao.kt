package com.loong.longtv.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loong.longtv.data.room.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: WatchHistoryEntity)

    @Query("SELECT * FROM watch_history WHERE siteKey = :siteKey AND videoId = :videoId LIMIT 1")
    suspend fun find(siteKey: String, videoId: String): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history ORDER BY updateTime DESC LIMIT 50")
    fun recent(): Flow<List<WatchHistoryEntity>>

    @Query("DELETE FROM watch_history WHERE historyKey IN (:historyKeys)")
    suspend fun deleteByKeys(historyKeys: List<String>)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()
}
