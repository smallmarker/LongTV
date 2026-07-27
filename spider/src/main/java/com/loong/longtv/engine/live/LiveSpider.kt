package com.loong.longtv.engine.live

import android.util.Log
import com.github.catvod.net.OkHttp
import com.loong.longtv.engine.source.model.LiveChannel
import com.loong.longtv.engine.source.model.LiveConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.Headers
import java.util.concurrent.TimeUnit

class LiveSpider {

    suspend fun loadChannels(sources: List<LiveConfig>): Result<List<LiveChannel>> = withContext(Dispatchers.IO) {
        runCatching {
            val channels = sources
                .filter { it.url.isNotBlank() }
                .map { source ->
                    async { fetchSource(source).getOrDefault(emptyList()) }
                }
                .awaitAll()
                .flatten()
                .distinctBy { "${it.name.trim()}|${it.url.trim()}" }

            if (channels.isEmpty() && sources.isNotEmpty()) {
                error("没有获取到可用直播频道")
            }
            channels
        }
    }

    private fun fetchSource(source: LiveConfig): Result<List<LiveChannel>> = runCatching {
        val timeoutMs = source.timeout.coerceIn(3, 30) * 1000
        val headers = Headers.Builder()
            .apply { source.ua?.takeIf { it.isNotBlank() }?.let { add("User-Agent", it) } }
            .build()
        val client = OkHttp.client(timeoutMs)
            .newBuilder()
            .callTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .build()
        val body = OkHttp.newCall(client, source.url, headers)
            .execute()
            .use { response -> if (response.isSuccessful) response.body?.string().orEmpty() else "" }
        if (body.isBlank()) {
            Log.w(TAG, "source=${source.name} returned empty body url=${source.url}")
            return@runCatching emptyList()
        }

        val channels = when {
            body.contains("#EXTM3U", ignoreCase = true) -> M3uLiveParser.parse(body, source.name)
            else -> TxtLiveParser.parse(body, source.name)
        }
        Log.d(TAG, "source=${source.name} channels=${channels.size} bytes=${body.length}")
        channels
    }.onFailure { error ->
        Log.w(TAG, "source=${source.name} failed url=${source.url}: ${error.message}", error)
    }

    companion object {
        private const val TAG = "LiveSpider"
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
                } else if (value.isDirectStreamUrl() && !isTxtPlaceholder(name, value)) {
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

private fun isTxtPlaceholder(name: String, url: String): Boolean {
    val normalizedUrl = url.trimEnd('/')
    return normalizedUrl.endsWith("/tv/t", ignoreCase = true) ||
        normalizedUrl.endsWith("/tv/v", ignoreCase = true) ||
        name.contains("⬇") ||
        Regex("""^\d{1,2}:\d{2}.*[🆚VS]""").containsMatchIn(name)
}

private fun String.isPlayableUrl(): Boolean {
    return startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)
}

private fun String.isDirectStreamUrl(): Boolean {
    if (!isPlayableUrl()) return false
    val path = substringBefore("?").substringBefore("#").lowercase()
    return path.endsWith(".m3u8") ||
        path.endsWith(".flv") ||
        path.endsWith(".mp4") ||
        path.endsWith(".ts")
}

private fun String.channelNameFromUrl(): String {
    return substringBefore("?")
        .substringAfterLast("/")
        .substringBeforeLast(".")
        .ifBlank { "直播频道" }
}
