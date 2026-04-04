package com.loong.longtv.ui.tv.detail

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import com.loong.longtv.ui.shared.detail.PlayerControlState
import com.loong.longtv.ui.shared.detail.VideoPlayerViewContent

@OptIn(UnstableApi::class)
@Composable
fun TvVideoPlayerView(
    modifier: Modifier,
    videoUrl: String,
    requestHeaders: Map<String, String> = emptyMap(),
    initialPositionMs: Long = 0,
    repeat: Boolean = true,
    autoPlay: Boolean = true,
    showController: Boolean = false,
    enableNativeController: Boolean = true,
    sharedSessionEnabled: Boolean = false,
    controlState: PlayerControlState? = null,
    onProgressChanged: (Long, Long) -> Unit = { _, _ -> },
    onControllerVisibilityChanged: ((Boolean) -> Unit)? = null,
    onPlaybackStateChanged: ((Int) -> Unit)? = null,
    onPlayingChanged: ((Boolean) -> Unit)? = null,
    onPlaybackError: ((String) -> Unit)? = null,
) {
    VideoPlayerViewContent(
        modifier = modifier,
        videoUrl = videoUrl,
        requestHeaders = requestHeaders,
        initialPositionMs = initialPositionMs,
        repeat = repeat,
        autoPlay = autoPlay,
        showController = showController,
        enableNativeController = enableNativeController,
        sharedSessionEnabled = sharedSessionEnabled,
        touchControllerEnabled = false,
        controlState = controlState,
        onProgressChanged = onProgressChanged,
        onControllerVisibilityChanged = onControllerVisibilityChanged,
        onPlaybackStateChanged = onPlaybackStateChanged,
        onPlayingChanged = onPlayingChanged,
        onPlaybackError = onPlaybackError
    )
}
