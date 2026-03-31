package com.loong.longtv.data.favorite

import com.loong.longtv.data.room.dao.FavoriteDao
import com.loong.longtv.data.room.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface FavoriteRepository {
    suspend fun save(entity: FavoriteEntity)
    suspend fun remove(favoriteKey: String)
    suspend fun find(siteKey: String, videoId: String): FavoriteEntity?
    fun all(): Flow<List<FavoriteEntity>>
}

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val dao: FavoriteDao
) : FavoriteRepository {

    override suspend fun save(entity: FavoriteEntity) {
        dao.upsert(entity)
    }

    override suspend fun remove(favoriteKey: String) {
        dao.delete(favoriteKey)
    }

    override suspend fun find(siteKey: String, videoId: String): FavoriteEntity? {
        return dao.find(siteKey, videoId)
    }

    override fun all(): Flow<List<FavoriteEntity>> = dao.all()
}
