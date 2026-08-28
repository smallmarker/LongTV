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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.BehindLiveWindowException
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.ControllerVisibilityListener
import java.io.File
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
    mediaIdentity: String = videoUrl,
    initialPositionMs: Long = 0,
    repeat: Boolean = true,
    autoPlay: Boolean = true,
    showController: Boolean = false,
    enableNativeController: Boolean = true,
    sharedSessionEnabled: Boolean = false,
    sharedSessionReleaseDelayMs: Long = 1_500L,
    cacheEnabled: Boolean = true,
    liveStream: Boolean = false,
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
    val latestOnProgressChanged by rememberUpdatedState(onProgressChanged)
    val latestOnControllerVisibilityChanged by rememberUpdatedState(onControllerVisibilityChanged)
    val latestOnPlaybackStateChanged by rememberUpdatedState(onPlaybackStateChanged)
    val latestOnPlayingChanged by rememberUpdatedState(onPlayingChanged)
    val latestOnPlaybackEnded by rememberUpdatedState(onPlaybackEnded)
    val latestOnPlaybackError by rememberUpdatedState(onPlaybackError)
    val currentResizeMode = controlState?.resizeMode ?: resizeMode
    val useNativeController = enableNativeController && (showController || touchControllerEnabled)
    val player = remember(videoUrl, requestHeaders, mediaIdentity, sharedSessionEnabled, cacheEnabled, liveStream) {
        if (sharedSessionEnabled) {
            SharedPlayerSession.acquire(context, videoUrl, requestHeaders, mediaIdentity, repeat, cacheEnabled, liveStream)
        } else {
            createExoPlayer(context, requestHeaders, repeat, cacheEnabled, liveStream)
        }
    }

    LaunchedEffect(player, videoUrl, requestHeaders, mediaIdentity, sharedSessionEnabled, cacheEnabled, liveStream) {
        try {
            if (!sharedSessionEnabled || SharedPlayerSession.needsPrepare(videoUrl, requestHeaders, mediaIdentity, cacheEnabled, liveStream)) {
                val mimeType = MediaMimeTypes.infer(videoUrl)
                val itemBuilder = MediaItem.Builder().setUri(videoUrl.toUri())
                if (mimeType != null) {
                    itemBuilder.setMimeType(mimeType)
                }
                if (liveStream) {
                    itemBuilder.setLiveConfiguration(
                        MediaItem.LiveConfiguration.Builder()
                            .setTargetOffsetMs(LIVE_TARGET_OFFSET_MS)
                            .setMinOffsetMs(LIVE_MIN_OFFSET_MS)
                            .setMaxOffsetMs(LIVE_MAX_OFFSET_MS)
                            .build()
                    )
                }
                val item = itemBuilder.build()
                player.setMediaItem(item)
                player.prepare()
                // A new episode must always receive its own initial position. Seeking
                // explicitly to 0 prevents the previous shared player's position
                // from carrying over when the resolved stream URL is identical.
                player.seekTo(initialPositionMs.coerceAtLeast(0L))
                SharedPlayerSession.markPrepared(videoUrl, requestHeaders, mediaIdentity, cacheEnabled, liveStream)
            } else if (initialPositionMs > 0 && player.currentPosition <= 0L) {
                player.seekTo(initialPositionMs)
            }
            player.playWhenReady = autoPlay
        } catch (error: Throwable) {
            Log.e("ComposeVideoPlayer", "prepare player failed for url=$videoUrl", error)
            latestOnPlaybackError?.invoke(error.message ?: "播放失败")
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
            is PlayerCommand.SeekToAndPlay -> {
                val duration = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                player.seekTo(command.positionMs.coerceIn(0L, duration))
                player.play()
            }
            is PlayerCommand.SetSpeed -> player.setPlaybackSpeed(command.speed)
            is PlayerCommand.SetVolume -> player.volume = command.volume.coerceIn(0f, 1f)
            is PlayerCommand.SetMuted -> player.volume = if (command.muted) 0f else controlState.volume
            is PlayerCommand.SetResizeMode -> Unit
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                latestOnPlaybackStateChanged?.invoke(playbackState)
                if (playbackState == Player.STATE_ENDED) {
                    latestOnPlaybackEnded?.invoke()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                latestOnPlayingChanged?.invoke(isPlaying)
            }

            override fun onPlayerError(error: PlaybackException) {
                if (liveStream && error.hasCause<BehindLiveWindowException>()) {
                    Log.w("ComposeVideoPlayer", "live window drift recovered for url=$videoUrl")
                    player.recoverLiveWindow(autoPlay)
                    latestOnPlaybackStateChanged?.invoke(Player.STATE_BUFFERING)
                    latestOnPlaybackError?.invoke(BEHIND_LIVE_WINDOW_ERROR)
                    return
                }
                Log.e("ComposeVideoPlayer", "player error for url=$videoUrl", error)
                latestOnPlaybackError?.invoke(error.toUserMessage())
            }
        }
        player.addListener(listener)
        latestOnPlaybackStateChanged?.invoke(player.playbackState)
        latestOnPlayingChanged?.invoke(player.isPlaying)
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
                if (liveStream && player.shouldResyncLiveWindow()) {
                    Log.w("ComposeVideoPlayer", "live offset too large, resync url=$videoUrl offset=${player.currentLiveOffset}")
                    player.recoverLiveWindow(autoPlay)
                    latestOnPlaybackStateChanged?.invoke(Player.STATE_BUFFERING)
                }
                latestOnProgressChanged(player.currentPosition, player.duration.coerceAtLeast(0))
            }
        }
    }

    DisposableEffect(videoUrl, requestHeaders) {
        onDispose {
            if (player.currentMediaItem != null) {
                latestOnProgressChanged(player.currentPosition, player.duration.coerceAtLeast(0))
            }
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
                setKeepContentOnPlayerReset(!liveStream)
                setShutterBackgroundColor(
                    if (liveStream) android.graphics.Color.BLACK else android.graphics.Color.TRANSPARENT
                )
                setSurfaceTapListener(onSurfaceTap)
                setControllerVisibilityListener(
                    ControllerVisibilityListener { visibility ->
                        latestOnControllerVisibilityChanged?.invoke(visibility == View.VISIBLE)
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
            playerView.setKeepContentOnPlayerReset(!liveStream)
            playerView.setShutterBackgroundColor(
                if (liveStream) android.graphics.Color.BLACK else android.graphics.Color.TRANSPARENT
            )
            playerView.setSurfaceTapListener(onSurfaceTap)
            playerView.setControllerVisibilityListener(
                ControllerVisibilityListener { visibility ->
                    latestOnControllerVisibilityChanged?.invoke(visibility == View.VISIBLE)
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
    repeat: Boolean,
    cacheEnabled: Boolean,
    liveStream: Boolean
): ExoPlayer {
    val httpFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(DEFAULT_VIDEO_USER_AGENT)
        .setConnectTimeoutMs(10_000)
        .setReadTimeoutMs(if (liveStream) 25_000 else 15_000)
        .setAllowCrossProtocolRedirects(true)
        .setDefaultRequestProperties(requestHeaders)
    val upstreamFactory = DefaultDataSource.Factory(context, httpFactory)
    val dataSourceFactory = if (cacheEnabled) {
        CacheDataSource.Factory()
            .setCache(VideoPlayerCache.get(context))
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    } else {
        upstreamFactory
    }

    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .setLoadControl(createStreamingLoadControl(liveStream))
        .build()
        .apply {
            repeatMode = if (repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
}

private fun createStreamingLoadControl(liveStream: Boolean): DefaultLoadControl {
    if (liveStream) {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                25_000,
                120_000,
                4_000,
                8_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
    return DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            15_000,
            50_000,
            1_500,
            3_000
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()
}

@OptIn(UnstableApi::class)
private object VideoPlayerCache {
    // Keep video data below 48MB so all disk caches stay within the app's 80MB budget.
    private const val MAX_CACHE_BYTES = 48L * 1024L * 1024L
    private var cache: SimpleCache? = null

    @Synchronized
    fun get(context: Context): SimpleCache {
        val existing = cache
        if (existing != null) return existing
        return SimpleCache(
            File(context.applicationContext.cacheDir, "video_media_cache"),
            LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
            StandaloneDatabaseProvider(context.applicationContext)
        ).also {
            cache = it
        }
    }
}

private const val DEFAULT_VIDEO_USER_AGENT = "LongTV/1.0 Android VideoPlayer"
private const val LIVE_TARGET_OFFSET_MS = 30_000L
private const val LIVE_MIN_OFFSET_MS = 15_000L
private const val LIVE_MAX_OFFSET_MS = 60_000L
private const val LIVE_RESYNC_OFFSET_MS = 75_000L
private const val BEHIND_LIVE_WINDOW_ERROR = "BehindLiveWindowException"

private fun PlaybackException.toUserMessage(): String {
    val details = generateSequence<Throwable>(this) { it.cause }
        .mapNotNull { it.message }
        .distinct()
        .joinToString("；")
    return details.ifBlank { message ?: "播放失败" }
}

private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean {
    return generateSequence<Throwable>(this) { it.cause }.any { it is T }
}

private fun ExoPlayer.shouldResyncLiveWindow(): Boolean {
    if (!isCurrentMediaItemLive) return false
    val offset = currentLiveOffset
    return offset != C.TIME_UNSET && offset > LIVE_RESYNC_OFFSET_MS
}

private fun ExoPlayer.recoverLiveWindow(autoPlay: Boolean) {
    seekToDefaultPosition()
    prepare()
    playWhenReady = autoPlay
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
        mediaIdentity: String,
        repeat: Boolean,
        cacheEnabled: Boolean,
        liveStream: Boolean
    ): ExoPlayer {
        releaseJob?.cancel()
        val key = buildKey(videoUrl, requestHeaders, mediaIdentity, cacheEnabled, liveStream)
        val existing = player
        return if (existing != null && mediaKey == key) {
            refCount += 1
            existing
        } else {
            existing?.release()
            val created = createExoPlayer(context.applicationContext, requestHeaders, repeat, cacheEnabled, liveStream)
            player = created
            attachedView = null
            mediaKey = key
            preparedKey = null
            refCount = 1
            created
        }
    }

    fun needsPrepare(
        videoUrl: String,
        requestHeaders: Map<String, String>,
        mediaIdentity: String,
        cacheEnabled: Boolean,
        liveStream: Boolean
    ): Boolean {
        return preparedKey != buildKey(videoUrl, requestHeaders, mediaIdentity, cacheEnabled, liveStream)
    }

    fun markPrepared(
        videoUrl: String,
        requestHeaders: Map<String, String>,
        mediaIdentity: String,
        cacheEnabled: Boolean,
        liveStream: Boolean
    ) {
        preparedKey = buildKey(videoUrl, requestHeaders, mediaIdentity, cacheEnabled, liveStream)
    }

    fun release(target: ExoPlayer, delayMs: Long) {
        if (player !== target) {
            target.release()
            return
        }
        refCount = (refCount - 1).coerceAtLeast(0)
        if (refCount == 0) {
            // Keep the delayed release for a seamless detail-to-fullscreen handoff,
            // but silence playback as soon as the last UI surface goes away.
            target.pause()
            releaseJob?.cancel()
            releaseJob = scope.launch {
                if (delayMs > 0) delay(delayMs)
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

    private fun buildKey(
        videoUrl: String,
        requestHeaders: Map<String, String>,
        mediaIdentity: String,
        cacheEnabled: Boolean,
        liveStream: Boolean
    ): String {
        return "$mediaIdentity#$videoUrl#cache=$cacheEnabled#live=$liveStream#" + requestHeaders.toList()
            .sortedBy { it.first }
            .joinToString("&") { "${it.first}=${it.second}" }
    }
}
