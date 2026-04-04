package com.loong.longtv.data.history

import com.loong.longtv.data.room.dao.WatchHistoryDao
import com.loong.longtv.data.room.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface WatchHistoryRepository {
    suspend fun save(history: WatchHistoryEntity)
    suspend fun find(siteKey: String, videoId: String): WatchHistoryEntity?
    fun recent(): Flow<List<WatchHistoryEntity>>
    suspend fun delete(historyKeys: List<String>)
    suspend fun clear()
}

@Singleton
class WatchHistoryRepositoryImpl @Inject constructor(
    private val dao: WatchHistoryDao
) : WatchHistoryRepository {

    override suspend fun save(history: WatchHistoryEntity) {
        dao.upsert(history)
    }

    override suspend fun find(siteKey: String, videoId: String): WatchHistoryEntity? {
        return dao.find(siteKey, videoId)
    }

    override fun recent(): Flow<List<WatchHistoryEntity>> = dao.recent()

    override suspend fun delete(historyKeys: List<String>) {
        if (historyKeys.isEmpty()) return
        dao.deleteByKeys(historyKeys)
    }

    override suspend fun clear() {
        dao.clearAll()
    }
}
