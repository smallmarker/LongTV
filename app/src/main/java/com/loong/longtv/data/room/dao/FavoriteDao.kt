package com.loong.longtv.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loong.longtv.data.room.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteEntity)

    @Query("SELECT * FROM favorite WHERE siteKey = :siteKey AND videoId = :videoId LIMIT 1")
    suspend fun find(siteKey: String, videoId: String): FavoriteEntity?

    @Query("SELECT * FROM favorite ORDER BY createTime DESC LIMIT 100")
    fun all(): Flow<List<FavoriteEntity>>

    @Query("DELETE FROM favorite WHERE favoriteKey = :favoriteKey")
    suspend fun delete(favoriteKey: String)
}
