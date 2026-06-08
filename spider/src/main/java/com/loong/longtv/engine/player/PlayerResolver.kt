package com.loong.longtv.engine.player

import android.util.Log
import com.loong.longtv.engine.source.model.PlayerInfo

/**
 * 播放解析结果
 */
data class PlayableMedia(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val type: PlayableType = PlayableType.UNKNOWN
) {
    fun needsParse(): Boolean = type == PlayableType.PENDING
    fun isReady(): Boolean = type != PlayableType.PENDING && type != PlayableType.UNKNOWN && url.isNotBlank()
}

enum class PlayableType(val label: String) {
    DIRECT("直链"),          // 直接是媒体文件URL
    SPIDER_RESOLVED("Spider解析"), // Spider返回parse=0，已解析
    PARSED("接口解析"),      // 通过解析接口获取
    PENDING("待解析"),       // 需要WebView或其他方式解析
    UNKNOWN("未知")
}

/**
 * 播放解析引擎 - 根据PlayerInfo决定播放策略
 *
 * 解析优先级：
 * 1. 检测URL是否为直链媒体 → 直接播放
 * 2. Spider返回parse=0 → 信任Spider结果直接播放
 * 3. Spider返回parse=1 → 需要调用解析接口
 */
class PlayerResolver {

    /**
     * 解析播放信息，返回可播放的媒体地址
     */
    fun resolve(playerInfo: PlayerInfo): PlayableMedia? {
        val sourceUrl = extractSourceUrl(playerInfo)
        if (sourceUrl.isBlank()) {
            Log.w(TAG, "Empty source URL from playerInfo")
            return null
        }

        // 1. 直链检测
        val directUrl = MediaDetector.extractDirectUrl(sourceUrl)
        if (directUrl != null) {
            return PlayableMedia(
                url = directUrl,
                headers = playerInfo.header,
                type = PlayableType.DIRECT
            )
        }

        // 2. Spider已解析 (parse=0) - 信任结果，但要求 URL 有合法 scheme
        if (!playerInfo.needsParse()) {
            val lower = sourceUrl.lowercase()
            if (!lower.startsWith("http://") && !lower.startsWith("https://") && !lower.startsWith("file://")) {
                Log.w(TAG, "Spider returned URL without valid scheme (will not play): $sourceUrl")
                return null
            }
            return PlayableMedia(
                url = sourceUrl,
                headers = playerInfo.header,
                type = PlayableType.SPIDER_RESOLVED
            )
        }

        // 3. 需要进一步解析 (parse=1)
        // TODO: 接入远程解析接口
        return PlayableMedia(
            url = sourceUrl,
            headers = playerInfo.header,
            type = PlayableType.PENDING
        )
    }

    /**
     * 从playerInfo中提取有效的源URL
     */
    private fun extractSourceUrl(info: PlayerInfo): String {
        val raw = info.effectiveUrl().trim()
        if (raw.isBlank()) return ""

        // 处理可能被前缀污染的URL
        val protocols = listOf("https://", "http://")
        val protocolIndex = protocols
            .map { raw.indexOf(it) }
            .filter { it >= 0 }
            .minOrNull()

        return if (protocolIndex != null && protocolIndex > 0) {
            raw.substring(protocolIndex)
        } else {
            raw
        }
    }

    companion object {
        private const val TAG = "PlayerResolver"
    }
}
