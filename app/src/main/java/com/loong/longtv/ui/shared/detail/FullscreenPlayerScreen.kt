package com.loong.longtv.ui.shared.detail

import android.app.Activity
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.navigation.NavController
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.loong.longtv.ui.shared.widgets.AppButton
import com.loong.longtv.ui.shared.widgets.TvSurface
import com.loong.longtv.ui.theme.AppCardOverlay
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.theme.BrandPrimaryDark
import com.loong.player.PlayerInteractionDefaults
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private enum class BottomPanel {
    SPEED,
    QUALITY,
    EPISODE
}

internal enum class PhoneGestureType {
    BRIGHTNESS,
    VOLUME,
    SEEK
}

internal enum class OverlayIcon {
    BRIGHTNESS,
    VOLUME,
    PLAY,
    PAUSE
}

internal data class AdjustmentOverlayState(
    val type: PhoneGestureType,
    val percent: Int
)

internal data class SeekOverlayState(
    val targetPositionMs: Long,
    val durationMs: Long,
    val deltaMs: Long,
    val reachedEdgeMessage: String? = null
)

internal sealed interface PhoneFeedbackOverlay {
    data class Adjustment(val state: AdjustmentOverlayState) : PhoneFeedbackOverlay
    data class Seek(val state: SeekOverlayState) : PhoneFeedbackOverlay
    data class PlayPause(val isPlaying: Boolean) : PhoneFeedbackOverlay
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
    manageOrientation: Boolean = true,
    onExitFullscreen: ((Long) -> Unit)? = null,
    onSwitchEpisode: ((Int) -> Unit)? = null,
    onPlaybackEnded: (() -> Unit)? = null,
    onProgressChangedExternal: ((Long, Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val audioManager = remember(context) { context.getSystemService(AUDIO_SERVICE) as AudioManager }
    val haptic = LocalHapticFeedback.current
    val controlState = rememberPlayerControlState()
    val speedOptions = remember { listOf(1.0f, 1.25f, 1.5f, 2.0f) }
    val qualityOptions = remember { listOf("跟随片源", "高清 1080P", "超清 4K") }
    var currentPositionMs by remember(initialPositionMs) { mutableLongStateOf(initialPositionMs) }
    var durationMs by remember(title, videoUrl) { mutableLongStateOf(0L) }
    var overlayVisible by remember(title, videoUrl) { mutableStateOf(false) }
    var isPlaying by remember(title, videoUrl) { mutableStateOf(false) }
    var isBuffering by remember(title, videoUrl) { mutableStateOf(false) }
    var playerReady by remember(title, videoUrl) { mutableStateOf(false) }
    var playbackError by remember(title, videoUrl) { mutableStateOf<String?>(null) }
    var interactionVersion by remember(title, videoUrl) { mutableIntStateOf(0) }
    var playbackSpeed by remember(title, videoUrl) { mutableFloatStateOf(1.0f) }
    var transientMessage by remember(title, videoUrl) { mutableStateOf<String?>(null) }
    var progressBarWidthPx by remember(title, videoUrl) { mutableFloatStateOf(1f) }
    var isScrubbing by remember(title, videoUrl) { mutableStateOf(false) }
    var scrubPositionMs by remember(title, videoUrl) { mutableLongStateOf(initialPositionMs) }
    var selectedQuality by remember(title, videoUrl) { mutableStateOf(qualityOptions[0]) }
    var activePanel by remember(title, videoUrl) { mutableStateOf<BottomPanel?>(null) }
    var gestureInProgress by remember(title, videoUrl) { mutableStateOf(false) }
    var brightnessLevel by remember(title, videoUrl) {
        mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f)
    }
    var volumePercent by remember(title, videoUrl) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        mutableIntStateOf((audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f / maxVolume).roundToLong().toInt())
    }
    var phoneFeedbackOverlay by remember(title, videoUrl) { mutableStateOf<PhoneFeedbackOverlay?>(null) }
    var phoneFeedbackVersion by remember(title, videoUrl) { mutableIntStateOf(0) }
    val tvFocusRequester = remember(title, videoUrl) { FocusRequester() }
    val tvPrimaryActionFocusRequester = remember(title, videoUrl) { FocusRequester() }
    val tvSpeedPanelFocusRequester = remember(title, videoUrl) { FocusRequester() }
    val tvQualityPanelFocusRequester = remember(title, videoUrl) { FocusRequester() }
    val tvEpisodePanelFocusRequester = remember(title, videoUrl) { FocusRequester() }

