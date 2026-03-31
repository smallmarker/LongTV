package com.loong.longtv.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val historyKey: String,
    val siteKey: String,
    val videoId: String,
    val title: String,
    val cover: String,
    val sourceName: String,
    val episodeName: String,
    val episodeUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val updateTime: Long = System.currentTimeMillis(),
)
