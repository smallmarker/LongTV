package com.loong.longtv.ui.phone.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.loong.player.ComposeVideoPlayer
import com.loong.player.PlayerResizeMode
import com.loong.longtv.ui.shared.detail.PlayerControlState

@Composable
fun PhoneVideoPlayerView(
    modifier: Modifier,
    videoUrl: String,
    requestHeaders: Map<String, String> = emptyMap(),
    initialPositionMs: Long = 0,
    repeat: Boolean = true,
    autoPlay: Boolean = true,
    showController: Boolean = false,
    enableNativeController: Boolean = true,
    sharedSessionEnabled: Boolean = false,
    sharedSessionReleaseDelayMs: Long = 1_500L,
    resizeMode: PlayerResizeMode = PlayerResizeMode.Fit,
    controlState: PlayerControlState? = null,
    onProgressChanged: (Long, Long) -> Unit = { _, _ -> },
    onControllerVisibilityChanged: ((Boolean) -> Unit)? = null,
    onPlaybackStateChanged: ((Int) -> Unit)? = null,
    onPlayingChanged: ((Boolean) -> Unit)? = null,
    onPlaybackError: ((String) -> Unit)? = null,
    onSurfaceTap: (() -> Unit)? = null,
) {
    ComposeVideoPlayer(
        modifier = modifier,
        videoUrl = videoUrl,
        requestHeaders = requestHeaders,
        initialPositionMs = initialPositionMs,
        repeat = repeat,
        autoPlay = autoPlay,
        showController = showController,
        enableNativeController = enableNativeController,
        sharedSessionEnabled = sharedSessionEnabled,
        sharedSessionReleaseDelayMs = sharedSessionReleaseDelayMs,
        touchControllerEnabled = true,
        resizeMode = resizeMode,
        controlState = controlState,
        onProgressChanged = onProgressChanged,
        onControllerVisibilityChanged = onControllerVisibilityChanged,
        onPlaybackStateChanged = onPlaybackStateChanged,
        onPlayingChanged = onPlayingChanged,
        onPlaybackError = onPlaybackError,
        onSurfaceTap = onSurfaceTap
    )
}
