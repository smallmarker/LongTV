package com.loong.longtv.engine.player

import android.net.Uri
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * 媒体URL检测器 - 识别直链播放地址
 */
object MediaDetector {

    private val MEDIA_EXTENSIONS = setOf(
        "m3u8", "mp4", "mpd", "flv", "mkv", "avi", "ts", "m4v", "mov", "wmv", "mpeg", "webm"
    )

    private val QUERY_KEYS = listOf("url", "play", "source", "src", "link", "vid")

    private val DIRECT_MEDIA_PATTERN = Regex(
        """https?://[^\s"'<>]+?\.(m3u8|mp4|mpd|flv|mkv|avi|ts|m4v|mov|wmv|mpeg|webm)(\?[^\s"'<>]*)?""",
        RegexOption.IGNORE_CASE
    )

    /**
     * 尝试从URL中提取直链媒体地址
     * 支持：直接后缀检测、query参数提取、URL解码后再检测
     */
    fun extractDirectUrl(rawUrl: String?, depth: Int = 0): String? {
        if (rawUrl.isNullOrBlank() || depth > 3) return null
        val trimmed = rawUrl.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // file:// 协议直接返回
        if (lower.startsWith("file://")) return trimmed

        val uri = runCatching { Uri.parse(trimmed) }.getOrNull()

        // 扩展名检测仅对 http/https 绝对 URL 生效，避免把相对路径/裸文件名误判为直链
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            val path = uri?.path.orEmpty().lowercase(Locale.ROOT)
            if (path.isNotBlank()) {
                val ext = path.substringAfterLast('.', "")
                if (ext in MEDIA_EXTENSIONS) return trimmed
            }
        }

        // 检查query参数中是否嵌套了媒体URL
        QUERY_KEYS.forEach { key ->
            val value = uri?.getQueryParameter(key)
            extractDirectUrl(value, depth + 1)?.let { return it }
        }

        // URL解码后再检测
        val decoded = runCatching {
            URLDecoder.decode(trimmed, StandardCharsets.UTF_8.name())
        }.getOrDefault(trimmed)
        if (decoded != trimmed) {
            extractDirectUrl(decoded, depth + 1)?.let { return it }
        }

        // 正则匹配（DIRECT_MEDIA_PATTERN 本身只匹配 https?:// 开头的 URL）
        return DIRECT_MEDIA_PATTERN.find(decoded)?.value
            ?: DIRECT_MEDIA_PATTERN.find(trimmed)?.value
    }

    fun isDirectMediaUrl(url: String?): Boolean = extractDirectUrl(url) != null
}
