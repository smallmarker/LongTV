package com.loong.longtv.ui.tv.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.loong.longtv.nav.Screen
import com.loong.longtv.ui.shared.detail.FullscreenPlayerContent
import com.loong.longtv.ui.shared.detail.FullscreenPlaybackFailure
import com.loong.longtv.ui.shared.detail.PlaybackSearchResume
import com.loong.longtv.ui.shared.detail.VideoDetailViewModel
import com.loong.longtv.ui.shared.detail.playbackIdentity
import com.loong.player.PlayerLoadingOverlay

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
        ?.takeIf { it.destination.route == Screen.VideoDetail.route }
    val detailViewModel = detailBackStackEntry?.let { hiltViewModel<VideoDetailViewModel>(it) }
    val detailState by detailViewModel?.uiState?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(null) }
    val state = detailState
    val detail = state?.detail
    val playLines = remember(detail) { detail?.getPlayLines().orEmpty() }
    val selectedSource = playLines.getOrNull(state?.selectedSourceIndex ?: -1)
    val resolvedMedia = state?.playableMedia
        ?.takeIf { it.url.isNotBlank() && !it.needsParse() }
    val searchSameTitle: ((Long) -> Unit)? = detail?.takeIf { it.name.isNotBlank() }?.let { video ->
        { position ->
            detailViewModel?.applyFullscreenProgress(position)
            PlaybackSearchResume.openSearch(
                navController, video, selectedSource, state.selectedEpisodeIndex, position, closeTvFullscreen = true
            )
        }
    }
    if (detailViewModel != null && resolvedMedia == null) {
        BackHandler { navController.popBackStack() }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val error = state?.error ?: state?.playableMedia?.let { "当前线路暂时无法播放" }
            if (state != null && error != null && !state.playerLoading) {
                FullscreenPlaybackFailure(
                    message = error,
                    isTv = true,
                    modifier = Modifier.align(Alignment.Center),
                    onRetry = { detailViewModel.recoverFullscreenPlayback(state.resumePositionMs, switchLine = false) },
                    onSwitchLine = if (playLines.size > 1) {
                        { detailViewModel.recoverFullscreenPlayback(state.resumePositionMs, switchLine = true) }
                    } else null,
                    onSearch = searchSameTitle?.let { search -> { search(state.resumePositionMs) } },
                    onBack = { navController.popBackStack() }
                )
            } else {
                PlayerLoadingOverlay()
            }
        }
        return
    }
    val resolvedEpisodeIndex = state?.selectedEpisodeIndex ?: episodeIndex
    val resolvedEpisodeCount = selectedSource?.episodes?.size ?: episodeCount
    val resolvedTitle = if (resolvedMedia != null) {
        buildString {
            append(detail?.name?.ifBlank { title } ?: title)
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
        initialPositionMs = state?.resumePositionMs ?: initialPositionMs,
        episodeIndex = resolvedEpisodeIndex,
        episodeCount = resolvedEpisodeCount,
        episodes = selectedSource?.episodes.orEmpty(),
        isTv = true,
        mediaIdentity = detail?.playbackIdentity(selectedSource, resolvedEpisodeIndex, state.playbackSessionId)
            ?: "episode-$episodeIndex",
        onExitFullscreen = { positionMs ->
            detailViewModel?.applyFullscreenProgress(positionMs)
            navController.popBackStack()
        },
        onSwitchEpisode = if (detailViewModel != null && selectedSource != null) {
            { targetIndex -> detailViewModel.playEpisode(selectedSource, targetIndex) }
        } else {
            null
        },
        onRetryPlayback = detailViewModel?.let { model ->
            { position -> model.recoverFullscreenPlayback(position, switchLine = false) }
        },
        onSwitchLine = detailViewModel?.takeIf { playLines.size > 1 }?.let { model ->
            { position -> model.recoverFullscreenPlayback(position, switchLine = true) }
        },
        onSearchSameTitle = searchSameTitle,
        episodeSkipSettings = state?.episodeSkipSettings,
        onSaveEpisodeSkipSettings = detailViewModel?.let { it::saveEpisodeSkipSettings },
        onProgressChangedExternal = { position, duration ->
            if (state != null) {
                detailViewModel?.saveProgress(state, selectedSource, position, duration)
            }
        }
    )
}
