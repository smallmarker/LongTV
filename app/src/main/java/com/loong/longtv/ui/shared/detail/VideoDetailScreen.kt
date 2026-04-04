package com.loong.longtv.ui.shared.detail

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.ContextWrapper
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.loong.longtv.nav.Screen
import com.loong.longtv.data.bean.PlayInfo
import com.loong.longtv.data.bean.UrlInfo
import com.loong.longtv.data.player.PlayableMedia
import com.loong.longtv.ui.theme.AppCardOverlay
import com.loong.longtv.ui.theme.AppBackground
import com.loong.longtv.ui.theme.AppBackgroundElevated
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppSurface
import com.loong.longtv.ui.theme.AppSurfaceVariant
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.theme.BrandSecondary
import com.loong.longtv.ui.theme.BrandAccent
import com.loong.longtv.ui.shared.widgets.AppButton
import com.loong.longtv.ui.shared.widgets.SectionHeader
import com.loong.longtv.ui.shared.widgets.SmartAsyncImage
import com.loong.longtv.ui.shared.widgets.TvSurface
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import android.view.KeyEvent as AndroidKeyEvent

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VideoDetailRouteContent(
    navigator: NavController,
    key: String,
    id: String,
    viewModel: VideoDetailViewModel,
    content: @Composable (
        state: VideoDetailUiState,
        detail: com.loong.longtv.data.bean.Video,
        sources: List<PlayInfo>,
        selectedSource: PlayInfo?,
        playableMedia: PlayableMedia?,
        onOpenFullscreen: (PlayableMedia, String) -> Unit,
        onProgressChanged: (Long, Long) -> Unit,
        onToggleFavorite: () -> Unit,
        onPlayCurrent: () -> Unit,
        onRetryCurrent: () -> Unit,
        onClearError: () -> Unit,
        onSelectSource: (Int) -> Unit,
        onSelectEpisode: (PlayInfo, Int) -> Unit
    ) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentBackStackEntry by navigator.currentBackStackEntryAsState()
    val savedStateHandle = currentBackStackEntry?.savedStateHandle
    var reopenFullscreenAfterSwitch by remember(key, id) { mutableStateOf(false) }

    fun openFullscreen(playableMedia: PlayableMedia, episodeName: String) {
        navigator.navigate(
            Screen.FullscreenPlayer.createRoute(
                title = buildString {
                    append(state.detail?.vod_name?.ifBlank { "全屏播放" } ?: "全屏播放")
                    if (episodeName.isNotBlank()) append(" - ").append(episodeName)
                },
                url = playableMedia.url,
                headers = playableMedia.headers,
                position = state.resumePositionMs,
                episodeIndex = state.selectedEpisodeIndex,
                episodeCount = state.detail?.playInfos?.getOrNull(state.selectedSourceIndex)?.urlInfos?.size ?: 0
            )
        )
    }

    LaunchedEffect(key, id) {
        viewModel.loadDetail(key, id)
    }

    LaunchedEffect(currentBackStackEntry) {
        val position = savedStateHandle?.remove<Long>("fullscreen_position") ?: return@LaunchedEffect
        viewModel.applyFullscreenProgress(position)
    }

    LaunchedEffect(currentBackStackEntry, state.detail, state.selectedSourceIndex) {
        val targetIndex = savedStateHandle?.remove<Int>("fullscreen_target_episode_index") ?: return@LaunchedEffect
        reopenFullscreenAfterSwitch = savedStateHandle?.remove<Boolean>("fullscreen_reenter") == true
        val playInfo = state.detail?.playInfos?.getOrNull(state.selectedSourceIndex) ?: return@LaunchedEffect
        if (targetIndex in playInfo.urlInfos.indices) {
            viewModel.playEpisode(playInfo, targetIndex)
        }
    }

    LaunchedEffect(state.playableMedia, reopenFullscreenAfterSwitch, state.selectedEpisodeIndex) {
        val playableMedia = state.playableMedia ?: return@LaunchedEffect
        if (!reopenFullscreenAfterSwitch || playableMedia.requiresParse || playableMedia.url.isBlank()) return@LaunchedEffect
        val episodeName = state.detail
            ?.playInfos
            ?.getOrNull(state.selectedSourceIndex)
            ?.urlInfos
            ?.getOrNull(state.selectedEpisodeIndex)
            ?.name
            .orEmpty()
        reopenFullscreenAfterSwitch = false
        openFullscreen(playableMedia, episodeName)
    }

    when {
        state.contentLoading && state.detail == null -> {
            CenterMessage("加载详情中...", Color.White)
        }

        state.error != null && state.detail == null -> {
            CenterMessage(state.error ?: "加载失败", Color.Red)
        }

        else -> {
            val detail = state.detail ?: return
            val sources = detail.playInfos
            val selectedSource = sources.getOrNull(state.selectedSourceIndex)
            val playableMedia = state.playableMedia
            val onPlayCurrent = {
                if (selectedSource != null) {
                    viewModel.playEpisode(selectedSource, state.selectedEpisodeIndex)
                }
            }

            content(
                state,
                detail,
                sources,
                selectedSource,
                playableMedia,
                ::openFullscreen,
                viewModel::saveProgress,
                viewModel::toggleFavorite,
                onPlayCurrent,
                viewModel::retryCurrentEpisode,
                viewModel::clearError,
                viewModel::selectSource,
                viewModel::playEpisode
            )
        }
    }
}