    val episodeLabel = if (episodeCount > 0) "第${episodeIndex + 1}集" else "单集播放"
    val compactTitle = buildString {
        append(title.ifBlank { "全屏播放" })
        if (episodeCount > 0) append(" · ").append(episodeLabel)
    }
    val currentDisplayPosition = if (isScrubbing) scrubPositionMs else currentPositionMs
    val progress = playbackProgress(currentDisplayPosition, durationMs)
    val brightnessPercent = (brightnessLevel * 100f).roundToInt().coerceIn(0, 100)
    val overlayAutoHideMs = PlayerInteractionDefaults.ControlsAutoHideMs

    fun registerInteraction() {
        overlayVisible = true
        interactionVersion += 1
    }

    fun showControls() {
        overlayVisible = true
        interactionVersion += 1
    }

    fun hideControls() {
        activePanel = null
        overlayVisible = false
    }

    fun toggleControls() {
        if (overlayVisible || activePanel != null) {
            hideControls()
        } else {
            showControls()
        }
    }

    fun showPhoneFeedback(
        overlay: PhoneFeedbackOverlay,
        keepControlsVisible: Boolean = true
    ) {
        phoneFeedbackOverlay = overlay
        phoneFeedbackVersion += 1
        if (keepControlsVisible) {
            registerInteraction()
        }
    }

    fun showTransientMessage(message: String) {
        transientMessage = message
        registerInteraction()
    }

    fun closePanel() {
        activePanel = null
        registerInteraction()
    }

    fun seekToPosition(positionMs: Long) {
        val duration = durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE
        currentPositionMs = positionMs.coerceIn(0L, duration)
        if (!isScrubbing) {
            scrubPositionMs = currentPositionMs
        }
        onProgressChangedExternal?.invoke(currentPositionMs, durationMs)
        controlState.seekTo(positionMs)
        registerInteraction()
    }

    fun previewSeekTo(positionMs: Long, deltaMs: Long, reachedEdgeMessage: String?) {
        val duration = durationMs.takeIf { it > 0 } ?: return
        isScrubbing = true
        scrubPositionMs = positionMs.coerceIn(0L, duration)
        showPhoneFeedback(
            PhoneFeedbackOverlay.Seek(
                SeekOverlayState(
                    targetPositionMs = scrubPositionMs,
                    durationMs = duration,
                    deltaMs = deltaMs,
                    reachedEdgeMessage = reachedEdgeMessage
                )
            )
        )
    }

    fun commitPreviewSeek() {
        if (!isScrubbing) return
        seekToPosition(scrubPositionMs)
        isScrubbing = false
        phoneFeedbackVersion += 1
    }

    fun togglePlayPause() {
        val targetIsPlaying = !isPlaying
        controlState.togglePlayPause()
        showPhoneFeedback(
            overlay = PhoneFeedbackOverlay.PlayPause(targetIsPlaying),
            keepControlsVisible = false
        )
    }

    fun showTvControls() {
        showControls()
    }

    fun refreshTvMenuInteraction() {
        if (isTv && overlayVisible) {
            interactionVersion += 1
        }
    }

    fun togglePanel(panel: BottomPanel) {
        activePanel = if (activePanel == panel) null else panel
        registerInteraction()
    }

    fun updateBrightnessPercent(targetPercent: Int) {
        val window = activity?.window ?: return
        val clamped = targetPercent.coerceIn(0, 100)
        brightnessLevel = clamped / 100f
        window.attributes = window.attributes.apply {
            screenBrightness = brightnessLevel
        }
        showPhoneFeedback(
            PhoneFeedbackOverlay.Adjustment(
                AdjustmentOverlayState(
                    type = PhoneGestureType.BRIGHTNESS,
                    percent = clamped
                )
            )
        )
    }

    fun updateVolumePercent(targetPercent: Int) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val clamped = targetPercent.coerceIn(0, 100)
        val targetVolume = (maxVolume * (clamped / 100f)).roundToInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        volumePercent = clamped
        showPhoneFeedback(
            PhoneFeedbackOverlay.Adjustment(
                AdjustmentOverlayState(
                    type = PhoneGestureType.VOLUME,
                    percent = clamped
                )
            )
        )
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

