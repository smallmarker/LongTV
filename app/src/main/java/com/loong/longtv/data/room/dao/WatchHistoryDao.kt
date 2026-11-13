package com.loong.longtv.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.loong.longtv.data.room.entity.EpisodeProgressEntity
import com.loong.longtv.data.room.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: WatchHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpisode(progress: EpisodeProgressEntity)

    @Transaction
    suspend fun saveProgress(progress: EpisodeProgressEntity, recent: WatchHistoryEntity?) {
        upsertEpisode(progress)
        if (recent != null) upsert(recent)
    }

    @Query(
        """
        SELECT * FROM episode_progress
        WHERE siteKey = :siteKey AND videoId = :videoId AND sourceName = :sourceName
          AND ((episodeIndex = :episodeIndex AND episodeName = :episodeName)
               OR (:matchByUrl AND episodeUrl = :episodeUrl)
               OR (:matchByName AND episodeName = :episodeName))
        ORDER BY CASE WHEN episodeIndex = :episodeIndex THEN 0 ELSE 1 END, updateTime DESC
        LIMIT 1
        """
    )
    suspend fun findEpisode(
        siteKey: String,
        videoId: String,
        sourceName: String,
        episodeIndex: Int,
        episodeName: String,
        episodeUrl: String,
        matchByName: Boolean,
        matchByUrl: Boolean
    ): EpisodeProgressEntity?

    @Query("SELECT * FROM watch_history WHERE siteKey = :siteKey AND videoId = :videoId LIMIT 1")
    suspend fun find(siteKey: String, videoId: String): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history ORDER BY updateTime DESC LIMIT 50")
    fun recent(): Flow<List<WatchHistoryEntity>>

    @Query("DELETE FROM watch_history WHERE historyKey IN (:historyKeys)")
    suspend fun deleteByKeys(historyKeys: List<String>)

    @Query(
        """
        DELETE FROM episode_progress WHERE EXISTS (
            SELECT 1 FROM watch_history WHERE historyKey IN (:historyKeys)
                AND watch_history.siteKey = episode_progress.siteKey
                AND watch_history.videoId = episode_progress.videoId
        )
        """
    )
    suspend fun deleteEpisodesByHistoryKeys(historyKeys: List<String>)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()

    @Query("DELETE FROM episode_progress")
    suspend fun clearEpisodes()

    @Transaction
    suspend fun deleteHistory(historyKeys: List<String>) {
        deleteEpisodesByHistoryKeys(historyKeys)
        deleteByKeys(historyKeys)
    }

    @Transaction
    suspend fun clearHistory() {
        clearEpisodes()
        clearAll()
    }
}
