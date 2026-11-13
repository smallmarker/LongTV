package com.loong.longtv.data.room.entity

import androidx.room.Entity

@Entity(
    tableName = "episode_progress",
    primaryKeys = ["siteKey", "videoId", "sourceName", "episodeIndex"]
)
data class EpisodeProgressEntity(
    val siteKey: String,
    val videoId: String,
    val sourceName: String,
    val episodeIndex: Int,
    val episodeName: String,
    val episodeUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val updateTime: Long
)