            overlayVisible -> {
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
        activePanel = null
        phoneFeedbackOverlay = null
        gestureInProgress = false
        overlayVisible = true
        interactionVersion += 1
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
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.set("fullscreen_position", 0L)
        navController.popBackStack()
    }

    fun playNextEpisodeFromCurrent() {
        val targetIndex = episodeIndex + 1
        if (targetIndex in 0 until episodeCount) {
            switchEpisode(targetIndex)
        } else {
            onPlaybackEnded?.invoke()
        }
    }

    fun positionFromBarOffset(offsetX: Float): Long {
        if (durationMs <= 0 || progressBarWidthPx <= 0f) return currentPositionMs
        val percent = (offsetX / progressBarWidthPx).coerceIn(0f, 1f)
        return (durationMs * percent).roundToLong()
    }

    fun seekByRemote(offsetMs: Long) {
        val duration = durationMs.takeIf { it > 0 } ?: return
        val targetPosition = (currentPositionMs + offsetMs).coerceIn(0L, duration)
        val reachedEdgeMessage = when {
            targetPosition == 0L && offsetMs < 0L -> "已至开头"
            targetPosition == duration && offsetMs > 0L -> "已至结尾"
            else -> null
        }
        controlState.seekBy(offsetMs)
        showPhoneFeedback(
            overlay = PhoneFeedbackOverlay.Seek(
                SeekOverlayState(
                    targetPositionMs = targetPosition,
                    durationMs = duration,
                    deltaMs = targetPosition - currentPositionMs,
                    reachedEdgeMessage = reachedEdgeMessage
                )
            ),
            keepControlsVisible = false
        )
    }

    BackHandler {
        if (!handleTvBack()) {
            exitFullscreen()
        }
    }

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
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    LaunchedEffect(
        overlayVisible,
        interactionVersion,
        playbackError,
        isBuffering,
        overlayAutoHideMs,
        gestureInProgress,
        activePanel
    ) {
        if (
            overlayVisible &&
            playbackError == null &&
            !isBuffering &&
            !gestureInProgress &&
            activePanel == null
        ) {
            delay(overlayAutoHideMs)
            overlayVisible = false
        }
    }

    LaunchedEffect(transientMessage) {
        if (transientMessage != null) {
            delay(1_000)
            transientMessage = null
        }
    }

    LaunchedEffect(phoneFeedbackOverlay, phoneFeedbackVersion, gestureInProgress) {
        val current = phoneFeedbackOverlay ?: return@LaunchedEffect
        val timeout = when (current) {
            is PhoneFeedbackOverlay.Adjustment -> PlayerInteractionDefaults.FeedbackAutoHideMs
            is PhoneFeedbackOverlay.Seek -> if (gestureInProgress) null else PlayerInteractionDefaults.SeekFeedbackAutoHideMs
            is PhoneFeedbackOverlay.PlayPause -> PlayerInteractionDefaults.PlayPauseFeedbackAutoHideMs
        }
        if (timeout == null) return@LaunchedEffect
        delay(timeout)
        if (phoneFeedbackOverlay == current) {
            phoneFeedbackOverlay = null
        }
    }

    LaunchedEffect(gestureInProgress, phoneFeedbackVersion) {
        if (gestureInProgress || phoneFeedbackOverlay !is PhoneFeedbackOverlay.Seek) {
            return@LaunchedEffect
        }
        delay(PlayerInteractionDefaults.SeekFeedbackAutoHideMs)
        if (phoneFeedbackOverlay is PhoneFeedbackOverlay.Seek) {
            phoneFeedbackOverlay = null
        }
    }

