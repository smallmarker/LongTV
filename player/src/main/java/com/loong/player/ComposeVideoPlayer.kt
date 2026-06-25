package com.loong.player

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.ControllerVisibilityListener
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun ComposeVideoPlayer(
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
    touchControllerEnabled: Boolean = true,
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
    val context = LocalContext.current
    val currentResizeMode = controlState?.resizeMode ?: resizeMode
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
                val mimeType = MediaMimeTypes.infer(videoUrl)
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
            Log.e("ComposeVideoPlayer", "prepare player failed for url=$videoUrl", error)
            onPlaybackError?.invoke(error.message ?: "播放失败")
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
            PlayerCommand.Play -> player.play()
            PlayerCommand.Pause -> player.pause()
            PlayerCommand.TogglePlayPause -> if (player.isPlaying) player.pause() else player.play()
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
            is PlayerCommand.SetSpeed -> player.setPlaybackSpeed(command.speed)
            is PlayerCommand.SetVolume -> player.volume = command.volume.coerceIn(0f, 1f)
            is PlayerCommand.SetMuted -> player.volume = if (command.muted) 0f else controlState.volume
            is PlayerCommand.SetResizeMode -> Unit
        }
    }

    DisposableEffect(player, onPlaybackError, onPlaybackStateChanged, onPlayingChanged, onPlaybackEnded) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                onPlaybackStateChanged?.invoke(playbackState)
                if (playbackState == Player.STATE_ENDED) {
                    onPlaybackEnded?.invoke()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                onPlayingChanged?.invoke(isPlaying)
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("ComposeVideoPlayer", "player error for url=$videoUrl", error)
                onPlaybackError?.invoke(error.message ?: "播放失败")
            }
        }
        player.addListener(listener)
        onPlaybackStateChanged?.invoke(player.playbackState)
        onPlayingChanged?.invoke(player.isPlaying)
        onDispose {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(player, controlState?.muted, controlState?.volume) {
        val state = controlState ?: return@LaunchedEffect
        player.volume = if (state.muted) 0f else state.volume
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
                SharedPlayerSession.release(player, sharedSessionReleaseDelayMs)
            } else {
                player.release()
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            createPlayerView(context).apply {
                if (sharedSessionEnabled) {
                    SharedPlayerSession.attachView(player, this)
                } else {
                    this.player = player
                }
                useController = useNativeController
                controllerAutoShow = useNativeController
                controllerHideOnTouch = true
                this.resizeMode = currentResizeMode.toMedia3ResizeMode()
                setKeepContentOnPlayerReset(true)
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                setSurfaceTapListener(onSurfaceTap)
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
            playerView.resizeMode = currentResizeMode.toMedia3ResizeMode()
            playerView.setKeepContentOnPlayerReset(true)
            playerView.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            playerView.setSurfaceTapListener(onSurfaceTap)
            playerView.setControllerVisibilityListener(
                ControllerVisibilityListener { visibility ->
                    onControllerVisibilityChanged?.invoke(visibility == View.VISIBLE)
                }
            )
        }
    )
}

private fun createPlayerView(context: Context): PlayerView {
    return LayoutInflater.from(context)
        .inflate(R.layout.loong_player_view, null, false) as PlayerView
}

private fun PlayerResizeMode.toMedia3ResizeMode(): Int = when (this) {
    PlayerResizeMode.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    PlayerResizeMode.Fill -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    PlayerResizeMode.Zoom -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    PlayerResizeMode.FixedWidth -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
    PlayerResizeMode.FixedHeight -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
}

private fun PlayerView.setSurfaceTapListener(onSurfaceTap: (() -> Unit)?) {
    setSurfaceTapListenerRecursively(onSurfaceTap)
}

private fun View.setSurfaceTapListenerRecursively(onSurfaceTap: (() -> Unit)?) {
    installSurfaceTapListener(onSurfaceTap)
    if (this is ViewGroup) {
        for (index in 0 until childCount) {
            getChildAt(index).setSurfaceTapListenerRecursively(onSurfaceTap)
        }
    }
}

private fun View.installSurfaceTapListener(onSurfaceTap: (() -> Unit)?) {
    if (onSurfaceTap == null) {
        setOnTouchListener(null)
        return
    }
    var downX = 0f
    var downY = 0f
    var downTime = 0L
    setOnTouchListener { _, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                downTime = event.eventTime
                true
            }
            MotionEvent.ACTION_UP -> {
                val movedX = abs(event.x - downX)
                val movedY = abs(event.y - downY)
                val elapsed = event.eventTime - downTime
                if (movedX < 24f && movedY < 24f && elapsed < 500L) {
                    performClick()
                    onSurfaceTap()
                }
                true
            }
            MotionEvent.ACTION_CANCEL -> true
            else -> true
        }
    }
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
        return preparedKey != buildKey(videoUrl, requestHeaders)
    }

    fun markPrepared(videoUrl: String, requestHeaders: Map<String, String>) {
        preparedKey = buildKey(videoUrl, requestHeaders)
    }

    fun release(target: ExoPlayer, delayMs: Long) {
        if (player !== target) {
            target.release()
            return
        }
        refCount = (refCount - 1).coerceAtLeast(0)
        if (refCount == 0) {
            releaseJob?.cancel()
            releaseJob = scope.launch {
                if (delayMs > 0) delay(delayMs)
                target.pause()
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
        return videoUrl + "#" + requestHeaders.toList()
            .sortedBy { it.first }
            .joinToString("&") { "${it.first}=${it.second}" }
    }
}
