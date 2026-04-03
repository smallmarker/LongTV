package com.loong.longtv.ui.detail

import android.app.Activity
import android.content.Context.AUDIO_SERVICE
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.navigation.NavController
import androidx.tv.material3.Text
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.theme.AppCardOverlay
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.theme.BrandPrimaryDark
import com.loong.longtv.ui.widgets.AppButton
import com.loong.longtv.ui.widgets.TvSurface
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

private enum class BottomPanel {
    SPEED,
    QUALITY,
    EPISODE
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullscreenPlayerScreen(
    navController: NavController,
    title: String,
    videoUrl: String,
    requestHeaders: Map<String, String>,
    initialPositionMs: Long,
    episodeIndex: Int,
    episodeCount: Int,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val audioManager = remember(context) { context.getSystemService(AUDIO_SERVICE) as AudioManager }
    val haptic = LocalHapticFeedback.current
    val deviceClass = currentDeviceClass()
    val isTv = deviceClass == DeviceClass.TV
    val phoneLayout = deviceClass == DeviceClass.PHONE
    val controlState = rememberPlayerControlState()
    val speedOptions = remember { listOf(1.0f, 1.25f, 1.5f, 2.0f) }
    val qualityOptions = remember { listOf("跟随片源", "高清 1080P", "超清 4K") }
    var currentPositionMs by remember(initialPositionMs) { mutableLongStateOf(initialPositionMs) }
    var durationMs by remember(title, videoUrl) { mutableLongStateOf(0L) }
    var overlayVisible by remember(title, videoUrl) { mutableStateOf(false) }
    var isPlaying by remember(title, videoUrl) { mutableStateOf(false) }
    var isBuffering by remember(title, videoUrl) { mutableStateOf(false) }
    var playbackError by remember(title, videoUrl) { mutableStateOf<String?>(null) }
    var interactionVersion by remember(title, videoUrl) { mutableIntStateOf(0) }
    var playbackSpeed by remember(title, videoUrl) { mutableFloatStateOf(1.0f) }
    var gestureMessage by remember(title, videoUrl) { mutableStateOf<String?>(null) }
    var progressBarWidthPx by remember(title, videoUrl) { mutableFloatStateOf(1f) }
    var isScrubbing by remember(title, videoUrl) { mutableStateOf(false) }
    var scrubPositionMs by remember(title, videoUrl) { mutableLongStateOf(initialPositionMs) }
    var selectedQuality by remember(title, videoUrl) { mutableStateOf(qualityOptions[0]) }
    var activePanel by remember(title, videoUrl) { mutableStateOf<BottomPanel?>(null) }
    var brightnessLevel by remember(title, videoUrl) { mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f) }
    var volumePercent by remember(title, videoUrl) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        mutableIntStateOf((audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f / maxVolume).roundToLong().toInt())
    }
    val episodeLabel = if (episodeCount > 0) "第${episodeIndex + 1}集 / 共${episodeCount}集" else "单集播放"
    val currentDisplayPosition = if (isScrubbing) scrubPositionMs else currentPositionMs
    val progress = playbackProgress(currentDisplayPosition, durationMs)
    val playbackStateLabel = when {
        playbackError != null -> "播放异常"
        isBuffering -> "缓冲中"
        isPlaying -> "播放中"
        else -> "已暂停"
    }
    val overlayAutoHideMs = if (isTv) 5_000L else 3_000L

    fun registerInteraction() {
        overlayVisible = true
        interactionVersion += 1
    }

    fun showGestureMessage(message: String) {
        gestureMessage = message
        registerInteraction()
    }

    fun closePanel() {
        activePanel = null
        registerInteraction()
    }

    fun seekToPosition(positionMs: Long) {
        controlState.seekTo(positionMs)
        registerInteraction()
    }

    fun previewSeekBy(offsetMs: Long) {
        val duration = durationMs.takeIf { it > 0 } ?: return
        isScrubbing = true
        scrubPositionMs = (currentPositionMs + offsetMs).coerceIn(0L, duration)
        registerInteraction()
    }

    fun commitPreviewSeek() {
        if (!isScrubbing) return
        seekToPosition(scrubPositionMs)
        isScrubbing = false
    }

    fun seekBy(offsetMs: Long) {
        controlState.seekBy(offsetMs)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        showGestureMessage(if (offsetMs >= 0) "快进 10 秒" else "快退 10 秒")
    }

    fun togglePlayPause() {
        controlState.togglePlayPause()
        registerInteraction()
    }

    fun togglePanel(panel: BottomPanel) {
        activePanel = if (activePanel == panel) null else panel
        registerInteraction()
    }

    fun cyclePlaybackSpeed() {
        val nextIndex = (speedOptions.indexOf(playbackSpeed).takeIf { it >= 0 } ?: 0)
            .let { (it + 1) % speedOptions.size }
        playbackSpeed = speedOptions[nextIndex]
        controlState.setSpeed(playbackSpeed)
        showGestureMessage("倍速 ${formatPlaybackSpeed(playbackSpeed)}")
    }

    fun exitFullscreen() {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.set("fullscreen_position", currentPositionMs)
        navController.popBackStack()
    }

    fun switchEpisode(targetIndex: Int) {
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

    fun positionFromBarOffset(offsetX: Float): Long {
        if (durationMs <= 0 || progressBarWidthPx <= 0f) return currentPositionMs
        val percent = (offsetX / progressBarWidthPx).coerceIn(0f, 1f)
        return (durationMs * percent).roundToLong()
    }

    fun adjustBrightness(deltaFraction: Float) {
        val window = activity?.window ?: return
        brightnessLevel = (brightnessLevel + deltaFraction).coerceIn(0.05f, 1f)
        window.attributes = window.attributes.apply {
            screenBrightness = brightnessLevel
        }
        showGestureMessage("亮度 ${(brightnessLevel * 100).roundToLong()}%")
    }

    fun adjustVolume(deltaFraction: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val target = (currentVolume + deltaFraction * maxVolume)
            .roundToLong()
            .toInt()
            .coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        volumePercent = (target * 100f / maxVolume).roundToLong().toInt()
        showGestureMessage("音量 ${volumePercent}%")
    }

    BackHandler(onBack = ::exitFullscreen)

    DisposableEffect(activity) {
        if (activity == null) onDispose { }
        else {
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

    LaunchedEffect(overlayVisible, interactionVersion, playbackError, isBuffering, overlayAutoHideMs) {
        if (overlayVisible && playbackError == null && !isBuffering) {
            delay(overlayAutoHideMs)
            overlayVisible = false
        }
    }

    LaunchedEffect(gestureMessage) {
        if (gestureMessage != null) {
            delay(900)
            gestureMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VideoPlayerView(
            modifier = Modifier.fillMaxSize(),
            videoUrl = videoUrl,
            requestHeaders = requestHeaders,
            initialPositionMs = initialPositionMs,
            showController = false,
            enableNativeController = false,
            controlState = controlState,
            onProgressChanged = { position, duration ->
                currentPositionMs = position
                durationMs = duration
                if (!isScrubbing) scrubPositionMs = position
            },
            onPlaybackStateChanged = { state ->
                isBuffering = state == Player.STATE_BUFFERING
            },
            onPlayingChanged = { playing ->
                isPlaying = playing
            },
            onPlaybackError = { message ->
                playbackError = message
                overlayVisible = true
            }
        )

        if (!isTv) {
            Row(modifier = Modifier.fillMaxSize()) {
                PhoneGestureZone(
                    modifier = Modifier.weight(1f),
                    onTap = ::registerInteraction,
                    onDoubleTap = { seekBy(-10_000L) }
                )
                PhoneGestureZone(
                    modifier = Modifier.weight(1f),
                    onTap = ::togglePlayPause,
                    onDoubleTap = ::togglePlayPause,
                    onHorizontalDrag = { deltaX ->
                        val duration = durationMs.takeIf { it > 0 } ?: return@PhoneGestureZone
                        val offsetMs = ((deltaX / 900f) * duration).roundToLong()
                        previewSeekBy(offsetMs)
                        showGestureMessage(
                            if (offsetMs >= 0) {
                                "快进至 ${formatPlayerTime(scrubPositionMs)}"
                            } else {
                                "快退至 ${formatPlayerTime(scrubPositionMs)}"
                            }
                        )
                    },
                    onHorizontalDragEnd = ::commitPreviewSeek
                )
                PhoneGestureZone(
                    modifier = Modifier.weight(1f),
                    onTap = ::registerInteraction,
                    onDoubleTap = { seekBy(10_000L) }
                )
            }
        }

        if (!isTv && overlayVisible) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp, bottom = 112.dp)
            ) {
                SideAdjustZone(
                    modifier = Modifier.weight(1f),
                    onAdjust = { delta -> adjustBrightness(delta / 450f) }
                )
                Spacer(modifier = Modifier.weight(1f))
                SideAdjustZone(
                    modifier = Modifier.weight(1f),
                    onAdjust = { delta -> adjustVolume(delta / 360f) }
                )
            }
        }

        if (gestureMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.56f))
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(
                    text = gestureMessage.orEmpty(),
                    color = AppTextPrimary,
                    fontSize = if (isTv) 18.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (overlayVisible) {
            val topBarHeight = if (isTv) 64.dp else 48.dp
            val progressBarHeight = if (isTv) 72.dp else 40.dp
            val bottomBarHeight = if (isTv) 80.dp else 56.dp
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topBarHeight),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconOnlyButton(
                            icon = PlayerIcon.Back,
                            large = isTv,
                            onClick = ::exitFullscreen
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title.ifBlank { "全屏播放" },
                            color = AppTextPrimary,
                            fontSize = if (isTv) 22.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = episodeLabel,
                            color = AppTextSecondary,
                            fontSize = if (isTv) 15.sp else 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InfoBadge(
                            text = if (isTv) "高清" else "1080P",
                            highlight = true,
                            large = isTv
                        )
                        InfoBadge(
                            text = playbackStateLabel,
                            highlight = isPlaying || isBuffering,
                            large = isTv
                        )
                        IconOnlyButton(
                            icon = PlayerIcon.More,
                            large = isTv,
                            onClick = ::registerInteraction
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xAA07101C), Color(0xF007101C))
                        )
                    )
                    .padding(horizontal = edgePadding, vertical = 12.dp)
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
                            onClose = ::closePanel,
                            onSpeedSelected = { speed ->
                                playbackSpeed = speed
                                controlState.setSpeed(speed)
                                showGestureMessage("倍速 ${formatPlaybackSpeed(speed)}")
                                activePanel = null
                            },
                            onQualitySelected = { quality ->
                                selectedQuality = quality
                                showGestureMessage("清晰度 $quality")
                                activePanel = null
                            },
                            onEpisodeSelected = { targetIndex ->
                                if (targetIndex != episodeIndex) switchEpisode(targetIndex)
                                else closePanel()
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
                                    onDragCancel = { isScrubbing = false },
                                    onDragEnd = {
                                        if (isScrubbing) seekToPosition(scrubPositionMs)
                                        isScrubbing = false
                                    }
                                ) { change, _ ->
                                    if (durationMs <= 0) return@detectDragGestures
                                    scrubPositionMs = positionFromBarOffset(change.position.x)
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
                                        listOf(BrandPrimary, BrandPrimaryDark)
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .padding(end = 0.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isTv) 14.dp else 10.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White)
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
                        .height(bottomBarHeight),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerActionChip(
                        text = if (isPlaying) "暂停" else "播放",
                        large = isTv,
                        onClick = ::togglePlayPause
                    )
                    if (episodeCount > 1) {
                        PlayerActionChip(
                            text = "上一集",
                            large = isTv,
                            enabled = episodeIndex > 0,
                            onClick = { if (episodeIndex > 0) switchEpisode(episodeIndex - 1) }
                        )
                        PlayerActionChip(
                            text = "下一集",
                            large = isTv,
                            enabled = episodeIndex < episodeCount - 1,
                            onClick = { if (episodeIndex < episodeCount - 1) switchEpisode(episodeIndex + 1) }
                        )
                    }
                    PlayerActionChip(
                        text = "倍速 ${formatPlaybackSpeed(playbackSpeed)}",
                        large = isTv,
                        onClick = { togglePanel(BottomPanel.SPEED) }
                    )
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
private fun SideAdjustZone(
    modifier: Modifier,
    onAdjust: (Float) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    if (kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x)) {
                        onAdjust(-dragAmount.y)
                        change.consume()
                    }
                }
            }
    )
}

