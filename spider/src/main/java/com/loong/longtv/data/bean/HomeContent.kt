package com.loong.longtv.data.bean

import com.google.gson.annotations.SerializedName

/**
 * @author zl
 * @Date 2025/10/9
 **/
data class HomeContent(
    @SerializedName("class")
    val tabs: List<HomeTabItem>,
    val filters: Map<String, List<Filter>>
)

data class HomeTabItem(
    val type_id: String,
    val type_name: String
)

data class Filter(
    val key: String,
    val name: String,
    val value: List<FilterValue>
)

data class FilterValue(
    val n: String,
    val v: String
)