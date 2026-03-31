package com.loong.longtv.data.player.parser

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalParserRegistry @Inject constructor(
    redirectParser: RedirectParser,
    htmlSnifferParser: HtmlSnifferParser,
) {

    private val parsers: List<LocalParser> = listOf(
        redirectParser,
        htmlSnifferParser
    )

    suspend fun resolve(request: ParseRequest): String? {
        parsers.forEach { parser ->
            val result = runCatching { parser.parse(request) }.getOrNull()
            if (!result.isNullOrBlank()) return result
        }
        return null
    }

    fun parserNames(): List<String> = parsers.map(LocalParser::name)
}
