package com.loong.longtv.core.spider

import android.net.Uri
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

object MediaUrlHelper {

    private val directExtensions = listOf(
        ".m3u8", ".mp4", ".mpd", ".flv", ".mkv", ".avi", ".ts", ".m4v", ".mov", ".wmv", ".mpeg"
    )

    private val queryKeys = listOf("url", "play", "source", "src", "link", "vid")

    private val directMediaPattern = Regex(
        pattern = """https?://[^\s"'<>]+?\.(m3u8|mp4|mpd|flv|mkv|avi|ts|m4v|mov|wmv|mpeg)(\?[^\s"'<>]*)?""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    fun isDirectMediaUrl(rawUrl: String?): Boolean = extractDirectMediaUrl(rawUrl) != null

    fun extractDirectMediaUrl(rawUrl: String?, depth: Int = 0): String? {
        if (rawUrl.isNullOrBlank() || depth > 3) return null
        val trimmed = rawUrl.trim()
        val lower = trimmed.lowercase(Locale.getDefault())

        if (lower.startsWith("file://")) return trimmed

        val uri = runCatching { Uri.parse(trimmed) }.getOrNull()
        val path = uri?.path.orEmpty().lowercase(Locale.getDefault())
        if (path.isNotBlank() && directExtensions.any(path::endsWith)) return trimmed

        queryKeys.forEach { key ->
            val queryValue = uri?.getQueryParameter(key)
            val extracted = extractDirectMediaUrl(queryValue, depth + 1)
            if (extracted != null) return extracted
        }

        val decoded = runCatching {
            URLDecoder.decode(trimmed, StandardCharsets.UTF_8.name())
        }.getOrDefault(trimmed)
        if (decoded != trimmed) {
            val extracted = extractDirectMediaUrl(decoded, depth + 1)
            if (extracted != null) return extracted
        }

        return directMediaPattern.find(decoded)?.value
            ?: directMediaPattern.find(trimmed)?.value
    }
}
