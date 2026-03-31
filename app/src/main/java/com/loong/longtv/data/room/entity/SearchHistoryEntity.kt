package com.loong.longtv.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author zl
 * @Date 2025/10/21
 **/
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val keyword: String,
    val timestamp: Long = System.currentTimeMillis()
)
