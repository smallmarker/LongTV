package com.loong.longtv.data.player.parser

import com.loong.longtv.core.spider.MediaUrlHelper
import java.net.URL
import javax.inject.Inject

class RedirectParser @Inject constructor(
    private val network: ParserNetwork
) : LocalParser {

    override val name: String = "redirect"

    override suspend fun parse(request: ParseRequest): String? {
        var currentUrl = request.sourceUrl
        repeat(MAX_REDIRECTS) {
            val result = network.fetch(currentUrl, request.headers, readBody = false)
            val mediaFromLocation = MediaUrlHelper.extractDirectMediaUrl(result.location)
            if (mediaFromLocation != null) return mediaFromLocation

            if (isPlayableContentType(result.contentType)) return currentUrl

            if (result.statusCode in 300..399 && !result.location.isNullOrBlank()) {
                currentUrl = URL(URL(currentUrl), result.location).toString()
                return@repeat
            }

            return null
        }
        return null
    }

    private fun isPlayableContentType(contentType: String): Boolean {
        return contentType.startsWith("video/") ||
            contentType.contains("application/vnd.apple.mpegurl") ||
            contentType.contains("application/x-mpegurl") ||
            contentType.contains("application/octet-stream") ||
            contentType.contains("application/dash+xml")
    }

    companion object {
        private const val MAX_REDIRECTS = 5
    }
}
