package com.loong.longtv.ui.shared.detail

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import com.loong.longtv.engine.source.model.PlayLine
import com.loong.longtv.engine.source.model.VodInfo
import com.loong.longtv.nav.Screen

internal data class PlaybackSearchResume(
    val title: String,
    val year: String,
    val episodeLabel: String?,
    val positionMs: Long
) {
    fun matches(detail: VodInfo): Boolean =
        detail.name.trim().equals(title, ignoreCase = true) && detail.year.trim() == year

    fun episodeIndex(line: PlayLine): Int? = line.uniqueEpisodeIndex(episodeLabel)

    companion object {
        private const val STATE_KEY = "playback_search_resume"
        private val yearPattern = Regex("[0-9]{4}")

        fun openSearch(
            navigator: NavController,
            detail: VodInfo,
            source: PlayLine?,
            episodeIndex: Int,
            positionMs: Long,
            closeTvFullscreen: Boolean = false
        ) {
            val title = detail.name.trim()
            if (title.isEmpty()) return
            val year = detail.year.trim()
            val episode = source?.episodes?.getOrNull(episodeIndex)
            val label = episode?.recoveryLabel()
            val canTransfer = positionMs > 0 && year.matches(yearPattern) && episode != null &&
                source?.uniqueEpisodeIndex(label) == episodeIndex

            navigator.navigate(Screen.SearchResult.createRoute(title)) {
                if (closeTvFullscreen) popUpTo(Screen.FullscreenPlayer.route) { inclusive = true }
            }
            if (canTransfer) {
                navigator.currentBackStackEntry?.savedStateHandle?.set(
                    STATE_KEY,
                    bundleOf("title" to title, "year" to year, "episode" to label, "position" to positionMs)
                )
            }
        }

        fun fromSearch(navigator: NavController): PlaybackSearchResume? {
            val entry = navigator.previousBackStackEntry ?: return null
            if (entry.destination.route != Screen.SearchResult.route) return null
            val saved = entry.savedStateHandle.get<Bundle>(STATE_KEY) ?: return null
            return PlaybackSearchResume(
                title = saved.getString("title") ?: return null,
                year = saved.getString("year") ?: return null,
                episodeLabel = saved.getString("episode"),
                positionMs = saved.getLong("position")
            )
        }
    }
}

internal fun PlayLine.uniqueEpisodeIndex(label: String?): Int? {
    if (label == null && episodes.size != 1) return null
    return episodes.indices.singleOrNull { episodes[it].recoveryLabel() == label }
        ?.takeIf { episodes[it].url.isNotBlank() }
}