    LaunchedEffect(isTv, playbackError) {
        if (isTv && playbackError == null) {
            tvFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(videoUrl, episodeIndex) {
        activePanel = null
        gestureInProgress = false
        isScrubbing = false
        phoneFeedbackOverlay = null
        transientMessage = null
    }

    LaunchedEffect(isTv, overlayVisible, activePanel, playbackError) {
        if (isTv && overlayVisible && activePanel == null && playbackError == null) {
            tvPrimaryActionFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(isTv, activePanel) {
        if (!isTv) return@LaunchedEffect
        when (activePanel) {
            BottomPanel.SPEED -> tvSpeedPanelFocusRequester.requestFocus()
            BottomPanel.QUALITY -> tvQualityPanelFocusRequester.requestFocus()
            BottomPanel.EPISODE -> tvEpisodePanelFocusRequester.requestFocus()
            null -> Unit
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
                            if (event.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                            val controlsVisible = overlayVisible || activePanel != null
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
                                        seekByRemote(-10_000L)
                                        true
                                    }
                                }

                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                                android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                    if (controlsVisible) {
                                        false
                                    } else {
                                        seekByRemote(10_000L)
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
            initialPositionMs = initialPositionMs,
            repeat = false,
            showController = false,
            enableNativeController = false,
            sharedSessionEnabled = true,
            sharedSessionReleaseDelayMs = 5_000L,
            controlState = controlState,
            onProgressChanged = { position, duration ->
                currentPositionMs = position
                durationMs = duration
                onProgressChangedExternal?.invoke(position, duration)
                if (!isScrubbing) {
                    scrubPositionMs = position
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
                overlayVisible = true
            },
            onSurfaceTap = null
        )

        val seekFeedbackVisible = gestureInProgress ||
            isScrubbing ||
            phoneFeedbackOverlay is PhoneFeedbackOverlay.Seek
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
                    gestureInProgress = true
                    activePanel = null
                    registerInteraction()
                },
                onGestureEnd = { type ->
                    gestureInProgress = false
                    when (type) {
                        PhoneGestureType.BRIGHTNESS,
                        PhoneGestureType.VOLUME -> phoneFeedbackVersion += 1

                        PhoneGestureType.SEEK -> {
                            commitPreviewSeek()
                            phoneFeedbackVersion += 1
                        }
                    }
                },
                onBrightnessChange = ::updateBrightnessPercent,
                onVolumeChange = ::updateVolumePercent,
                onSeekPreview = ::previewSeekTo,
                onBoundaryReached = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
        }

        phoneFeedbackOverlay?.let { overlay ->
            when (overlay) {
                is PhoneFeedbackOverlay.Adjustment -> AdjustmentFeedbackOverlay(
                    modifier = Modifier.align(Alignment.Center),
                    type = overlay.state.type,
                    percent = overlay.state.percent
                )
                is PhoneFeedbackOverlay.Seek -> SeekFeedbackOverlay(
                    modifier = Modifier.align(Alignment.Center),
                    state = overlay.state
                )
                is PhoneFeedbackOverlay.PlayPause -> PlayPauseFeedbackOverlay(
                    modifier = Modifier.align(Alignment.Center),
                    isPlaying = overlay.isPlaying
                )
            }
        }

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

        if (overlayVisible) {
            val topBarHeight = if (isTv) 64.dp else 48.dp
            val progressBarHeight = if (isTv) 72.dp else 42.dp
            val bottomBarHeight = if (isTv) 80.dp else 52.dp
            val edgePadding = if (isTv) 28.dp else 16.dp

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
                    .padding(horizontal = edgePadding, vertical = if (isTv) 12.dp else 10.dp)
            ) {
                val panel = activePanel
                if (panel != null) {
                    PlayerOptionPanel(
                        panel = panel,
                        isTv = isTv,
                        playbackSpeed = playbackSpeed,
                        selectedQuality = selectedQuality,
                        episodeIndex = episodeIndex,
                        episodeCount = episodeCount,
                        speedOptions = speedOptions,
                        qualityOptions = qualityOptions,
                        speedFocusRequester = tvSpeedPanelFocusRequester,
                        qualityFocusRequester = tvQualityPanelFocusRequester,
                        episodeFocusRequester = tvEpisodePanelFocusRequester,
                        onClose = ::closePanel,
                        onSpeedSelected = { speed ->
                            playbackSpeed = speed
                            controlState.setSpeed(speed)
                            showTransientMessage("倍速 ${formatPlaybackSpeed(speed)}")
                            activePanel = null
                        },
                        onQualitySelected = { quality ->
                            selectedQuality = quality
                            showTransientMessage("清晰度 $quality")
                            activePanel = null
                        },
                        onEpisodeSelected = { targetIndex ->
                            if (targetIndex != episodeIndex) {
                                switchEpisode(targetIndex)
                            } else {
                                closePanel()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                                    scrubPositionMs = positionFromBarOffset(offset.x)
                                    seekToPosition(scrubPositionMs)
                                }
                            }
                            .pointerInput(durationMs, progressBarWidthPx) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        if (durationMs <= 0) return@detectDragGestures
                                        isScrubbing = true
                                        scrubPositionMs = positionFromBarOffset(offset.x)
                                        registerInteraction()
                                    },
                                onDragCancel = {
                                    isScrubbing = false
                                    registerInteraction()
                                },
                                onDragEnd = {
                                    if (isScrubbing) {
                                        seekToPosition(scrubPositionMs)
                                    }
                                    isScrubbing = false
                                    }
                                ) { change, _ ->
                                    if (durationMs <= 0) return@detectDragGestures
                                    scrubPositionMs = positionFromBarOffset(change.position.x)
                                    registerInteraction()
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
                        .padding(start = if (isTv) 10.dp else 0.dp, end = if (isTv) 8.dp else 0.dp)
                        .height(bottomBarHeight),
                    horizontalArrangement = Arrangement.spacedBy(if (isTv) 12.dp else 8.dp),
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
                            text = "倍速 ${formatPlaybackSpeed(playbackSpeed)}",
                            large = isTv,
                            onClick = { togglePanel(BottomPanel.SPEED) }
                        )
                    }
                    if (episodeCount > 1) {
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
                }
            }
        }

        if (playbackError != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppCardOverlay)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = playbackError ?: "播放失败",
                    color = AppError,
                    fontSize = if (isTv) 18.sp else 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "当前地址可能不是直链，或站点还需要进一步解析。",
                    color = AppTextMuted,
                    fontSize = if (isTv) 14.sp else 12.sp,
                    textAlign = TextAlign.Center
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppButton(
                        text = "重试",
                        onClick = {
                            playbackError = null
                            overlayVisible = true
                            currentPositionMs = 0L
                            switchEpisode(episodeIndex)
                        }
                    )
                    AppButton(text = "返回详情", onClick = ::exitFullscreen)
                }
            }
        }
    }
}

