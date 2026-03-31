package com.loong.longtv.data.player

import com.loong.longtv.core.spider.MediaUrlHelper
import com.loong.longtv.data.bean.PlayerContent
import com.loong.longtv.data.player.parser.LocalParserRegistry
import com.loong.longtv.data.player.parser.ParseRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PlayableMedia(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val requiresParse: Boolean = false,
    val resolutionType: PlaybackResolutionType = PlaybackResolutionType.UNKNOWN,
    val resolutionNote: String = "",
)

enum class PlaybackResolutionType(val label: String) {
    DIRECT("直链"),
    SPIDER("Spider直出"),
    LOCAL_PARSER("本地解析"),
    PENDING_PARSE("待解析"),
    UNKNOWN("未知")
}

@Singleton
class PlayerResolver @Inject constructor(
    private val localParserRegistry: LocalParserRegistry
) {

    suspend fun resolve(content: PlayerContent?): PlayableMedia? = withContext(Dispatchers.IO) {
        if (content == null) return@withContext null
        val sourceUrl = sanitizeSourceUrl(content.playUrl.ifBlank { content.url })
        if (sourceUrl.isBlank()) return@withContext null

        val directUrl = MediaUrlHelper.extractDirectMediaUrl(sourceUrl)
        if (directUrl != null) {
            return@withContext PlayableMedia(
                url = directUrl,
                headers = content.header,
                requiresParse = false,
                resolutionType = PlaybackResolutionType.DIRECT,
                resolutionNote = "从播放地址中直接识别到媒体直链"
            )
        }

        if (content.parse == 0) {
            return@withContext PlayableMedia(
                url = sourceUrl,
                headers = content.header,
                requiresParse = false,
                resolutionType = PlaybackResolutionType.SPIDER,
                resolutionNote = "Spider 已返回可直接播放地址"
            )
        }

        val parsedUrl = localParserRegistry.resolve(
            ParseRequest(
                sourceUrl = sourceUrl,
                headers = content.header
            )
        )
        if (parsedUrl != null) {
            return@withContext PlayableMedia(
                url = parsedUrl,
                headers = content.header,
                requiresParse = false,
                resolutionType = PlaybackResolutionType.LOCAL_PARSER,
                resolutionNote = "本地解析器已命中播放地址"
            )
        }

        PlayableMedia(
            url = sourceUrl,
            headers = content.header,
            requiresParse = true,
            resolutionType = PlaybackResolutionType.PENDING_PARSE,
            resolutionNote = "Spider 返回的地址仍需进一步解析"
        )
    }

    private fun sanitizeSourceUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        val protocolIndex = listOf("https://", "http://")
            .map { marker -> trimmed.indexOf(marker) }
            .filter { it >= 0 }
            .minOrNull()
        return if (protocolIndex != null) trimmed.substring(protocolIndex).trim() else trimmed
    }
}