@Composable
private fun PhoneGestureZone(
    modifier: Modifier,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onHorizontalDrag: ((Float) -> Unit)? = null,
    onHorizontalDragEnd: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { onDoubleTap() }
                )
            }
            .pointerInput(onHorizontalDrag, onHorizontalDragEnd) {
                if (onHorizontalDrag == null) return@pointerInput
                detectDragGestures(
                    onDragEnd = { onHorizontalDragEnd?.invoke() },
                    onDragCancel = { onHorizontalDragEnd?.invoke() }
                ) { change, dragAmount ->
                    if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)) {
                        onHorizontalDrag.invoke(dragAmount.x)
                        change.consume()
                    }
                }
            }
    )
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
    onClose: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onQualitySelected: (String) -> Unit,
    onEpisodeSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(AppCardOverlay)
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
            AppButton(text = "收起", onClick = onClose)
        }
        when (panel) {
            BottomPanel.SPEED -> {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    speedOptions.forEach { option ->
                        PlayerPanelChip(
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
                    qualityOptions.forEach { option ->
                        PlayerPanelChip(
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
                        PlayerPanelChip(
                            text = "第${index + 1}集",
                            selected = index == episodeIndex,
                            large = isTv,
                            onClick = { onEpisodeSelected(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerPanelChip(
    text: String,
    selected: Boolean,
    large: Boolean,
    onClick: () -> Unit
) {
    TvSurface(onClick = onClick) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (selected) BrandPrimary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f))
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
    text: String,
    large: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TvSurface(onClick = onClick) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (enabled) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f)
                )
                .padding(horizontal = if (large) 18.dp else 12.dp, vertical = if (large) 14.dp else 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (enabled) AppTextPrimary else AppTextPrimary.copy(alpha = 0.4f),
                fontSize = if (large) 16.sp else 12.sp,
                maxLines = 1
            )
        }
    }
}

private enum class PlayerIcon {
    Back,
    More
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
                PlayerIcon.More -> MoreDotsIcon(color = AppTextPrimary)
            }
        }
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
            start = androidx.compose.ui.geometry.Offset(startX, size.height * 0.2f),
            end = androidx.compose.ui.geometry.Offset(endX, midY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(endX, midY),
            end = androidx.compose.ui.geometry.Offset(startX, size.height * 0.8f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun MoreDotsIcon(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = size.minDimension * 0.1f
        val centerY = size.height / 2f
        drawCircle(color = color, radius = radius, center = androidx.compose.ui.geometry.Offset(size.width * 0.25f, centerY))
        drawCircle(color = color, radius = radius, center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, centerY))
        drawCircle(color = color, radius = radius, center = androidx.compose.ui.geometry.Offset(size.width * 0.75f, centerY))
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
    else -> "x${speed}"
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
