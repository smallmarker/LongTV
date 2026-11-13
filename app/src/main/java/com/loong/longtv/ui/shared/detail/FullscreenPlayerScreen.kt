package com.loong.longtv.ui.shared.detail

import android.app.Activity
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.SystemClock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.navigation.NavController
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.loong.longtv.engine.source.model.Episode
import com.loong.longtv.data.EpisodeSkipSettings
import com.loong.longtv.ui.shared.widgets.TvSurface
import com.loong.longtv.ui.theme.AppCardOverlay
import com.loong.longtv.ui.theme.AppSurfaceVariant
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.theme.BrandPrimaryDark
import com.loong.player.PhoneFeedbackOverlay
import com.loong.player.PhoneFullscreenGestureLayer
import com.loong.player.PhoneGestureType
import com.loong.player.PlayerFeedbackPopup
import com.loong.player.PlayerLoadingOverlay
import com.loong.player.SeekOverlayState
import com.loong.player.rememberPlayerInteractionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private enum class BottomPanel {
    SPEED,
    QUALITY,
    EPISODE,
    PLAYBACK_SETTINGS
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FullscreenPlayerContent(
    navController: NavController,
    title: String,
    videoUrl: String,
    requestHeaders: Map<String, String>,
    initialPositionMs: Long,
    episodeIndex: Int,
    episodeCount: Int,
    isTv: Boolean,
    episodes: List<Episode> = emptyList(),
    mediaIdentity: String = "episode-$episodeIndex",
    manageOrientation: Boolean = true,
    onExitFullscreen: ((Long) -> Unit)? = null,
    onSwitchEpisode: ((Int) -> Unit)? = null,
    onRetryPlayback: ((Long) -> Unit)? = null,
    onSwitchLine: ((Long) -> Unit)? = null,
    onSearchSameTitle: ((Long) -> Unit)? = null,
    episodeSkipSettings: EpisodeSkipSettings? = null,
    onSaveEpisodeSkipSettings: ((EpisodeSkipSettings) -> Unit)? = null,
    onProgressChangedExternal: ((Long, Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val audioManager = remember(context) { context.getSystemService(AUDIO_SERVICE) as AudioManager }
    val haptic = LocalHapticFeedback.current
    val controlState = rememberPlayerControlState()
    val speedOptions = remember { listOf(1.0f, 1.25f, 1.5f, 2.0f) }
    val qualityOptions = remember { listOf("跟随片源", "高清 1080P", "超清 4K") }
    var currentPositionMs by remember(videoUrl, episodeIndex, initialPositionMs) {
        mutableLongStateOf(initialPositionMs)
    }
    var episodeSwitchPending by remember(mediaIdentity, videoUrl) { mutableStateOf(false) }
    var retryCount by remember(mediaIdentity, videoUrl) { mutableIntStateOf(0) }
    var retryPosition by remember(mediaIdentity, videoUrl) { mutableLongStateOf(initialPositionMs) }
    var introChecked by remember(mediaIdentity, videoUrl, retryCount) { mutableStateOf(false) }
    var skipOutroCompleted by remember(mediaIdentity, videoUrl, retryCount) { mutableStateOf(false) }
    var editedOutro by remember(mediaIdentity, videoUrl, retryCount) { mutableStateOf<EpisodeSkipSettings?>(null) }
    var settingsPositionMs by remember(mediaIdentity, videoUrl) { mutableLongStateOf(0L) }
    var settingsOpenCount by remember(mediaIdentity, videoUrl) { mutableIntStateOf(0) }
    var resumeAfterSettings by remember(mediaIdentity, videoUrl) { mutableStateOf(false) }
    var durationMs by remember(title, videoUrl) { mutableLongStateOf(0L) }
    var isPlaying by remember(title, videoUrl) { mutableStateOf(false) }
    var isBuffering by remember(title, videoUrl) { mutableStateOf(false) }
    var playerReady by remember(title, videoUrl) { mutableStateOf(false) }
    var playbackError by remember(title, videoUrl) { mutableStateOf<String?>(null) }
    var playbackSpeed by remember(title, videoUrl) { mutableFloatStateOf(1.0f) }
    var transientMessage by remember(title, videoUrl) { mutableStateOf<String?>(null) }
    var progressBarWidthPx by remember(title, videoUrl) { mutableFloatStateOf(1f) }
    var selectedQuality by remember(title, videoUrl) { mutableStateOf(qualityOptions[0]) }
    var activePanel by remember(title, videoUrl) { mutableStateOf<BottomPanel?>(null) }
    var displayedPanel by remember(title, videoUrl) { mutableStateOf<BottomPanel?>(null) }
    val interactionState = rememberPlayerInteractionState(
        initialPositionMs = initialPositionMs,
        canAutoHideControls = playbackError == null && !isBuffering && activePanel == null
    )
    var brightnessLevel by remember(title, videoUrl) {
        mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f)
    }
    var volumePercent by remember(title, videoUrl) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        mutableIntStateOf((audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f / maxVolume).roundToLong().toInt())
    }
    val tvFocusRequester = remember(title, videoUrl) { FocusRequester() }
    val tvPrimaryActionFocusRequester = remember(title, videoUrl) { FocusRequester() }
    val tvSpeedPanelFocusRequester = remember(title, videoUrl) { FocusRequester() }
    val tvQualityPanelFocusRequester = remember(title, videoUrl) { FocusRequester() }
    val tvEpisodePanelFocusRequester = remember(title, videoUrl) { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    var remoteSeekJob by remember(title, videoUrl) { mutableStateOf<Job?>(null) }
    var remoteSeekDirection by remember(title, videoUrl) { mutableIntStateOf(0) }
    var remoteSeekAnchorMs by remember(title, videoUrl) { mutableLongStateOf(0L) }
    var remoteSeekPreviewMs by remember(title, videoUrl) { mutableLongStateOf(0L) }
    var currentClockText by remember { mutableStateOf(formatCurrentTime()) }

    val episodeLabel = if (episodeCount > 0) "第${episodeIndex + 1}集" else "单集播放"
    val compactTitle = buildString {
        append(title.ifBlank { "全屏播放" })
        if (episodeCount > 0) append(" · ").append(episodeLabel)
    }
    val currentDisplayPosition = if (interactionState.isScrubbing) interactionState.scrubPositionMs else currentPositionMs
    val progress = playbackProgress(currentDisplayPosition, durationMs)
    val brightnessPercent = (brightnessLevel * 100f).roundToInt().coerceIn(0, 100)

    fun registerInteraction() {
        interactionState.registerInteraction()
    }

    fun showControls() {
        interactionState.showControls()
    }

    fun closePanel() {
        if (activePanel == BottomPanel.PLAYBACK_SETTINGS && resumeAfterSettings) {
            resumeAfterSettings = false
            controlState.play()
        }
        activePanel = null
    }

    fun hideControls() {
        if (activePanel != null || displayedPanel != null) {
            closePanel()
        } else {
            interactionState.hideControls()
        }
    }

    fun toggleControls() {
        if (interactionState.controlsVisible || activePanel != null) {
            hideControls()
        } else {
            showControls()
        }
    }

    fun showTransientMessage(message: String) {
        transientMessage = message
        registerInteraction()
    }

    fun openPlaybackSettings() {
        if (durationMs <= 0L || !playerReady || isBuffering) {
            showTransientMessage("视频加载完成后可设置片头片尾")
            return
        }
        settingsPositionMs = currentPositionMs.coerceIn(0L, durationMs)
        settingsOpenCount++
        resumeAfterSettings = isPlaying
        controlState.pause()
        activePanel = BottomPanel.PLAYBACK_SETTINGS
        registerInteraction()
    }

    fun seekToPosition(positionMs: Long) {
        val duration = durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE
        currentPositionMs = positionMs.coerceIn(0L, duration)
        interactionState.commitScrub(currentPositionMs)
        onProgressChangedExternal?.invoke(currentPositionMs, durationMs)
        controlState.seekTo(currentPositionMs)
    }

    fun togglePlayPause() {
        val targetIsPlaying = !isPlaying
        controlState.togglePlayPause()
        interactionState.showPlayPause(targetIsPlaying)
    }

    fun replayFromStart() {
        currentPositionMs = 0L
        interactionState.commitScrub(0L)
        onProgressChangedExternal?.invoke(0L, durationMs)
        controlState.seekToAndPlay(0L)
        interactionState.showPlayPause(true)
    }

    fun showTvControls() {
        showControls()
    }

    fun refreshTvMenuInteraction() {
        if (isTv && interactionState.controlsVisible) {
            interactionState.registerInteraction()
        }
    }

    fun togglePanel(panel: BottomPanel) {
        if (activePanel == panel) {
            closePanel()
        } else {
            activePanel = panel
            registerInteraction()
        }
    }

    fun updateBrightnessPercent(targetPercent: Int) {
        val window = activity?.window ?: return
        val clamped = targetPercent.coerceIn(0, 100)
        brightnessLevel = clamped / 100f
        window.attributes = window.attributes.apply {
            screenBrightness = brightnessLevel
        }
        interactionState.showAdjustment(PhoneGestureType.BRIGHTNESS, clamped)
    }

    fun updateVolumePercent(targetPercent: Int) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val clamped = targetPercent.coerceIn(0, 100)
        val targetVolume = (maxVolume * (clamped / 100f)).roundToInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        volumePercent = clamped
        interactionState.showAdjustment(PhoneGestureType.VOLUME, clamped)
    }

    fun hideTvControls() {
        hideControls()
    }

    fun handleTvBack(): Boolean {
        return when {
            activePanel != null -> {
                closePanel()
                true
            }

            interactionState.controlsVisible -> {
                hideTvControls()
                true
            }

            else -> false
        }
    }

    fun exitFullscreen() {
        onExitFullscreen?.let { exit ->
            exit(currentPositionMs)
            return
        }
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.set("fullscreen_position", currentPositionMs)
        navController.popBackStack()
    }

    fun switchEpisode(targetIndex: Int) {
        if (episodeSwitchPending || targetIndex !in 0 until episodeCount || targetIndex == episodeIndex) return
        episodeSwitchPending = true
        activePanel = null
        interactionState.clearFeedback()
        interactionState.cancelGesture()
        interactionState.showControls()
        onSwitchEpisode?.let { switch ->
            switch(targetIndex)
            return
        }
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.set("fullscreen_target_episode_index", targetIndex)
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.set("fullscreen_reenter", true)
        navController.popBackStack()
    }

    fun playNextEpisodeFromCurrent() {
        val targetIndex = episodeIndex + 1
        if (targetIndex in 0 until episodeCount) {
            switchEpisode(targetIndex)
        } else if (!episodeSwitchPending) {
            episodeSwitchPending = true
            exitFullscreen()
        }
    }

    LaunchedEffect(
        episodeSkipSettings, currentPositionMs, durationMs, isPlaying, activePanel,
        episodeSwitchPending, playbackError, interactionState.isScrubbing, editedOutro
    ) {
        val settings = episodeSkipSettings ?: return@LaunchedEffect
        if (!isPlaying || activePanel == BottomPanel.PLAYBACK_SETTINGS ||
            episodeSwitchPending || playbackError != null || interactionState.isScrubbing
        ) return@LaunchedEffect
        val hasNext = episodeIndex + 1 < episodeCount
        val range = settings.playbackRange(durationMs, hasNext) ?: return@LaunchedEffect
        if (!introChecked) {
            introChecked = true
            if (currentPositionMs < range.first) {
                controlState.seekTo(range.first)
                return@LaunchedEffect
            }
        }
        if (editedOutro != null) {
            if (editedOutro == settings && currentPositionMs < range.last) editedOutro = null
            return@LaunchedEffect
        }
        if (hasNext && settings.outroRemainingMs != null && currentPositionMs in range.last until durationMs) {
            skipOutroCompleted = true
            onProgressChangedExternal?.invoke(durationMs, durationMs)
            playNextEpisodeFromCurrent()
        }
    }

    fun positionFromBarOffset(offsetX: Float): Long {
        if (durationMs <= 0 || progressBarWidthPx <= 0f) return currentPositionMs
        val percent = (offsetX / progressBarWidthPx).coerceIn(0f, 1f)
        return (durationMs * percent).roundToLong()
    }

    fun remoteSeekOverlayState(targetPositionMs: Long, anchorPositionMs: Long, direction: Int): SeekOverlayState {
        val duration = durationMs.coerceAtLeast(0L)
        val reachedEdgeMessage = when {
            targetPositionMs == 0L && direction < 0 -> "已至开头"
            duration > 0L && targetPositionMs == duration && direction > 0 -> "已至结尾"
            else -> null
        }
        return SeekOverlayState(
            targetPositionMs = targetPositionMs,
            durationMs = duration,
            deltaMs = targetPositionMs - anchorPositionMs,
            reachedEdgeMessage = reachedEdgeMessage
        )
    }

    fun updateRemoteSeekPreview(direction: Int, duration: Long, deltaMs: Long) {
        val targetPosition = (remoteSeekAnchorMs + direction * deltaMs).coerceIn(0L, duration)
        remoteSeekPreviewMs = targetPosition
        interactionState.updateSeekPreview(
            remoteSeekOverlayState(
                targetPositionMs = targetPosition,
                anchorPositionMs = remoteSeekAnchorMs,
                direction = direction
            )
        )
    }

    fun startRemoteSeek(direction: Int) {
        val duration = durationMs.takeIf { it > 0 } ?: return
        if (remoteSeekDirection == direction && remoteSeekJob?.isActive == true) return

        remoteSeekJob?.cancel()
        remoteSeekDirection = direction
        remoteSeekAnchorMs = currentPositionMs.coerceIn(0L, duration)
        remoteSeekPreviewMs = remoteSeekAnchorMs
        activePanel = null
        interactionState.beginGesture()
        updateRemoteSeekPreview(
            direction = direction,
            duration = duration,
            deltaMs = REMOTE_SEEK_TAP_STEP_MS
        )

        remoteSeekJob = coroutineScope.launch {
            delay(REMOTE_SEEK_HOLD_DELAY_MS)
            val startedAt = SystemClock.elapsedRealtime()
            while (isActive) {
                val elapsedMs = SystemClock.elapsedRealtime() - startedAt
                updateRemoteSeekPreview(
                    direction = direction,
                    duration = duration,
                    deltaMs = remoteSeekDeltaMs(elapsedMs)
                )
                delay(REMOTE_SEEK_PREVIEW_FRAME_MS)
            }
        }
    }

    fun finishRemoteSeek() {
        val direction = remoteSeekDirection
        if (direction == 0) return

        remoteSeekJob?.cancel()
        remoteSeekJob = null
        remoteSeekDirection = 0

        val targetPosition = remoteSeekPreviewMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
        interactionState.endGesture(PhoneGestureType.SEEK)
        interactionState.showFeedback(
            PhoneFeedbackOverlay.Seek(
                remoteSeekOverlayState(
                    targetPositionMs = targetPosition,
                    anchorPositionMs = remoteSeekAnchorMs,
                    direction = direction
                )
            ),
            keepControlsVisible = false
        )
        currentPositionMs = targetPosition
        controlState.seekTo(targetPosition)
    }

    BackHandler {
        if (playbackError != null || !handleTvBack()) {
            exitFullscreen()
        }
    }

    KeepScreenOnEffect(enabled = isTv)

    DisposableEffect(activity, manageOrientation) {
        if (activity == null) {
            onDispose { }
        } else if (!manageOrientation) {
            onDispose { }
        } else {
            val window = activity.window
            val previousOrientation = activity.requestedOrientation
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            onDispose {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    LaunchedEffect(transientMessage) {
        if (transientMessage != null) {
            delay(1_000)
            transientMessage = null
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            currentClockText = formatCurrentTime()
            delay(1_000L)
        }
    }

    LaunchedEffect(isTv, playbackError) {
        if (isTv && playbackError == null) {
            tvFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(videoUrl, episodeIndex) {
        remoteSeekJob?.cancel()
        remoteSeekJob = null
        remoteSeekDirection = 0
        activePanel = null
        displayedPanel = null
        interactionState.clearForNewMedia(initialPositionMs)
        transientMessage = null
    }

    LaunchedEffect(isTv, interactionState.controlsVisible, activePanel, displayedPanel, playbackError) {
        if (
            isTv &&
            interactionState.controlsVisible &&
            activePanel == null &&
            displayedPanel == null &&
            playbackError == null
        ) {
            delay(32)
            runCatching {
                tvPrimaryActionFocusRequester.requestFocus()
            }
        }
    }

    LaunchedEffect(activePanel) {
        if (activePanel != null) {
            displayedPanel = activePanel
        } else if (displayedPanel != null) {
            delay(180)
            displayedPanel = null
            interactionState.hideControls()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(
                if (isTv) {
                    Modifier
                        .focusRequester(tvFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (playbackError != null) return@onPreviewKeyEvent false
                            val controlsVisible = interactionState.controlsVisible || activePanel != null
                            if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) {
                                return@onPreviewKeyEvent when (event.nativeKeyEvent.keyCode) {
                                    android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                                    android.view.KeyEvent.KEYCODE_MEDIA_REWIND,
                                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                                    android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                        if (!controlsVisible && remoteSeekDirection != 0) {
                                            finishRemoteSeek()
                                            true
                                        } else {
                                            false
                                        }
                                    }

                                    else -> false
                                }
                            }
                            if (event.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                            when (event.nativeKeyEvent.keyCode) {
                                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                android.view.KeyEvent.KEYCODE_ENTER,
                                android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                                android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                                android.view.KeyEvent.KEYCODE_MEDIA_PLAY,
                                android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                    if (controlsVisible) {
                                        false
                                    } else {
                                        togglePlayPause()
                                        true
                                    }
                                }

                                android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                                android.view.KeyEvent.KEYCODE_MEDIA_REWIND -> {
                                    if (controlsVisible) {
                                        false
                                    } else {
                                        startRemoteSeek(-1)
                                        true
                                    }
                                }

                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                                android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                    if (controlsVisible) {
                                        false
                                    } else {
                                        startRemoteSeek(1)
                                        true
                                    }
                                }

                                android.view.KeyEvent.KEYCODE_DPAD_UP,
                                android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                                android.view.KeyEvent.KEYCODE_MENU -> {
                                    if (controlsVisible) {
                                        false
                                    } else {
                                        showTvControls()
                                        true
                                    }
                                }

                                else -> false
                            }
                        }
                } else {
                    Modifier
                }
            )
    ) {
        VideoPlayerView(
            modifier = Modifier.fillMaxSize(),
            videoUrl = videoUrl,
            requestHeaders = requestHeaders,
            mediaIdentity = if (retryCount == 0) mediaIdentity else "$mediaIdentity#retry-$retryCount",
            initialPositionMs = if (retryCount == 0) initialPositionMs else retryPosition,
            repeat = false,
            showController = false,
            enableNativeController = false,
            sharedSessionEnabled = true,
            sharedSessionReleaseDelayMs = 5_000L,
            controlState = controlState,
            onProgressChanged = { position, duration ->
                if (playbackError == null) {
                    currentPositionMs = position
                    durationMs = duration
                    if (!skipOutroCompleted) onProgressChangedExternal?.invoke(position, duration)
                    interactionState.syncPlaybackPosition(position)
                }
            },
            onPlaybackStateChanged = { state ->
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    playerReady = true
                }
            },
            onPlayingChanged = { playing ->
                isPlaying = playing
            },
            onPlaybackEnded = ::playNextEpisodeFromCurrent,
            onPlaybackError = { message ->
                playbackError = message
                activePanel = null
                remoteSeekJob?.cancel()
                remoteSeekDirection = 0
                interactionState.cancelGesture()
                interactionState.showControls()
            },
            onSurfaceTap = null
        )

        Text(
            text = currentClockText,
            color = AppTextPrimary,
            fontSize = if (isTv) 18.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = if (isTv) 18.dp else 14.dp,
                    end = if (isTv) 22.dp else 16.dp
                )
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.42f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )

        val seekFeedbackVisible = interactionState.gestureInProgress ||
            interactionState.isScrubbing ||
            interactionState.feedbackOverlay != null
        if (playbackError == null && !isPlaying && !seekFeedbackVisible && (!playerReady || isBuffering)) {
            PlayerLoadingOverlay(
                modifier = Modifier.matchParentSize()
            )
        }

        if (!isTv && playbackError == null) {
            PhoneFullscreenGestureLayer(
                modifier = Modifier.fillMaxSize(),
                resetKey = "$videoUrl#$episodeIndex",
                durationMs = durationMs,
                currentPositionMs = currentPositionMs,
                brightnessPercent = brightnessPercent,
                volumePercent = volumePercent,
                onSingleTap = ::toggleControls,
                onDoubleTap = ::togglePlayPause,
                onGestureStart = {
                    interactionState.beginGesture()
                    activePanel = null
                },
                onGestureEnd = { type ->
                    interactionState.endGesture(type)
                },
                onBrightnessChange = ::updateBrightnessPercent,
                onVolumeChange = ::updateVolumePercent,
                onSeekPreview = { positionMs, deltaMs, reachedEdgeMessage ->
                    interactionState.updateSeekPreview(
                        SeekOverlayState(
                            targetPositionMs = positionMs,
                            durationMs = durationMs,
                            deltaMs = deltaMs,
                            reachedEdgeMessage = reachedEdgeMessage
                        )
                    )
                },
                onSeekCommit = { state ->
                    currentPositionMs = state.targetPositionMs
                    interactionState.commitScrub(
                        positionMs = state.targetPositionMs,
                        keepControlsVisible = false
                    )
                    onProgressChangedExternal?.invoke(state.targetPositionMs, durationMs)
                    controlState.seekTo(state.targetPositionMs)
                },
                onBoundaryReached = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
        }

        PlayerFeedbackPopup(
            overlay = interactionState.feedbackOverlay,
            tvLayout = isTv
        )

        transientMessage?.let { message ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.56f))
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message,
                    color = AppTextPrimary,
                    fontSize = if (isTv) 18.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (playbackError == null && (interactionState.controlsVisible || activePanel != null || displayedPanel != null)) {
            val topBarHeight = if (isTv) 64.dp else 48.dp
            val progressBarHeight = if (isTv) 52.dp else 42.dp
            val bottomBarHeight = if (isTv) 44.dp else 52.dp
            val edgePadding = if (isTv) 10.dp else 16.dp
            val focusSafeInset = if (isTv) 6.dp else 0.dp
            val panel = activePanel

            if (panel == null && displayedPanel == null) {
                Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xE607101C), Color(0x6A07101C), Color.Transparent)
                        )
                    )
                    .padding(horizontal = edgePadding, vertical = 14.dp)
            ) {
                PlayerTopInlineBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topBarHeight),
                    title = compactTitle,
                    onBack = ::exitFullscreen,
                    large = isTv
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        if (!isTv) return@onPreviewKeyEvent false
                        if (event.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                        when (event.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_UP,
                            android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                            android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                            android.view.KeyEvent.KEYCODE_ENTER,
                            android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                            android.view.KeyEvent.KEYCODE_MENU -> refreshTvMenuInteraction()
                        }
                        when (event.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_BACK,
                            android.view.KeyEvent.KEYCODE_ESCAPE -> handleTvBack()
                            else -> false
                        }
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xB0060D16), Color(0xF2060D16))
                        )
                    )
                    .padding(
                        start = edgePadding,
                        top = if (isTv) 2.dp else 10.dp,
                        end = edgePadding,
                        bottom = if (isTv) 12.dp else 16.dp
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = focusSafeInset)
                        .height(progressBarHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPlayerTime(currentDisplayPosition),
                        color = AppTextPrimary,
                        fontSize = if (isTv) 18.sp else 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(if (isTv) 18.dp else 12.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isTv) 28.dp else 20.dp)
                            .onSizeChanged { progressBarWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                            .pointerInput(durationMs, progressBarWidthPx) {
                                detectTapGestures { offset ->
                                    if (durationMs <= 0) return@detectTapGestures
                                    seekToPosition(positionFromBarOffset(offset.x))
                                }
                            }
                            .pointerInput(durationMs, progressBarWidthPx) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        if (durationMs <= 0) return@detectDragGestures
                                        interactionState.updateScrubPosition(positionFromBarOffset(offset.x))
                                    },
                                onDragCancel = {
                                    interactionState.cancelScrub()
                                },
                                onDragEnd = {
                                    if (interactionState.isScrubbing) {
                                        seekToPosition(interactionState.scrubPositionMs)
                                    }
                                    }
                                ) { change, _ ->
                                    if (durationMs <= 0) return@detectDragGestures
                                    interactionState.updateScrubPosition(positionFromBarOffset(change.position.x))
                                }
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isTv) 8.dp else 4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(if (isTv) 8.dp else 4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(BrandPrimary, Color(0xFF7CC7FF))
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth(progress),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isTv) 14.dp else 10.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0xFFE7ECF3))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(if (isTv) 18.dp else 12.dp))
                    Text(
                        text = if (durationMs > 0) formatPlayerTime(durationMs) else "--:--",
                        color = AppTextPrimary,
                        fontSize = if (isTv) 18.sp else 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = focusSafeInset)
                        .height(bottomBarHeight),
                    horizontalArrangement = Arrangement.spacedBy(if (isTv) 10.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (episodeCount > 1) {
                        PlayerActionChip(
                            modifier = if (isTv) Modifier.focusRequester(tvPrimaryActionFocusRequester) else Modifier,
                            text = "下一集",
                            large = isTv,
                            enabled = episodeIndex < episodeCount - 1,
                            onClick = {
                                if (episodeIndex < episodeCount - 1) {
                                    switchEpisode(episodeIndex + 1)
                                }
                            }
                        )
                        PlayerActionChip(
                            text = "上一集",
                            large = isTv,
                            enabled = episodeIndex > 0,
                            onClick = {
                                if (episodeIndex > 0) {
                                    switchEpisode(episodeIndex - 1)
                                }
                            }
                        )
                    } else {
                        PlayerActionChip(
                            modifier = if (isTv) Modifier.focusRequester(tvPrimaryActionFocusRequester) else Modifier,
                            text = "重播",
                            large = isTv,
                            onClick = ::replayFromStart
                        )
                        PlayerActionChip(
                            text = "倍速 ${formatPlaybackSpeed(playbackSpeed)}",
                            large = isTv,
                            onClick = { togglePanel(BottomPanel.SPEED) }
                        )
                    }
                    if (episodeCount > 1) {
                        PlayerActionChip(
                            text = "重播",
                            large = isTv,
                            onClick = ::replayFromStart
                        )
                        PlayerActionChip(
                            text = "倍速 ${formatPlaybackSpeed(playbackSpeed)}",
                            large = isTv,
                            onClick = { togglePanel(BottomPanel.SPEED) }
                        )
                    }
                    PlayerActionChip(
                        text = "清晰度 $selectedQuality",
                        large = isTv,
                        onClick = { togglePanel(BottomPanel.QUALITY) }
                    )
                    PlayerActionChip(
                        text = if (isTv) "选集 第${episodeIndex + 1}集" else "选集 C-${episodeIndex + 1}",
                        large = isTv,
                        onClick = { togglePanel(BottomPanel.EPISODE) }
                    )
                    if (isTv && episodeSkipSettings != null && onSaveEpisodeSkipSettings != null) {
                        PlayerActionChip(text = "播放设置", large = true, onClick = ::openPlaybackSettings)
                    }
                }
            }

            }

            AnimatedVisibility(
                visible = panel != null,
                enter = slideInVertically(animationSpec = tween(220)) { it / 2 } + fadeIn(animationSpec = tween(160)),
                exit = slideOutVertically(animationSpec = tween(180)) { it / 2 } + fadeOut(animationSpec = tween(120)),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        if (!isTv) return@onPreviewKeyEvent false
                        if (event.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN) {
                            return@onPreviewKeyEvent false
                        }
                        when (event.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_BACK,
                            android.view.KeyEvent.KEYCODE_ESCAPE -> {
                                closePanel()
                                true
                            }

                            else -> {
                                refreshTvMenuInteraction()
                                false
                            }
                        }
                    }
                    .padding(
                        start = edgePadding,
                        end = edgePadding,
                        bottom = if (isTv) 24.dp else 16.dp
                    )
            ) {
                displayedPanel?.let { shownPanel ->
                    PlayerOptionPanel(
                        panel = shownPanel,
                        isTv = isTv,
                        playbackSpeed = playbackSpeed,
                        selectedQuality = selectedQuality,
                        episodeIndex = episodeIndex,
                        episodeCount = episodeCount,
                        episodes = episodes,
                        speedOptions = speedOptions,
                        qualityOptions = qualityOptions,
                        speedFocusRequester = tvSpeedPanelFocusRequester,
                        qualityFocusRequester = tvQualityPanelFocusRequester,
                        episodeFocusRequester = tvEpisodePanelFocusRequester,
                        episodeSkipSettings = episodeSkipSettings,
                        settingsPositionMs = settingsPositionMs,
                        settingsOpenCount = settingsOpenCount,
                        durationMs = durationMs,
                        onSaveEpisodeSkipSettings = { settings ->
                            introChecked = true
                            if (settings.outroRemainingMs != null) {
                                editedOutro = settings
                            } else {
                                editedOutro = null
                            }
                            onSaveEpisodeSkipSettings?.invoke(settings)
                            closePanel()
                            showTransientMessage("片头片尾已保存")
                        },
                        onClose = ::closePanel,
                        onSpeedSelected = { speed ->
                            playbackSpeed = speed
                            controlState.setSpeed(speed)
                            showTransientMessage("倍速 ${formatPlaybackSpeed(speed)}")
                            closePanel()
                        },
                        onQualitySelected = { quality ->
                            selectedQuality = quality
                            showTransientMessage("清晰度 $quality")
                            closePanel()
                        },
                        onEpisodeSelected = { targetIndex ->
                            if (targetIndex != episodeIndex) {
                                switchEpisode(targetIndex)
                            } else {
                                closePanel()
                            }
                        }
                    )
                }
            }
        }

        playbackError?.let { error ->
            FullscreenPlaybackFailure(
                message = error,
                isTv = isTv,
                modifier = Modifier.align(Alignment.Center),
                onRetry = {
                    onProgressChangedExternal?.invoke(currentPositionMs, durationMs)
                    if (onRetryPlayback != null) {
                        onRetryPlayback(currentPositionMs)
                    } else {
                        retryPosition = currentPositionMs
                        retryCount++
                        playbackError = null
                        playerReady = false
                    }
                },
                onSwitchLine = onSwitchLine?.let { switch ->
                    {
                        onProgressChangedExternal?.invoke(currentPositionMs, durationMs)
                        switch(currentPositionMs)
                    }
                },
                onSearch = onSearchSameTitle?.let { search ->
                    {
                        onProgressChangedExternal?.invoke(currentPositionMs, durationMs)
                        search(currentPositionMs)
                    }
                },
                onBack = ::exitFullscreen
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun PlayerOptionPanel(
    panel: BottomPanel,
    isTv: Boolean,
    playbackSpeed: Float,
    selectedQuality: String,
    episodeIndex: Int,
    episodeCount: Int,
    episodes: List<Episode>,
    speedOptions: List<Float>,
    qualityOptions: List<String>,
    speedFocusRequester: FocusRequester,
    qualityFocusRequester: FocusRequester,
    episodeFocusRequester: FocusRequester,
    episodeSkipSettings: EpisodeSkipSettings?,
    settingsPositionMs: Long,
    settingsOpenCount: Int,
    durationMs: Long,
    onSaveEpisodeSkipSettings: (EpisodeSkipSettings) -> Unit,
    onClose: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onQualitySelected: (String) -> Unit,
    onEpisodeSelected: (Int) -> Unit
) {
    LaunchedEffect(isTv, panel, playbackSpeed, selectedQuality, episodeIndex) {
        if (!isTv) return@LaunchedEffect
        delay(32)
        runCatching {
            when (panel) {
                BottomPanel.SPEED -> speedFocusRequester.requestFocus()
                BottomPanel.QUALITY -> qualityFocusRequester.requestFocus()
                BottomPanel.EPISODE -> episodeFocusRequester.requestFocus()
                BottomPanel.PLAYBACK_SETTINGS -> Unit
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        AppSurfaceVariant.copy(alpha = 0.72f),
                        Color(0xB00B1727)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (panel) {
                    BottomPanel.SPEED -> "倍速设置"
                    BottomPanel.QUALITY -> "清晰度设置"
                    BottomPanel.EPISODE -> "选集"
                    BottomPanel.PLAYBACK_SETTINGS -> "片头片尾"
                },
                color = AppTextPrimary,
                fontSize = if (isTv) 18.sp else 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            PlayerPanelCloseButton(
                large = isTv,
                label = if (panel == BottomPanel.PLAYBACK_SETTINGS) "取消" else "收起",
                onClick = onClose
            )
        }
        when (panel) {
            BottomPanel.SPEED -> {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (option in speedOptions) {
                        PlayerPanelChip(
                            modifier = if (isTv && option == playbackSpeed) Modifier.focusRequester(speedFocusRequester) else Modifier,
                            text = formatPlaybackSpeed(option),
                            selected = option == playbackSpeed,
                            large = isTv,
                            onClick = { onSpeedSelected(option) }
                        )
                    }
                }
            }

            BottomPanel.QUALITY -> {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (option in qualityOptions) {
                        PlayerPanelChip(
                            modifier = if (isTv && option == selectedQuality) Modifier.focusRequester(qualityFocusRequester) else Modifier,
                            text = option,
                            selected = option == selectedQuality,
                            large = isTv,
                            onClick = { onQualitySelected(option) }
                        )
                    }
                }
            }

            BottomPanel.PLAYBACK_SETTINGS -> {
                if (episodeSkipSettings != null && durationMs > 0L) {
                    key(settingsOpenCount) {
                        EpisodeSkipEditor(episodeSkipSettings, settingsPositionMs, durationMs, onSaveEpisodeSkipSettings)
                    }
                }
            }

            BottomPanel.EPISODE -> {
                GroupedEpisodes(episodeCount, episodeIndex, phoneLayout = !isTv, resetKey = episodes) { visibleIndices ->
                    val episodeScrollState = rememberScrollState()
                    val currentEpisodeRequester = remember { BringIntoViewRequester() }
                    LaunchedEffect(visibleIndices) {
                        episodeScrollState.scrollTo(0)
                        if (!isTv && episodeIndex in visibleIndices) {
                            withFrameNanos { }
                            currentEpisodeRequester.bringIntoView()
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = if (isTv) 244.dp else 204.dp)
                            .verticalScroll(episodeScrollState)
                    ) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (index in visibleIndices) {
                                val focusModifier = when {
                                    index != episodeIndex -> Modifier
                                    isTv -> Modifier.focusRequester(episodeFocusRequester)
                                    else -> Modifier.bringIntoViewRequester(currentEpisodeRequester)
                                }
                                PlayerPanelChip(
                                    modifier = Modifier
                                        .width(if (isTv) 128.dp else 104.dp)
                                        .height(if (isTv) 48.dp else 38.dp)
                                        .then(focusModifier),
                                    text = episodes.getOrNull(index)?.displayLabel(index) ?: "第${index + 1}集",
                                    selected = index == episodeIndex,
                                    large = isTv,
                                    fillContainer = true,
                                    onClick = { onEpisodeSelected(index) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun EpisodeSkipEditor(
    settings: EpisodeSkipSettings,
    capturedPositionMs: Long,
    durationMs: Long,
    onSave: (EpisodeSkipSettings) -> Unit
) {
    var editingOutro by remember { mutableStateOf(false) }
    var introEnd by remember { mutableStateOf(settings.introEndMs) }
    var outroStart by remember { mutableStateOf(settings.outroRemainingMs?.let { durationMs - it }) }
    val firstItem = remember { FocusRequester() }
    LaunchedEffect(firstItem) {
        withFrameNanos { }
        firstItem.requestFocus()
    }
    val selectedTime = if (editingOutro) outroStart else introEnd
    fun setTime(timeMs: Long?) {
        if (editingOutro) outroStart = timeMs else introEnd = timeMs
    }
    fun adjust(deltaMs: Long) {
        val start = selectedTime ?: if (editingOutro) durationMs else 0L
        setTime((start + deltaMs).coerceIn(0L, durationMs))
    }
    val normalizedIntro = introEnd?.takeUnless { it == 0L }
    val normalizedOutro = outroStart?.takeUnless { it == durationMs }
    val draft = EpisodeSkipSettings(normalizedIntro, normalizedOutro?.let { durationMs - it })
    val canSave = draft.playbackRange(durationMs, hasNextEpisode = true) != null

    Column(
        modifier = Modifier.fillMaxWidth().focusGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlayerPanelChip(
                modifier = Modifier.focusRequester(firstItem),
                text = "片头结束  ${skipTimeLabel(introEnd)}",
                selected = !editingOutro,
                large = true,
                onClick = { editingOutro = false }
            )
            PlayerPanelChip(
                text = "片尾开始  ${skipTimeLabel(outroStart)}",
                selected = editingOutro,
                large = true,
                onClick = { editingOutro = true }
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayerActionChip(text = "用当前位置 ${formatPlayerTime(capturedPositionMs)}", large = true,
                onClick = { setTime(capturedPositionMs) })
            PlayerActionChip(text = "−10 秒", large = true, onClick = { adjust(-10_000L) })
            PlayerActionChip(text = "+10 秒", large = true, onClick = { adjust(10_000L) })
            PlayerActionChip(text = "关闭此项", large = true, onClick = { setTime(null) })
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PlayerPanelChip(text = "保存", selected = canSave, large = true, onClick = {
                if (canSave) onSave(draft)
            })
            Text(
                text = if (canSave) "应用到本剧" else "请在片头结束与片尾开始之间留出至少 1 秒",
                color = if (canSave) AppTextMuted else AppTextPrimary,
                fontSize = 14.sp
            )
        }
    }
}

private fun skipTimeLabel(positionMs: Long?): String =
    when {
        positionMs == null -> "关闭"
        positionMs < 0L -> "超出本集时长"
        else -> formatPlayerTime(positionMs)
    }

@Composable
private fun PlayerPanelCloseButton(
    large: Boolean,
    label: String,
    onClick: () -> Unit
) {
    TvSurface(onClick = onClick) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF101A26).copy(alpha = 0.84f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(8.dp))
                .padding(horizontal = if (large) 16.dp else 12.dp, vertical = if (large) 10.dp else 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color(0xFFE7ECF3),
                fontSize = if (large) 15.sp else 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PlayerPanelChip(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    large: Boolean,
    fillContainer: Boolean = false,
    onClick: () -> Unit
) {
    TvSurface(
        onClick = onClick,
        modifier = modifier
    ) {
        val background = if (selected) {
            BrandPrimary.copy(alpha = 0.20f)
        } else {
            Color(0xFF111B28).copy(alpha = 0.82f)
        }
        val borderColor = if (selected) {
            BrandPrimary.copy(alpha = 0.48f)
        } else {
            Color.White.copy(alpha = 0.10f)
        }
        Box(
            modifier = (if (fillContainer) Modifier.fillMaxSize() else Modifier)
                .clip(RoundedCornerShape(8.dp))
                .background(background)
                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
                .padding(horizontal = if (large) 18.dp else 14.dp, vertical = if (large) 12.dp else 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) BrandPrimary else AppTextPrimary,
                fontSize = if (large) 15.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlayerActionChip(
    modifier: Modifier = Modifier,
    text: String,
    large: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TvSurface(
        onClick = { if (enabled) onClick() },
        modifier = modifier
    ) {
        val background = if (enabled) {
            Color(0xFF101A26).copy(alpha = 0.84f)
        } else {
            Color(0xFF101A26).copy(alpha = 0.42f)
        }
        val borderColor = if (enabled) {
            Color.White.copy(alpha = 0.12f)
        } else {
            Color.White.copy(alpha = 0.05f)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(background)
                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
                .padding(horizontal = if (large) 14.dp else 12.dp, vertical = if (large) 9.dp else 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (enabled) Color(0xFFE7ECF3) else AppTextPrimary.copy(alpha = 0.35f),
                fontSize = if (large) 14.sp else 12.sp,
                maxLines = 1
            )
        }
    }
}

private enum class PlayerIcon {
    Back
}

private const val REMOTE_SEEK_TAP_STEP_MS = 10_000L
private const val REMOTE_SEEK_HOLD_DELAY_MS = 250L
private const val REMOTE_SEEK_BASE_SPEED_MS_PER_SECOND = 40_000L
private const val REMOTE_SEEK_ACCELERATION_MS_PER_SECOND_SQUARED = 30_000L
private const val REMOTE_SEEK_PREVIEW_FRAME_MS = 50L

private fun remoteSeekDeltaMs(elapsedMs: Long): Long {
    val elapsedSeconds = elapsedMs.coerceAtLeast(0L) / 1_000.0
    val acceleratedDistance =
        REMOTE_SEEK_BASE_SPEED_MS_PER_SECOND * elapsedSeconds +
            REMOTE_SEEK_ACCELERATION_MS_PER_SECOND_SQUARED * elapsedSeconds * elapsedSeconds / 2.0
    return REMOTE_SEEK_TAP_STEP_MS + acceleratedDistance.roundToLong()
}

private fun formatCurrentTime(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

@Composable
private fun IconOnlyButton(
    icon: PlayerIcon,
    large: Boolean,
    onClick: () -> Unit
) {
    TvSurface(onClick = onClick) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .size(if (large) 28.dp else 22.dp),
            contentAlignment = Alignment.Center
        ) {
            when (icon) {
                PlayerIcon.Back -> ArrowBackIcon(color = AppTextPrimary)
            }
        }
    }
}

@Composable
internal fun PlayerTopInlineBar(
    modifier: Modifier = Modifier,
    title: String,
    onBack: () -> Unit,
    large: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBack)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = AppTextPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "返回", color = AppTextSecondary, fontSize = 13.sp)
        }
        Text(
            text = title,
            color = AppTextPrimary,
            fontSize = if (large) 20.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f)
        )
    }
}

@Composable
private fun ArrowBackIcon(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = size.minDimension * 0.12f
        val midY = size.height / 2f
        val startX = size.width * 0.72f
        val endX = size.width * 0.24f
        drawLine(
            color = color,
            start = Offset(startX, size.height * 0.2f),
            end = Offset(endX, midY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(endX, midY),
            end = Offset(startX, size.height * 0.8f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun InfoBadge(
    text: String,
    highlight: Boolean = false,
    large: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (highlight) BrandPrimary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
            .padding(horizontal = if (large) 12.dp else 10.dp, vertical = if (large) 8.dp else 6.dp)
    ) {
        Text(
            text = text,
            color = if (highlight) BrandPrimary else AppTextPrimary,
            fontSize = if (large) 14.sp else 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatPlaybackSpeed(speed: Float): String = when (speed) {
    1.0f -> "x1.0"
    1.25f -> "x1.25"
    1.5f -> "x1.5"
    2.0f -> "x2.0"
    else -> "x$speed"
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
