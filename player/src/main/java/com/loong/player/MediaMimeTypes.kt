package com.loong.player

import android.net.Uri
import java.util.Locale

internal object MediaMimeTypes {
    private val queryKeys = listOf("url", "play", "source", "src", "link", "vid")

    fun infer(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return null

        val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return null
        extToMime(uri.path.orEmpty().lowercase(Locale.ROOT).substringAfterLast('.', ""))
            ?.let { return it }

        for (key in queryKeys) {
            val value = uri.getQueryParameter(key) ?: continue
            extToMime(value.lowercase(Locale.ROOT).substringAfterLast('.', ""))
                ?.let { return it }
        }
        return null
    }

    private fun extToMime(ext: String): String? = when (ext) {
        "m3u8" -> "application/x-mpegURL"
        "mpd" -> "application/dash+xml"
        "mp4", "m4v" -> "video/mp4"
        "flv" -> "video/x-flv"
        "mkv" -> "video/x-matroska"
        else -> null
    }
}