@Composable
internal fun PhoneFullscreenGestureLayer(
    modifier: Modifier,
    resetKey: Any? = Unit,
    durationMs: Long,
    currentPositionMs: Long,
    brightnessPercent: Int,
    volumePercent: Int,
    reservedBottom: Dp = 80.dp,
    onSingleTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onGestureStart: () -> Unit,
    onGestureEnd: (PhoneGestureType) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onSeekPreview: (Long, Long, String?) -> Unit,
    onBoundaryReached: () -> Unit
) {
    Box(
        modifier = modifier
            .pointerInput(resetKey, onSingleTap, onDoubleTap) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { onDoubleTap() }
                )
            }
            .pointerInput(resetKey, durationMs, currentPositionMs, brightnessPercent, volumePercent) {
                val reservedLeftEdgePx = 15f
                val reservedBottomPx = reservedBottom.toPx()
                val triggerDistancePx = 10f
                val verticalSensitivityPx = 8f
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.position.x <= reservedLeftEdgePx) {
                        return@awaitEachGesture
                    }
                    if (down.position.y >= size.height - reservedBottomPx) {
                        return@awaitEachGesture
                    }

                    val pointerId = down.id
                    val zone = when {
                        down.position.x < size.width / 3f -> PhoneGestureType.BRIGHTNESS
                        down.position.x > size.width * 2f / 3f -> PhoneGestureType.VOLUME
                        else -> null
                    }
                    val seekAnchorMs = currentPositionMs
                    val brightnessAnchor = brightnessPercent
                    val volumeAnchor = volumePercent
                    var totalX = 0f
                    var totalY = 0f
                    var gestureType: PhoneGestureType? = null
                    var boundaryTriggered = false

                    var gestureEnded = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) {
                                gestureType?.let {
                                    onGestureEnd(it)
                                    gestureEnded = true
                                }
                                break
                            }

                            val delta = change.position - change.previousPosition
                            totalX += delta.x
                            totalY += delta.y

                            if (gestureType == null) {
                                val absX = abs(totalX)
                                val absY = abs(totalY)
                                if (hypot(absX.toDouble(), absY.toDouble()) < triggerDistancePx) {
                                    continue
                                }
                                val angle = Math.toDegrees(atan2(absY.toDouble(), absX.toDouble())).toFloat()
                                gestureType = when {
                                    angle < 30f -> PhoneGestureType.SEEK
                                    angle > 60f && zone != null -> zone
                                    absX >= absY -> PhoneGestureType.SEEK
                                    zone != null -> zone
                                    else -> PhoneGestureType.SEEK
                                }
                                onGestureStart()
                            }

                            when (gestureType) {
                                PhoneGestureType.BRIGHTNESS -> {
                                    val targetPercent = (brightnessAnchor + (-totalY / verticalSensitivityPx))
                                        .roundToInt()
                                        .coerceIn(0, 100)
                                    onBrightnessChange(targetPercent)
                                    val atBoundary = targetPercent == 0 || targetPercent == 100
                                    if (atBoundary && !boundaryTriggered) {
                                        onBoundaryReached()
                                        boundaryTriggered = true
                                    } else if (!atBoundary) {
                                        boundaryTriggered = false
                                    }
                                }

                                PhoneGestureType.VOLUME -> {
                                    val targetPercent = (volumeAnchor + (-totalY / verticalSensitivityPx))
                                        .roundToInt()
                                        .coerceIn(0, 100)
                                    onVolumeChange(targetPercent)
                                    val atBoundary = targetPercent == 0 || targetPercent == 100
                                    if (atBoundary && !boundaryTriggered) {
                                        onBoundaryReached()
                                        boundaryTriggered = true
                                    } else if (!atBoundary) {
                                        boundaryTriggered = false
                                    }
                                }

                                PhoneGestureType.SEEK -> {
                                    if (durationMs <= 0L) {
                                        continue
                                    }
                                    val previewDeltaMs = ((totalX / size.width) * durationMs * 0.8f).roundToLong()
                                    val targetMs = (seekAnchorMs + previewDeltaMs).coerceIn(0L, durationMs)
                                    val reachedEdgeMessage = when {
                                        targetMs == 0L && previewDeltaMs < 0L -> "已至开头"
                                        targetMs == durationMs && previewDeltaMs > 0L -> "已至结尾"
                                        else -> null
                                    }
                                    onSeekPreview(
                                        targetMs,
                                        targetMs - seekAnchorMs,
                                        reachedEdgeMessage
                                    )
                                    val atBoundary = reachedEdgeMessage != null
                                    if (atBoundary && !boundaryTriggered) {
                                        onBoundaryReached()
                                        boundaryTriggered = true
                                    } else if (!atBoundary) {
                                        boundaryTriggered = false
                                    }
                                }
                            }
                            change.consume()
                        }
                    } finally {
                        if (!gestureEnded) {
                            gestureType?.let(onGestureEnd)
                        }
                    }
                }
            }
    )
}

