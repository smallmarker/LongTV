package com.loong.longtv.ui.shared.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.loong.player.PlayerInputMode
import com.loong.player.UnifiedVideoPlayer
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass

@Composable
fun VideoPlayerView(
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
    val deviceClass = currentDeviceClass()
    val inputMode = when (deviceClass) {
        DeviceClass.PHONE -> PlayerInputMode.Touch
        DeviceClass.TV -> PlayerInputMode.Remote
    }

    KeepScreenOnEffect(enabled = deviceClass == DeviceClass.TV)

    UnifiedVideoPlayer(
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
        inputMode = inputMode,
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
