package com.loong.longtv.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite")
data class FavoriteEntity(
    @PrimaryKey val favoriteKey: String,
    val siteKey: String,
    val videoId: String,
    val title: String,
    val cover: String,
    val createTime: Long = System.currentTimeMillis(),
)