@Composable
internal fun AdjustmentFeedbackOverlay(
    modifier: Modifier,
    type: PhoneGestureType,
    percent: Int
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xCC000000))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OverlayGlyph(
            icon = if (type == PhoneGestureType.BRIGHTNESS) OverlayIcon.BRIGHTNESS else OverlayIcon.VOLUME,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = if (type == PhoneGestureType.BRIGHTNESS) "亮度 $percent%" else "音量 $percent%",
            color = AppTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .width(104.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.18f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent / 100f)
                    .height(4.dp)
                    .background(BrandPrimary)
            )
        }
    }
}

@Composable
internal fun SeekFeedbackOverlay(
    modifier: Modifier,
    state: SeekOverlayState
) {
    val isForward = state.deltaMs >= 0
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xCC000000))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isForward) "快进  ▶▶" else "◀◀  快退",
            color = AppTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .width(104.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.18f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(playbackProgress(state.targetPositionMs, state.durationMs))
                    .height(4.dp)
                    .background(BrandPrimary)
            )
        }
        Text(
            text = "${formatPlayerTime(state.targetPositionMs)} / ${formatPlayerTime(state.durationMs)}",
            color = AppTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = state.reachedEdgeMessage ?: signedDurationLabel(state.deltaMs),
            color = if (state.reachedEdgeMessage == null) AppTextSecondary else BrandPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun PlayPauseFeedbackOverlay(
    modifier: Modifier,
    isPlaying: Boolean
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color(0xB3000000)),
        contentAlignment = Alignment.Center
    ) {
        OverlayGlyph(
            icon = if (isPlaying) OverlayIcon.PLAY else OverlayIcon.PAUSE,
            modifier = Modifier.size(56.dp)
        )
    }
}

