package com.loong.longtv.data.player.parser

import com.loong.longtv.core.spider.MediaUrlHelper
import javax.inject.Inject

class HtmlSnifferParser @Inject constructor(
    private val network: ParserNetwork
) : LocalParser {

    override val name: String = "html_sniffer"

    override suspend fun parse(request: ParseRequest): String? {
        val result = network.fetch(request.sourceUrl, request.headers, readBody = true)
        if (!shouldSniff(result.contentType)) return null
        return MediaUrlHelper.extractDirectMediaUrl(result.body)
    }

    private fun shouldSniff(contentType: String): Boolean {
        return contentType.contains("html") ||
            contentType.contains("json") ||
            contentType.contains("javascript")
    }
}