@Composable
internal fun DetailPreviewBlock(
    modifier: Modifier = Modifier,
    detail: com.loong.longtv.data.bean.Video,
    playableMedia: PlayableMedia?,
    resumePositionMs: Long,
    selectedSource: PlayInfo?,
    selectedEpisodeIndex: Int,
    onBack: () -> Unit,
    onOpenFullscreen: (PlayableMedia, String) -> Unit,
    onProgressChanged: (Long, Long) -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember(context) { context.getSystemService(AUDIO_SERVICE) as AudioManager }
    val activity = context.findActivity()
    val haptic = LocalHapticFeedback.current
    val controlState = rememberPlayerControlState()
    val title = detail.vod_name.safeText("未命名视频")
    val episodeName = selectedSource?.urlInfos?.getOrNull(selectedEpisodeIndex)?.name.orEmpty()
    val topBarTitle = buildString {
        append(title)
        if (episodeName.isNotBlank()) append(" · ").append(episodeName)
    }
    var currentPositionMs by remember(playableMedia?.url, resumePositionMs) { mutableLongStateOf(resumePositionMs) }
    var durationMs by remember(playableMedia?.url) { mutableLongStateOf(0L) }
    var isPlaying by remember(playableMedia?.url) { mutableStateOf(false) }
    var isBuffering by remember(playableMedia?.url) { mutableStateOf(false) }
    var chromeVisible by remember(playableMedia?.url) { mutableStateOf(false) }
    var chromeInteractionVersion by remember(playableMedia?.url) { mutableLongStateOf(0L) }
    var gestureInProgress by remember(playableMedia?.url) { mutableStateOf(false) }
    var isScrubbing by remember(playableMedia?.url) { mutableStateOf(false) }
    var scrubPositionMs by remember(playableMedia?.url, resumePositionMs) { mutableLongStateOf(resumePositionMs) }
    var phoneFeedbackOverlay by remember(playableMedia?.url) { mutableStateOf<PhoneFeedbackOverlay?>(null) }
    var phoneFeedbackVersion by remember(playableMedia?.url) { mutableIntStateOf(0) }
    var brightnessLevel by remember(playableMedia?.url) {
        mutableStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f)
    }
    var volumePercent by remember(playableMedia?.url) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        mutableStateOf(((audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f) / maxVolume).roundToInt())
    }

    LaunchedEffect(chromeVisible, chromeInteractionVersion, playableMedia?.url, gestureInProgress) {
        if (chromeVisible && !gestureInProgress) {
            kotlinx.coroutines.delay(3_000)
            chromeVisible = false
        }
    }

    LaunchedEffect(phoneFeedbackOverlay, phoneFeedbackVersion, gestureInProgress) {
        val current = phoneFeedbackOverlay ?: return@LaunchedEffect
        val timeout = when (current) {
            is PhoneFeedbackOverlay.Adjustment -> 1_000L
            is PhoneFeedbackOverlay.Seek -> if (gestureInProgress) Long.MAX_VALUE else 500L
            is PhoneFeedbackOverlay.PlayPause -> 500L
        }
        if (timeout != Long.MAX_VALUE) {
            kotlinx.coroutines.delay(timeout)
            if (phoneFeedbackOverlay == current) {
                phoneFeedbackOverlay = null
            }
        }
    }

    fun registerChromeInteraction() {
        chromeVisible = true
        chromeInteractionVersion += 1
    }

    fun showPhoneFeedback(
        overlay: PhoneFeedbackOverlay,
        keepControlsVisible: Boolean = true
    ) {
        phoneFeedbackOverlay = overlay
        phoneFeedbackVersion += 1
        if (keepControlsVisible) {
            registerChromeInteraction()
        }
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
        val target = (maxVolume * (clamped / 100f)).roundToInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
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
        controlState.seekTo(scrubPositionMs)
        isScrubbing = false
        phoneFeedbackVersion += 1
        registerChromeInteraction()
    }

    fun togglePlayPauseFromGesture() {
        val targetIsPlaying = !isPlaying
        controlState.togglePlayPause()
        showPhoneFeedback(
            overlay = PhoneFeedbackOverlay.PlayPause(targetIsPlaying),
            keepControlsVisible = false
        )
    }

    fun togglePlayPauseFromControl() {
        controlState.togglePlayPause()
        registerChromeInteraction()
    }

    Box(
        modifier = modifier
            .background(AppCardOverlay)
    ) {
        if (playableMedia?.url?.isNotBlank() == true && !playableMedia.requiresParse) {
            VideoPlayerView(
                modifier = Modifier.fillMaxSize(),
                videoUrl = playableMedia.url,
                requestHeaders = playableMedia.headers,
                initialPositionMs = resumePositionMs,
                enableNativeController = false,
                sharedSessionEnabled = true,
                controlState = controlState,
                onProgressChanged = { position, duration ->
                    currentPositionMs = position
                    durationMs = duration
                    if (!isScrubbing) {
                        scrubPositionMs = position
                    }
                    onProgressChanged(position, duration)
                },
                onPlaybackStateChanged = { state ->
                    isBuffering = state == Player.STATE_BUFFERING
                },
                onPlayingChanged = { playing ->
                    isPlaying = playing
                }
            )
        } else {
            SmartAsyncImage(
                model = detail.vod_pic,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                fallbackText = title
            )
        }

        if (playableMedia?.url?.isNotBlank() == true && !playableMedia.requiresParse) {
            PhoneFullscreenGestureLayer(
                modifier = Modifier.matchParentSize(),
                durationMs = durationMs,
                currentPositionMs = currentPositionMs,
                brightnessPercent = (brightnessLevel * 100f).roundToInt().coerceIn(0, 100),
                volumePercent = volumePercent,
                onSingleTap = {
                    chromeVisible = !chromeVisible
                    if (chromeVisible) {
                        chromeInteractionVersion += 1
                    }
                },
                onDoubleTap = ::togglePlayPauseFromGesture,
                onGestureStart = {
                    gestureInProgress = true
                    registerChromeInteraction()
                },
                onGestureEnd = { type ->
                    gestureInProgress = false
                    when (type) {
                        PhoneGestureType.BRIGHTNESS,
                        PhoneGestureType.VOLUME -> phoneFeedbackVersion += 1
                        PhoneGestureType.SEEK -> commitPreviewSeek()
                    }
                },
                onBrightnessChange = ::updateBrightnessPercent,
                onVolumeChange = ::updateVolumePercent,
                onSeekPreview = ::previewSeekTo,
                onBoundaryReached = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                }
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x6607101C),
                            Color.Transparent,
                            Color(0xE607101C)
                        )
                    )
                )
        )

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

        if (chromeVisible) {
            PlayerTopInlineBar(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp),
                title = topBarTitle,
                onBack = onBack,
                large = false
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniPlayPauseIcon(
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable(onClick = ::togglePlayPauseFromControl)
                        .padding(2.dp)
                )
                Text(
                    text = when {
                        playableMedia == null -> "--:--"
                        playableMedia.requiresParse -> "--:--"
                        else -> formatPlayerTime(if (isScrubbing) scrubPositionMs else currentPositionMs)
                    },
                    color = AppTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 10.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(playbackProgress(currentPositionMs, durationMs))
                            .height(5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BrandPrimary, Color(0xFFFF7F58))
                                )
                            )
                    )
                }
                Text(
                    text = when {
                        playableMedia == null -> "--:--"
                        playableMedia.requiresParse -> "--:--"
                        durationMs > 0 -> formatPlayerTime(durationMs)
                        else -> "--:--"
                    },
                    color = AppTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 10.dp)
                )
                if (playableMedia != null && !playableMedia.requiresParse && playableMedia.url.isNotBlank()) {
                    FullscreenIconButton(
                        modifier = Modifier.padding(start = 10.dp),
                        onClick = { onOpenFullscreen(playableMedia, episodeName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayPauseIcon(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(12.dp)) {
        if (isPlaying) {
            val barWidth = size.width * 0.22f
            val gap = size.width * 0.16f
            drawRoundRect(
                color = AppTextMuted,
                topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.2f, 0f),
                size = androidx.compose.ui.geometry.Size(barWidth, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
            )
            drawRoundRect(
                color = AppTextMuted,
                topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.2f + barWidth + gap, 0f),
                size = androidx.compose.ui.geometry.Size(barWidth, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        } else {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width * 0.18f, size.height * 0.08f)
                lineTo(size.width * 0.82f, size.height * 0.5f)
                lineTo(size.width * 0.18f, size.height * 0.92f)
                close()
            }
            drawPath(path = path, color = AppTextMuted)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailInfoBlock(
    navigator: NavController,
    state: VideoDetailUiState,
    detail: com.loong.longtv.data.bean.Video,
    selectedSource: PlayInfo?,
    playableMedia: PlayableMedia?,
    resumePositionMs: Long,
    phoneLayout: Boolean,
    onToggleFavorite: () -> Unit,
    onPlayCurrent: () -> Unit,
    onRetryCurrent: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = detail.vod_name.safeText("未命名视频")
    val typeName = detail.type_name.safeText()
    val year = detail.vod_year.safeText()
    val area = detail.vod_area.safeText()
    val remarks = detail.vod_remarks.safeText()
    val content = detail.vod_content.safeText("暂无简介\nID: ${detail.vod_id.safeText()}")
    var showFullDescription by remember(title, content) { mutableStateOf(false) }

    if (phoneLayout) {
        Column(modifier = modifier) {
            Text(
                text = title,
                color = AppTextPrimary,
                fontSize = 22.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailChip(text = state.siteName.ifBlank { state.siteKey })
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = listOf(typeName, year, area, remarks)
                    .filter { it.isNotBlank() }
                    .joinToString(" / "),
                color = AppTextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = content,
                color = AppTextMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            PhonePlaybackSummaryCard(
                selectedSource = selectedSource,
                selectedEpisodeIndex = state.selectedEpisodeIndex,
                playableMedia = playableMedia,
                resumePositionMs = resumePositionMs
            )
            Spacer(modifier = Modifier.height(16.dp))
            PhoneDetailActionGroup(
                favorite = state.favorite,
                selectedSource = selectedSource,
                playableMedia = playableMedia,
                error = state.error,
                onToggleFavorite = onToggleFavorite,
                onRetryCurrent = onRetryCurrent,
                onClearError = onClearError
            )
            if (state.playerLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "正在获取播放地址...", color = Color.White, fontSize = 14.sp)
            }
            if (state.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = state.error ?: "", color = AppError, fontSize = 13.sp)
            }
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = title,
                    color = AppTextPrimary,
                    fontSize = 24.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = listOf(typeName, year, area, remarks)
                        .filter { it.isNotBlank() }
                        .joinToString(" / "),
                    color = AppTextSecondary,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = content,
                    color = AppTextMuted,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            CurrentPlaybackInfo(
                selectedSource = selectedSource,
                playableMedia = playableMedia,
                siteLabel = state.siteName.ifBlank { state.siteKey },
                phoneLayout = false
            )
            Spacer(modifier = Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TvDetailActionButton(
                    text = "简介",
                    onClick = { showFullDescription = true }
                )
                TvDetailActionButton(
                    text = if (state.favorite) "取消收藏" else "收藏",
                    onClick = onToggleFavorite
                )
                if (selectedSource != null && playableMedia == null && selectedSource.urlInfos.isNotEmpty()) {
                    TvDetailActionButton(
                        text = if (resumePositionMs > 0) "继续观看" else "播放当前集",
                        onClick = onPlayCurrent,
                        prominent = true
                    )
                }
                if (playableMedia != null && !playableMedia.requiresParse && playableMedia.url.isNotBlank()) {
                    TvDetailActionButton(text = "重新获取片源", onClick = onRetryCurrent)
                }
            }

            if (state.playerLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "正在获取播放地址...", color = Color.White, fontSize = 14.sp)
            }
            if (state.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = state.error ?: "", color = AppError, fontSize = 13.sp)
                if (selectedSource != null && selectedSource.urlInfos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TvDetailActionButton(text = "重试当前集", onClick = onRetryCurrent, prominent = true)
                        TvDetailActionButton(text = "清除提示", onClick = onClearError)
                    }
                }
            }
        }
    }

    if (!phoneLayout && showFullDescription) {
        TvFullDescriptionDialog(
            title = title,
            content = content,
            onDismiss = { showFullDescription = false }
        )
    }
}

@Composable
private fun TvDetailActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(16.dp)
    TvSurface(
        onClick = onClick,
        modifier = modifier
            .clip(shape)
            .border(
                BorderStroke(
                    1.dp,
                    if (prominent) BrandPrimary.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.08f)
                ),
                shape
            ),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (prominent) BrandPrimary.copy(alpha = 0.16f) else AppSurface.copy(alpha = 0.92f),
            focusedContainerColor = if (prominent) BrandPrimary.copy(alpha = 0.28f) else AppSurfaceVariant,
            pressedContainerColor = if (prominent) BrandPrimary.copy(alpha = 0.22f) else AppSurfaceVariant.copy(alpha = 0.96f),
            disabledContainerColor = AppSurfaceVariant.copy(alpha = 0.58f)
        )
    ) {
        Text(
            text = text,
            color = if (prominent) BrandPrimary else AppTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun TvFullDescriptionDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    val contentScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val closeButtonFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    val scrollProgress = if (contentScrollState.maxValue == 0) 0f
    else contentScrollState.value.toFloat() / contentScrollState.maxValue.toFloat()

    LaunchedEffect(Unit) {
        closeButtonFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.72f),
                            Color(0xF2142033),
                            Color.Black.copy(alpha = 0.82f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(820.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .border(
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        RoundedCornerShape(30.dp)
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AppBackgroundElevated,
                                AppSurface,
                                AppBackgroundElevated
                            )
                        )
                    )
                    .padding(horizontal = 30.dp, vertical = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "剧情简介",
                            color = BrandSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = title,
                            color = AppTextPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TvDetailActionButton(
                        text = "关闭",
                        onClick = onDismiss,
                        modifier = Modifier
                            .focusRequester(closeButtonFocusRequester)
                            .focusProperties {
                                down = contentFocusRequester
                            }
                    )
                }
                Spacer(modifier = Modifier.height(22.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            RoundedCornerShape(24.dp)
                        )
                        .background(Color.White.copy(alpha = 0.035f))
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .focusRequester(contentFocusRequester)
                                .focusProperties {
                                    up = closeButtonFocusRequester
                                }
                                .focusable()
                                .onPreviewKeyEvent { event ->
                                    if (event.nativeKeyEvent.action != AndroidKeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                                    when (event.nativeKeyEvent.keyCode) {
                                        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                            if (contentScrollState.value >= contentScrollState.maxValue) {
                                                return@onPreviewKeyEvent false
                                            }
                                            coroutineScope.launch {
                                                contentScrollState.animateScrollTo(
                                                    (contentScrollState.value + 140).coerceAtMost(contentScrollState.maxValue)
                                                )
                                            }
                                            true
                                        }

                                        AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                                            if (contentScrollState.value <= 0) {
                                                return@onPreviewKeyEvent false
                                            }
                                            coroutineScope.launch {
                                                contentScrollState.animateScrollTo(
                                                    (contentScrollState.value - 140).coerceAtLeast(0)
                                                )
                                            }
                                            true
                                        }

                                        else -> false
                                    }
                                }
                                .verticalScroll(contentScrollState),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = content,
                                color = AppTextSecondary,
                                fontSize = 18.sp,
                                lineHeight = 29.sp
                            )
                        }

                        Column(
                            modifier = Modifier.height(360.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${(scrollProgress * 100).toInt()}%",
                                color = AppTextMuted,
                                fontSize = 12.sp
                            )
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .weight(1f)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxSize(
                                            fraction = (if (contentScrollState.maxValue == 0) 1f else (0.18f + scrollProgress * 0.82f))
                                                .coerceIn(0.18f, 1f)
                                        )
                                        .align(Alignment.TopCenter)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    BrandSecondary,
                                                    BrandPrimary
                                                )
                                            )
                                        )
                                )
                            }
                            Text(
                                text = if (contentScrollState.maxValue > 0) "可滚动" else "到底了",
                                color = AppTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "按返回键或聚焦“关闭”即可退出",
                    color = AppTextMuted,
                    fontSize = 13.sp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(56.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    AppBackgroundElevated.copy(alpha = 0.74f)
                                )
                            )
                        )
                )
            }
        }
    }
}

