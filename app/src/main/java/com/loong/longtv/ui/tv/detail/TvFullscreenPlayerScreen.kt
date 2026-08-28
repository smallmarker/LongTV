package com.loong.longtv.ui.tv.detail

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.loong.longtv.ui.shared.detail.FullscreenPlayerContent
import com.loong.longtv.ui.shared.detail.VideoDetailViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TvFullscreenPlayerScreen(
    navController: NavController,
    title: String,
    videoUrl: String,
    requestHeaders: Map<String, String>,
    initialPositionMs: Long,
    episodeIndex: Int,
    episodeCount: Int,
) {
    val detailBackStackEntry = navController.previousBackStackEntry
    val detailViewModel = detailBackStackEntry?.let { hiltViewModel<VideoDetailViewModel>(it) }
    val detailState by detailViewModel?.uiState?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(null) }
    val playLines = detailState?.detail?.getPlayLines().orEmpty()
    val selectedSource = playLines.getOrNull(detailState?.selectedSourceIndex ?: -1)
    val resolvedMedia = detailState?.playableMedia
        ?.takeIf { it.url.isNotBlank() && !it.needsParse() }
    val resolvedEpisodeIndex = resolvedMedia?.let { detailState?.selectedEpisodeIndex } ?: episodeIndex
    val resolvedEpisodeCount = selectedSource?.episodes?.size?.takeIf { it > 0 } ?: episodeCount
    val resolvedTitle = if (resolvedMedia != null) {
        buildString {
            append(detailState?.detail?.name?.ifBlank { title } ?: title)
            selectedSource
                ?.episodes
                ?.getOrNull(resolvedEpisodeIndex)
                ?.name
                ?.takeIf { it.isNotBlank() }
                ?.let { append(" - ").append(it) }
        }
    } else {
        title
    }

    FullscreenPlayerContent(
        navController = navController,
        title = resolvedTitle,
        videoUrl = resolvedMedia?.url ?: videoUrl,
        requestHeaders = resolvedMedia?.headers ?: requestHeaders,
        initialPositionMs = resolvedMedia?.let { detailState?.resumePositionMs } ?: initialPositionMs,
        episodeIndex = resolvedEpisodeIndex,
        episodeCount = resolvedEpisodeCount,
        isTv = true,
        onExitFullscreen = { positionMs ->
            val source = selectedSource
            if (source != null && resolvedEpisodeIndex in source.episodes.indices) {
                detailViewModel?.applyFullscreenProgress(source, resolvedEpisodeIndex, positionMs)
            } else {
                detailViewModel?.applyFullscreenProgress(positionMs)
            }
            navController.popBackStack()
        },
        onSwitchEpisode = { targetIndex ->
            val source = selectedSource
            if (source != null && targetIndex in source.episodes.indices) {
                detailViewModel?.playEpisode(source, targetIndex)
            }
        }
    )
}
