package com.loong.longtv.engine.live

import com.github.catvod.net.OkHttp
import com.loong.longtv.engine.source.model.LiveChannel
import com.loong.longtv.engine.source.model.LiveConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LiveSpider {

    suspend fun loadChannels(sources: List<LiveConfig>): Result<List<LiveChannel>> = withContext(Dispatchers.IO) {
        runCatching {
            val channels = sources.flatMap { source ->
                fetchSource(source).getOrDefault(emptyList())
            }.distinctBy { "${it.name.trim()}|${it.url.trim()}" }

            if (channels.isEmpty() && sources.isNotEmpty()) {
                error("没有获取到可用直播频道")
            }
            channels
        }
    }

    private fun fetchSource(source: LiveConfig): Result<List<LiveChannel>> = runCatching {
        val body = OkHttp.string(source.url)
        if (body.isBlank()) return@runCatching emptyList()

        when {
            body.contains("#EXTM3U", ignoreCase = true) -> M3uLiveParser.parse(body, source.name)
            else -> TxtLiveParser.parse(body, source.name)
        }
    }
}

object M3uLiveParser {

    fun parse(content: String, sourceName: String): List<LiveChannel> {
        val channels = mutableListOf<LiveChannel>()
        var pendingInfo: ExtInf? = null

        content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                when {
                    line.startsWith("#EXTINF", ignoreCase = true) -> pendingInfo = parseExtInf(line)
                    line.startsWith("#") -> Unit
                    line.isPlayableUrl() -> {
                        val info = pendingInfo
                        val name = info?.name?.takeIf { it.isNotBlank() } ?: line.channelNameFromUrl()
                        channels += LiveChannel(
                            name = name,
                            url = line,
                            group = info?.group.orEmpty(),
                            logo = info?.logo.orEmpty(),
                            sourceName = sourceName
                        )
                        pendingInfo = null
                    }
                }
            }

        return channels.filter { it.name.isNotBlank() && it.url.isPlayableUrl() }
    }

    private fun parseExtInf(line: String): ExtInf {
        val title = line.substringAfter(",", "").trim()
        return ExtInf(
            name = title.ifBlank {
                line.attribute("tvg-name").ifBlank { line.attribute("tvg-id") }
            },
            group = line.attribute("group-title"),
            logo = line.attribute("tvg-logo")
        )
    }

    private fun String.attribute(name: String): String {
        val marker = "$name=\""
        val start = indexOf(marker)
        if (start < 0) return ""
        return substring(start + marker.length).substringBefore("\"").trim()
    }

    private data class ExtInf(
        val name: String,
        val group: String,
        val logo: String,
    )
}

object TxtLiveParser {

    fun parse(content: String, sourceName: String): List<LiveChannel> {
        val channels = mutableListOf<LiveChannel>()
        var group = ""

        content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                val parts = line.split(",", limit = 2).map { it.trim() }
                if (parts.size < 2) return@forEach

                val name = parts[0]
                val value = parts[1]
                if (value.equals("#genre#", ignoreCase = true)) {
                    group = name
                } else if (value.isPlayableUrl()) {
                    channels += LiveChannel(
                        name = name,
                        url = value,
                        group = group,
                        sourceName = sourceName
                    )
                }
            }

        return channels
    }
}

private fun String.isPlayableUrl(): Boolean {
    return startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)
}

private fun String.channelNameFromUrl(): String {
    return substringBefore("?")
        .substringAfterLast("/")
        .substringBeforeLast(".")
        .ifBlank { "直播频道" }
}
