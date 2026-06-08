package com.loong.longtv.ui.shared.detail

import android.util.Log
import android.view.View
import android.content.Context
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
import com.loong.longtv.engine.player.MediaDetector
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.ControllerVisibilityListener
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.phone.detail.PhoneVideoPlayerView
import com.loong.longtv.ui.tv.detail.TvVideoPlayerView
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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
    enableNativeController: Boolean = true,
    sharedSessionEnabled: Boolean = false,
    controlState: PlayerControlState? = null,
    onProgressChanged: (Long, Long) -> Unit = { _, _ -> },
    onControllerVisibilityChanged: ((Boolean) -> Unit)? = null,
    onPlaybackStateChanged: ((Int) -> Unit)? = null,
    onPlayingChanged: ((Boolean) -> Unit)? = null,
    onPlaybackError: ((String) -> Unit)? = null,
) {
    when (currentDeviceClass()) {
        DeviceClass.PHONE -> PhoneVideoPlayerView(
            modifier = modifier,
            videoUrl = videoUrl,
            requestHeaders = requestHeaders,
            initialPositionMs = initialPositionMs,
            repeat = repeat,
            autoPlay = autoPlay,
            showController = showController,
            enableNativeController = enableNativeController,
            sharedSessionEnabled = sharedSessionEnabled,
            controlState = controlState,
            onProgressChanged = onProgressChanged,
            onControllerVisibilityChanged = onControllerVisibilityChanged,
            onPlaybackStateChanged = onPlaybackStateChanged,
            onPlayingChanged = onPlayingChanged,
            onPlaybackError = onPlaybackError
        )
        DeviceClass.TV -> TvVideoPlayerView(
            modifier = modifier,
            videoUrl = videoUrl,
            requestHeaders = requestHeaders,
            initialPositionMs = initialPositionMs,
            repeat = repeat,
            autoPlay = autoPlay,
            showController = showController,
            enableNativeController = enableNativeController,
            sharedSessionEnabled = sharedSessionEnabled,
            controlState = controlState,
            onProgressChanged = onProgressChanged,
            onControllerVisibilityChanged = onControllerVisibilityChanged,
            onPlaybackStateChanged = onPlaybackStateChanged,
            onPlayingChanged = onPlayingChanged,
            onPlaybackError = onPlaybackError
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun VideoPlayerViewContent(
    modifier: Modifier,
    videoUrl: String,
    requestHeaders: Map<String, String> = emptyMap(),
    initialPositionMs: Long = 0,
    repeat: Boolean = true,
    autoPlay: Boolean = true,
    showController: Boolean = false,
    enableNativeController: Boolean = true,
    sharedSessionEnabled: Boolean = false,
    touchControllerEnabled: Boolean,
    controlState: PlayerControlState? = null,
    onProgressChanged: (Long, Long) -> Unit = { _, _ -> },
    onControllerVisibilityChanged: ((Boolean) -> Unit)? = null,
    onPlaybackStateChanged: ((Int) -> Unit)? = null,
    onPlayingChanged: ((Boolean) -> Unit)? = null,
    onPlaybackError: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val useNativeController = enableNativeController && (showController || touchControllerEnabled)
    val player = remember(videoUrl, requestHeaders, sharedSessionEnabled) {
        if (sharedSessionEnabled) {
            SharedPlayerSession.acquire(context, videoUrl, requestHeaders, repeat)
        } else {
            createExoPlayer(context, requestHeaders, repeat)
        }
    }

    LaunchedEffect(player, videoUrl, requestHeaders, sharedSessionEnabled) {
        try {
            if (!sharedSessionEnabled || SharedPlayerSession.needsPrepare(videoUrl, requestHeaders)) {
                val mimeType = MediaDetector.inferMimeType(videoUrl)
                val item = if (mimeType != null) {
                    MediaItem.Builder().setUri(videoUrl.toUri()).setMimeType(mimeType).build()
                } else {
                    MediaItem.fromUri(videoUrl.toUri())
                }
                player.setMediaItem(item)
                player.prepare()
                if (initialPositionMs > 0) player.seekTo(initialPositionMs)
                SharedPlayerSession.markPrepared(videoUrl, requestHeaders)
            } else if (initialPositionMs > 0 && player.currentPosition <= 0L) {
                player.seekTo(initialPositionMs)
            }
            player.playWhenReady = autoPlay
        } catch (error: Throwable) {
            Log.e("VideoPlayerView", "prepare player failed for url=$videoUrl", error)
        }
    }

    LaunchedEffect(player, videoUrl, initialPositionMs) {
        if (sharedSessionEnabled) return@LaunchedEffect
        if (initialPositionMs <= 0 || player.currentMediaItem == null) return@LaunchedEffect
        if (abs(player.currentPosition - initialPositionMs) > 1_500) {
            player.seekTo(initialPositionMs)
        }
    }

    LaunchedEffect(player, controlState, controlState?.commandVersion) {
        val command = controlState?.consume() ?: return@LaunchedEffect
        when (command) {
            PlayerCommand.TogglePlayPause -> {
                if (player.isPlaying) player.pause() else player.play()
            }

            is PlayerCommand.SeekBy -> {
                val duration = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                val target = (player.currentPosition + command.offsetMs)
                    .coerceAtLeast(0L)
                    .coerceAtMost(duration)
                player.seekTo(target)
            }

            is PlayerCommand.SeekTo -> {
                val duration = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                player.seekTo(command.positionMs.coerceIn(0L, duration))
            }

            is PlayerCommand.SetSpeed -> {
                player.setPlaybackSpeed(command.speed)
            }
        }
    }

    DisposableEffect(player, onPlaybackError, onPlaybackStateChanged, onPlayingChanged) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                onPlaybackStateChanged?.invoke(playbackState)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                onPlayingChanged?.invoke(isPlaying)
            }

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
            if (sharedSessionEnabled) {
                SharedPlayerSession.detachView(player)
                SharedPlayerSession.release(player)
            } else {
                player.release()
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            PlayerView(context).apply {
                if (sharedSessionEnabled) {
                    SharedPlayerSession.attachView(player, this)
                } else {
                    this.player = player
                }
                this.useController = useNativeController
                this.controllerAutoShow = useNativeController
                this.controllerHideOnTouch = true
                this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                this.setKeepContentOnPlayerReset(true)
                this.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                setControllerVisibilityListener(
                    ControllerVisibilityListener { visibility ->
                        onControllerVisibilityChanged?.invoke(visibility == View.VISIBLE)
                    }
                )
            }
        },
        update = { playerView ->
            if (sharedSessionEnabled) {
                SharedPlayerSession.attachView(player, playerView)
            } else {
                playerView.player = player
            }
            playerView.useController = useNativeController
            playerView.controllerAutoShow = useNativeController
            playerView.controllerHideOnTouch = true
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            playerView.setKeepContentOnPlayerReset(true)
            playerView.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            playerView.setControllerVisibilityListener(
                ControllerVisibilityListener { visibility ->
                    onControllerVisibilityChanged?.invoke(visibility == View.VISIBLE)
                }
            )
        }
    )
}

@OptIn(UnstableApi::class)
private fun createExoPlayer(
    context: Context,
    requestHeaders: Map<String, String>,
    repeat: Boolean
): ExoPlayer {
    val httpFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setDefaultRequestProperties(requestHeaders)
    val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)

    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .setLoadControl(DefaultLoadControl())
        .build()
        .apply {
            repeatMode = if (repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
}

private object SharedPlayerSession {
    private val scope = MainScope()
    private var releaseJob: Job? = null
    private var player: ExoPlayer? = null
    private var attachedView: PlayerView? = null
    private var mediaKey: String? = null
    private var preparedKey: String? = null
    private var refCount: Int = 0

    fun acquire(
        context: Context,
        videoUrl: String,
        requestHeaders: Map<String, String>,
        repeat: Boolean
    ): ExoPlayer {
        releaseJob?.cancel()
        val key = buildKey(videoUrl, requestHeaders)
        val existing = player
        return if (existing != null && mediaKey == key) {
            refCount += 1
            existing
        } else {
            existing?.release()
            val created = createExoPlayer(context.applicationContext, requestHeaders, repeat)
            player = created
            attachedView = null
            mediaKey = key
            preparedKey = null
            refCount = 1
            created
        }
    }

    fun needsPrepare(videoUrl: String, requestHeaders: Map<String, String>): Boolean {
        val key = buildKey(videoUrl, requestHeaders)
        return preparedKey != key
    }

    fun markPrepared(videoUrl: String, requestHeaders: Map<String, String>) {
        preparedKey = buildKey(videoUrl, requestHeaders)
    }

    fun release(target: ExoPlayer) {
        if (player !== target) {
            target.release()
            return
        }
        refCount = (refCount - 1).coerceAtLeast(0)
        if (refCount == 0) {
            releaseJob?.cancel()
            releaseJob = scope.launch {
                delay(1_500)
                player?.release()
                player = null
                attachedView = null
                mediaKey = null
                preparedKey = null
            }
        }
    }

    fun attachView(target: ExoPlayer, newView: PlayerView) {
        if (player !== target) {
            newView.player = target
            return
        }
        val previousView = attachedView
        if (previousView === newView && newView.player === target) return
        PlayerView.switchTargetView(target, previousView, newView)
        attachedView = newView
    }

    fun detachView(target: ExoPlayer) {
        if (player !== target) return
        attachedView = null
    }

    private fun buildKey(videoUrl: String, requestHeaders: Map<String, String>): String {
        return videoUrl + "#" + requestHeaders.toList().sortedBy { it.first }.joinToString("&") { "${it.first}=${it.second}" }
    }
}