@Composable
internal fun OverlayGlyph(
    icon: OverlayIcon,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.08f
        when (icon) {
            OverlayIcon.BRIGHTNESS -> {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension * 0.18f
                drawCircle(
                    color = Color.White,
                    radius = radius,
                    center = center,
                    style = Stroke(width = stroke)
                )
                repeat(8) { index ->
                    val angle = Math.toRadians((index * 45.0) - 90.0)
                    val start = Offset(
                        x = center.x + kotlin.math.cos(angle).toFloat() * (radius + stroke),
                        y = center.y + kotlin.math.sin(angle).toFloat() * (radius + stroke)
                    )
                    val end = Offset(
                        x = center.x + kotlin.math.cos(angle).toFloat() * (radius + stroke * 2.2f),
                        y = center.y + kotlin.math.sin(angle).toFloat() * (radius + stroke * 2.2f)
                    )
                    drawLine(
                        color = Color.White,
                        start = start,
                        end = end,
                        strokeWidth = stroke * 0.7f,
                        cap = StrokeCap.Round
                    )
                }
            }

            OverlayIcon.VOLUME -> {
                val speaker = Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.42f)
                    lineTo(size.width * 0.38f, size.height * 0.42f)
                    lineTo(size.width * 0.54f, size.height * 0.26f)
                    lineTo(size.width * 0.54f, size.height * 0.74f)
                    lineTo(size.width * 0.38f, size.height * 0.58f)
                    lineTo(size.width * 0.2f, size.height * 0.58f)
                    close()
                }
                drawPath(path = speaker, color = Color.White)
                drawArc(
                    color = Color.White,
                    startAngle = -40f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.42f, size.height * 0.24f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.32f, size.height * 0.52f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.72f),
                    startAngle = -35f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.5f, size.height * 0.14f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.26f, size.height * 0.72f),
                    style = Stroke(width = stroke * 0.72f, cap = StrokeCap.Round)
                )
            }

            OverlayIcon.PLAY -> {
                val path = Path().apply {
                    moveTo(size.width * 0.34f, size.height * 0.22f)
                    lineTo(size.width * 0.74f, size.height * 0.5f)
                    lineTo(size.width * 0.34f, size.height * 0.78f)
                    close()
                }
                drawPath(path = path, color = Color.White.copy(alpha = 0.92f))
            }

            OverlayIcon.PAUSE -> {
                val barWidth = size.width * 0.14f
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.92f),
                    topLeft = Offset(size.width * 0.28f, size.height * 0.22f),
                    size = androidx.compose.ui.geometry.Size(barWidth, size.height * 0.56f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.92f),
                    topLeft = Offset(size.width * 0.58f, size.height * 0.22f),
                    size = androidx.compose.ui.geometry.Size(barWidth, size.height * 0.56f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerOptionPanel(
    panel: BottomPanel,
    isTv: Boolean,
    playbackSpeed: Float,
    selectedQuality: String,
    episodeIndex: Int,
    episodeCount: Int,
    speedOptions: List<Float>,
    qualityOptions: List<String>,
    speedFocusRequester: FocusRequester,
    qualityFocusRequester: FocusRequester,
    episodeFocusRequester: FocusRequester,
    onClose: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onQualitySelected: (String) -> Unit,
    onEpisodeSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xEE060D16))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), RoundedCornerShape(12.dp))
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
                },
                color = AppTextPrimary,
                fontSize = if (isTv) 18.sp else 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            PlayerPanelCloseButton(
                large = isTv,
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

            BottomPanel.EPISODE -> {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(episodeCount) { index ->
                        val focusModifier = if (isTv && index == episodeIndex) {
                            Modifier.focusRequester(episodeFocusRequester)
                        } else {
                            Modifier
                        }
                        PlayerPanelChip(
                            modifier = Modifier
                                .width(if (isTv) 108.dp else 78.dp)
                                .height(if (isTv) 48.dp else 38.dp)
                                .then(focusModifier),
                            text = "第${index + 1}集",
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

@Composable
private fun PlayerPanelCloseButton(
    large: Boolean,
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
                text = "收起",
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
                fontWeight = FontWeight.SemiBold
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
                .padding(horizontal = if (large) 18.dp else 12.dp, vertical = if (large) 14.dp else 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (enabled) Color(0xFFE7ECF3) else AppTextPrimary.copy(alpha = 0.35f),
                fontSize = if (large) 16.sp else 12.sp,
                maxLines = 1
            )
        }
    }
}

private enum class PlayerIcon {
    Back
}

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

private fun signedDurationLabel(deltaMs: Long): String {
    val absSeconds = (abs(deltaMs) / 1_000L).coerceAtLeast(0L)
    val prefix = if (deltaMs >= 0) "+" else "-"
    return "$prefix${formatPlayerTime(absSeconds * 1_000L)}"
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
