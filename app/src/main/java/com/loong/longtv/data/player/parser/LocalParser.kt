package com.loong.longtv.data.player.parser

data class ParseRequest(
    val sourceUrl: String,
    val headers: Map<String, String> = emptyMap()
)

interface LocalParser {
    val name: String
    suspend fun parse(request: ParseRequest): String?
}
