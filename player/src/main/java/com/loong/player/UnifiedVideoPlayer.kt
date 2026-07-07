package com.loong.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class PlayerInputMode {
    Touch,
    Remote
}

@Composable
fun UnifiedVideoPlayer(
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
    cacheEnabled: Boolean = true,
    liveStream: Boolean = false,
    inputMode: PlayerInputMode = PlayerInputMode.Touch,
    resizeMode: PlayerResizeMode = PlayerResizeMode.Fit,
    controlState: PlayerControlState? = null,
    onProgressChanged: (Long, Long) -> Unit = { _, _ -> },
    onControllerVisibilityChanged: ((Boolean) -> Unit)? = null,
    onPlaybackStateChanged: ((Int) -> Unit)? = null,
    onPlayingChanged: ((Boolean) -> Unit)? = null,
    onPlaybackEnded: (() -> Unit)? = null,
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
        cacheEnabled = cacheEnabled,
        liveStream = liveStream,
        touchControllerEnabled = inputMode == PlayerInputMode.Touch,
        resizeMode = resizeMode,
        controlState = controlState,
        onProgressChanged = onProgressChanged,
        onControllerVisibilityChanged = onControllerVisibilityChanged,
        onPlaybackStateChanged = onPlaybackStateChanged,
        onPlayingChanged = onPlayingChanged,
        onPlaybackEnded = onPlaybackEnded,
        onPlaybackError = onPlaybackError,
        onSurfaceTap = onSurfaceTap
    )
}
