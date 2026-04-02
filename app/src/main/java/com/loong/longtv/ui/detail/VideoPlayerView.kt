package com.loong.longtv.ui.detail

import android.util.Log
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.ControllerVisibilityListener
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import kotlin.math.abs
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    modifier: Modifier,
    videoUrl: String,
    requestHeaders: Map<String, String> = emptyMap(),
    initialPositionMs: Long = 0,
    repeat: Boolean = true,
    autoPlay: Boolean = true,
    showController: Boolean = false,
    onProgressChanged: (Long, Long) -> Unit = { _, _ -> },
    onControllerVisibilityChanged: ((Boolean) -> Unit)? = null,
    onPlaybackError: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val deviceClass = currentDeviceClass()
    val showTouchController = deviceClass != DeviceClass.TV
    val player = remember(videoUrl, requestHeaders) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(requestHeaders)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                repeatMode = if (repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            }
    }

    LaunchedEffect(player, videoUrl, requestHeaders) {
        try {
            val item = MediaItem.fromUri(videoUrl.toUri())
            player.setMediaItem(item)
            player.prepare()
            if (initialPositionMs > 0) player.seekTo(initialPositionMs)
            player.playWhenReady = autoPlay
        } catch (error: Throwable) {
            Log.e("VideoPlayerView", "prepare player failed for url=$videoUrl", error)
        }
    }

    LaunchedEffect(player, videoUrl, initialPositionMs) {
        if (initialPositionMs <= 0 || player.currentMediaItem == null) return@LaunchedEffect
        if (abs(player.currentPosition - initialPositionMs) > 1_500) {
            player.seekTo(initialPositionMs)
        }
    }

    DisposableEffect(player, onPlaybackError) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("VideoPlayerView", "player error for url=$videoUrl", error)
                onPlaybackError?.invoke(error.message ?: "播放失败")
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(player, videoUrl) {
        while (true) {
            delay(1_000)
            if (player.currentMediaItem != null) {
                onProgressChanged(player.currentPosition, player.duration.coerceAtLeast(0))
            }
        }
    }

    DisposableEffect(videoUrl, requestHeaders) {
        onDispose {
            onProgressChanged(player.currentPosition, player.duration.coerceAtLeast(0))
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            PlayerView(context).apply {
                this.player = player
                this.useController = showController || showTouchController
                this.controllerAutoShow = showController || showTouchController
                this.controllerHideOnTouch = true
                this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setControllerVisibilityListener(
                    ControllerVisibilityListener { visibility ->
                        onControllerVisibilityChanged?.invoke(visibility == View.VISIBLE)
                    }
                )
            }
        },
        update = { playerView ->
            playerView.player = player
            playerView.useController = showController || showTouchController
            playerView.controllerAutoShow = showController || showTouchController
            playerView.controllerHideOnTouch = true
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            playerView.setControllerVisibilityListener(
                ControllerVisibilityListener { visibility ->
                    onControllerVisibilityChanged?.invoke(visibility == View.VISIBLE)
                }
            )
        }
    )
}
