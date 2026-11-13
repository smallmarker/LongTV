package com.loong.longtv.data.history

import com.loong.longtv.data.room.dao.WatchHistoryDao
import com.loong.longtv.data.room.entity.WatchHistoryEntity
import com.loong.longtv.data.room.entity.EpisodeProgressEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface WatchHistoryRepository {
    suspend fun saveProgress(progress: EpisodeProgressEntity, recent: WatchHistoryEntity?)
    suspend fun findEpisode(
        siteKey: String, videoId: String, sourceName: String, episodeIndex: Int,
        episodeName: String, episodeUrl: String, matchByName: Boolean, matchByUrl: Boolean
    ): EpisodeProgressEntity?
    suspend fun find(siteKey: String, videoId: String): WatchHistoryEntity?
    fun recent(): Flow<List<WatchHistoryEntity>>
    suspend fun delete(historyKeys: List<String>)
    suspend fun clear()
}

@Singleton
class WatchHistoryRepositoryImpl @Inject constructor(
    private val dao: WatchHistoryDao
) : WatchHistoryRepository {

    override suspend fun saveProgress(progress: EpisodeProgressEntity, recent: WatchHistoryEntity?) {
        dao.saveProgress(progress, recent)
    }

    override suspend fun findEpisode(
        siteKey: String, videoId: String, sourceName: String, episodeIndex: Int,
        episodeName: String, episodeUrl: String, matchByName: Boolean, matchByUrl: Boolean
    ) = dao.findEpisode(siteKey, videoId, sourceName, episodeIndex, episodeName, episodeUrl, matchByName, matchByUrl)

    override suspend fun find(siteKey: String, videoId: String): WatchHistoryEntity? {
        return dao.find(siteKey, videoId)
    }

    override fun recent(): Flow<List<WatchHistoryEntity>> = dao.recent()

    override suspend fun delete(historyKeys: List<String>) {
        if (historyKeys.isEmpty()) return
        dao.deleteHistory(historyKeys)
    }

    override suspend fun clear() {
        dao.clearHistory()
    }
}
