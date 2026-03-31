package com.loong.longtv.data.bean

data class PlayerContent(
    val parse: Int = 0,
    val url: String = "",
    val header: Map<String, String> = emptyMap(),
    val playUrl: String = "",
)