private fun String?.safeText(fallback: String = ""): String {
    return this.orEmpty().takeIf { it.isNotBlank() } ?: fallback
}

@Composable
private fun DetailChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                RoundedCornerShape(999.dp)
            )
    ) {
        Text(
            text = text,
            color = AppTextPrimary,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun PhonePlaybackSummaryCard(
    selectedSource: PlayInfo?,
    selectedEpisodeIndex: Int,
    playableMedia: PlayableMedia?,
    resumePositionMs: Long
) {
    val episodeName = selectedSource?.urlInfos?.getOrNull(selectedEpisodeIndex)?.name.orEmpty()
    val statusLabel = when {
        playableMedia == null -> "待加载"
        playableMedia.requiresParse -> "需换源"
        playableMedia.url.isBlank() -> "未就绪"
        else -> "可播放"
    }
    val statusColor = when {
        playableMedia == null -> AppTextMuted
        playableMedia.requiresParse -> BrandAccent
        playableMedia.url.isBlank() -> AppTextSecondary
        else -> BrandSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = selectedSource?.flag ?: "未选择线路",
                    color = AppTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = episodeName.ifBlank { "选择剧集后即可开始播放" },
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (selectedSource != null) {
                DetailChip(text = "共 ${selectedSource.urlInfos.size} 集")
            }
            if (playableMedia != null && playableMedia.resolutionNote.isNotBlank()) {
                DetailChip(text = playableMedia.resolutionNote)
            } else if (playableMedia != null) {
                DetailChip(text = playableMedia.resolutionType.label)
            }
        }
    }
}

@Composable
private fun PhoneDetailActionGroup(
    favorite: Boolean,
    selectedSource: PlayInfo?,
    playableMedia: PlayableMedia?,
    error: String?,
    onToggleFavorite: () -> Unit,
    onRetryCurrent: () -> Unit,
    onClearError: () -> Unit
) {
    val canPlay = selectedSource != null && selectedSource.urlInfos.isNotEmpty()
    val showRefresh = playableMedia != null

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PhoneDetailActionButton(
                text = if (favorite) "已收藏" else "收藏",
                onClick = onToggleFavorite,
                modifier = Modifier.weight(1f),
                primary = false
            )
            if (showRefresh) {
                PhoneDetailActionButton(
                    text = "刷新片源",
                    onClick = onRetryCurrent,
                    modifier = Modifier.weight(1f),
                    enabled = canPlay,
                    primary = false
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        if (error != null) {
            PhoneDetailActionButton(
                text = "清除提示",
                onClick = onClearError,
                modifier = Modifier.fillMaxWidth(),
                primary = false
            )
        }
    }
}

@Composable
private fun PhoneDetailActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean
) {
    val background = when {
        !enabled -> AppSurfaceVariant
        primary -> BrandPrimary
        else -> Color.White.copy(alpha = 0.05f)
    }
    val contentColor = when {
        !enabled -> AppTextPrimary.copy(alpha = 0.45f)
        primary -> Color(0xFF0E1626)
        else -> AppTextPrimary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (primary) 18.dp else 16.dp))
            .background(background)
            .then(
                if (!primary) {
                    Modifier.border(
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = if (primary) 14.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PlaybackDecisionCard(
    playableMedia: PlayableMedia?,
    selectedSource: PlayInfo?,
    selectedEpisodeIndex: Int,
    resumePositionMs: Long,
    phoneLayout: Boolean
) {
    val episodeName = selectedSource?.urlInfos?.getOrNull(selectedEpisodeIndex)?.name.orEmpty()
    val decisionText = when {
        playableMedia == null -> "选定线路后可直接预览，适合先试播再进全屏。"
        playableMedia.requiresParse -> "当前线路返回的是中转页，建议优先切换其他线路。"
        resumePositionMs > 0 -> "已保存上次进度，适合直接继续观看。"
        else -> "当前片源已可直播，适合直接进入全屏沉浸观看。"
    }
    TvSurface(onClick = {}) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "播放建议",
                color = AppTextPrimary,
                fontSize = if (phoneLayout) 15.sp else 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = buildString {
                    append(decisionText)
                    if (episodeName.isNotBlank()) append(" 当前选择：").append(episodeName).append("。")
                },
                color = AppTextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun FullscreenIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        ExpandCornersIcon(size = 16.dp, color = AppTextPrimary)
    }
}

@Composable
private fun ExpandCornersIcon(
    size: Dp,
    color: Color
) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = size.toPx() * 0.1f
        val inset = stroke
        val arm = size.toPx() * 0.32f
        val max = size.toPx() - inset

        drawLine(color, start = androidx.compose.ui.geometry.Offset(inset, arm + inset), end = androidx.compose.ui.geometry.Offset(inset, inset), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, start = androidx.compose.ui.geometry.Offset(inset, inset), end = androidx.compose.ui.geometry.Offset(arm + inset, inset), strokeWidth = stroke, cap = StrokeCap.Round)

        drawLine(color, start = androidx.compose.ui.geometry.Offset(max - arm, inset), end = androidx.compose.ui.geometry.Offset(max, inset), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, start = androidx.compose.ui.geometry.Offset(max, inset), end = androidx.compose.ui.geometry.Offset(max, arm + inset), strokeWidth = stroke, cap = StrokeCap.Round)

        drawLine(color, start = androidx.compose.ui.geometry.Offset(inset, max - arm), end = androidx.compose.ui.geometry.Offset(inset, max), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, start = androidx.compose.ui.geometry.Offset(inset, max), end = androidx.compose.ui.geometry.Offset(arm + inset, max), strokeWidth = stroke, cap = StrokeCap.Round)

        drawLine(color, start = androidx.compose.ui.geometry.Offset(max - arm, max), end = androidx.compose.ui.geometry.Offset(max, max), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, start = androidx.compose.ui.geometry.Offset(max, max - arm), end = androidx.compose.ui.geometry.Offset(max, max), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

private tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CurrentPlaybackInfo(
    selectedSource: PlayInfo?,
    playableMedia: PlayableMedia?,
    siteLabel: String,
    phoneLayout: Boolean
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (siteLabel.isNotBlank()) {
            DetailChip(text = siteLabel)
        }
        if (selectedSource != null) {
            DetailChip(text = "共 ${selectedSource.urlInfos.size} 集")
        }
        if (phoneLayout && playableMedia != null) {
            DetailChip(text = "播放: ${playableMedia.resolutionType.label}")
            if (playableMedia.resolutionNote.isNotBlank()) {
                DetailChip(text = playableMedia.resolutionNote)
            }
        }
    }
}

@Composable
internal fun CenterMessage(message: String, color: Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = color)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SourceRow(
    sources: List<PlayInfo>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    phoneLayout: Boolean
) {
    if (phoneLayout) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            sources.forEachIndexed { index, source ->
                PhoneSelectorChip(
                    text = source.flag,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) }
                )
            }
        }
        return
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sources.forEachIndexed { index, source ->
            TvDetailChoiceChip(
                text = source.flag,
                selected = index == selectedIndex,
                onClick = { onSelect(index) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EpisodeGrid(
    compact: Boolean,
    phoneLayout: Boolean,
    playInfo: PlayInfo?,
    selectedIndex: Int,
    onSelect: (PlayInfo, Int) -> Unit
) {
    val episodes = playInfo?.urlInfos.orEmpty()
    if (episodes.isEmpty()) {
        Text(text = "当前线路暂无可播放集数", color = AppTextMuted, fontSize = 14.sp)
        return
    }

    if (phoneLayout) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            episodes.chunked(3).forEachIndexed { rowIndex, rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEachIndexed { columnIndex, episode ->
                        val actualIndex = rowIndex * 3 + columnIndex
                        PhoneEpisodeCard(
                            text = episode.displayLabel(actualIndex),
                            selected = actualIndex == selectedIndex,
                            onClick = { playInfo?.let { onSelect(it, actualIndex) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    } else if (compact) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            episodes.forEachIndexed { index, episode ->
                TvEpisodeTile(
                    text = episode.displayLabel(index),
                    selected = index == selectedIndex,
                    onClick = { playInfo?.let { onSelect(it, index) } }
                )
            }
        }
    } else {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            episodes.forEachIndexed { index, episode ->
                TvEpisodeTile(
                    text = episode.displayLabel(index),
                    selected = index == selectedIndex,
                    onClick = { playInfo?.let { onSelect(it, index) } }
                )
            }
        }
    }
}

private fun UrlInfo.displayLabel(index: Int): String {
    val raw = name.orEmpty().trim()
    val normalized = raw.lowercase()
    return when {
        raw.isBlank() -> "第 ${index + 1} 集"
        normalized == "播放" -> "第 ${index + 1} 集"
        normalized == "立即播放" -> "第 ${index + 1} 集"
        else -> raw
    }
}

@Composable
private fun PhoneSelectorChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) BrandSecondary.copy(alpha = 0.12f)
                else Color.White.copy(alpha = 0.04f)
            )
            .border(
                border = BorderStroke(
                    1.dp,
                    if (selected) BrandSecondary.copy(alpha = 0.34f)
                    else Color.White.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) AppTextPrimary else AppTextSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun TvDetailChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    TvSurface(
        onClick = onClick,
        modifier = Modifier
            .clip(shape)
            .border(
                BorderStroke(
                    1.dp,
                    if (selected) BrandSecondary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f)
                ),
                shape
            ),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) BrandSecondary.copy(alpha = 0.16f) else AppSurface.copy(alpha = 0.9f),
            focusedContainerColor = if (selected) BrandSecondary.copy(alpha = 0.28f) else AppSurfaceVariant,
            pressedContainerColor = if (selected) BrandSecondary.copy(alpha = 0.22f) else AppSurfaceVariant.copy(alpha = 0.96f),
            disabledContainerColor = AppSurfaceVariant.copy(alpha = 0.58f)
        )
    ) {
        Text(
            text = if (selected) "当前  $text" else text,
            color = if (selected) AppTextPrimary else AppTextSecondary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun TvEpisodeTile(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    TvSurface(
        onClick = onClick,
        modifier = Modifier
            .width(148.dp)
            .clip(shape)
            .border(
                BorderStroke(
                    1.dp,
                    if (selected) BrandPrimary.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.08f)
                ),
                shape
            ),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) BrandPrimary.copy(alpha = 0.14f) else AppSurface.copy(alpha = 0.9f),
            focusedContainerColor = if (selected) BrandPrimary.copy(alpha = 0.24f) else AppSurfaceVariant,
            pressedContainerColor = if (selected) BrandPrimary.copy(alpha = 0.18f) else AppSurfaceVariant.copy(alpha = 0.96f),
            disabledContainerColor = AppSurfaceVariant.copy(alpha = 0.58f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (selected) {
                Text(
                    text = "当前播放",
                    color = BrandPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = text,
                color = AppTextPrimary,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PhoneEpisodeCard(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) Color.White.copy(alpha = 0.10f)
                else Color.White.copy(alpha = 0.035f)
            )
            .border(
                border = BorderStroke(
                    1.dp,
                    if (selected) Color.White.copy(alpha = 0.22f)
                    else Color.White.copy(alpha = 0.07f)
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (selected) {
                Text(
                    text = "当前播放",
                    color = BrandSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = text,
                color = AppTextPrimary,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
