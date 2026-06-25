package com.loong.longtv.ui.shared.detail

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.ContextWrapper
import android.media.AudioManager
import android.os.Build
import android.text.Html
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
import androidx.compose.runtime.key
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
import androidx.compose.ui.layout.onSizeChanged
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
import com.loong.longtv.engine.source.model.PlayLine
import com.loong.longtv.engine.source.model.Episode
import com.loong.longtv.engine.player.PlayableMedia
import com.loong.longtv.ui.theme.AppCardOverlay
import com.loong.longtv.ui.theme.AppBackground
import com.loong.longtv.ui.theme.AppBackgroundElevated
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppSurface
import com.loong.longtv.ui.theme.AppSurfaceVariant
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.AppThemeTokens
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.theme.BrandSecondary
import com.loong.longtv.ui.theme.BrandAccent
import com.loong.longtv.ui.shared.widgets.AppButton
import com.loong.longtv.ui.shared.widgets.SectionHeader
import com.loong.longtv.ui.shared.widgets.SmartAsyncImage
import com.loong.longtv.ui.shared.widgets.TvSurface
import com.loong.player.PlayerFeedbackPopup
import com.loong.player.PhoneFullscreenGestureLayer
import com.loong.player.PhoneGestureType
import com.loong.player.PlayerLoadingOverlay
import com.loong.player.SeekOverlayState
import com.loong.player.rememberPlayerInteractionState
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
        detail: com.loong.longtv.engine.source.model.VodInfo,
        sources: List<PlayLine>,
        selectedSource: PlayLine?,
        playableMedia: PlayableMedia?,
        openingFullscreen: Boolean,
        onOpenFullscreen: (PlayableMedia, String, Long) -> Unit,
        onProgressChanged: (Long, Long) -> Unit,
        onToggleFavorite: () -> Unit,
        onPlayCurrent: () -> Unit,
        onRetryCurrent: () -> Unit,
        onClearError: () -> Unit,
        onSelectSource: (Int) -> Unit,
        onSelectEpisode: (PlayLine, Int) -> Unit
    ) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentBackStackEntry by navigator.currentBackStackEntryAsState()
    val savedStateHandle = currentBackStackEntry?.savedStateHandle
    var reopenFullscreenAfterSwitch by remember(key, id) { mutableStateOf(false) }
    var openingFullscreen by remember(key, id) { mutableStateOf(false) }

    fun openFullscreen(playableMedia: PlayableMedia, episodeName: String, positionMs: Long) {
        openingFullscreen = true
        navigator.navigate(
            Screen.FullscreenPlayer.createRoute(
                title = buildString {
                    append(state.detail?.name?.ifBlank { "全屏播放" } ?: "全屏播放")
                    if (episodeName.isNotBlank()) append(" - ").append(episodeName)
                },
                url = playableMedia.url,
                headers = playableMedia.headers,
                position = positionMs,
                episodeIndex = state.selectedEpisodeIndex,
                episodeCount = state.detail?.getPlayLines()?.getOrNull(state.selectedSourceIndex)?.episodes?.size ?: 0
            )
        )
    }

    LaunchedEffect(key, id) {
        viewModel.loadDetail(key, id)
    }

    LaunchedEffect(currentBackStackEntry) {
        val position = savedStateHandle?.remove<Long>("fullscreen_position") ?: return@LaunchedEffect
        openingFullscreen = false
        viewModel.applyFullscreenProgress(position)
    }

    LaunchedEffect(currentBackStackEntry, state.detail, state.selectedSourceIndex) {
        val targetIndex = savedStateHandle?.remove<Int>("fullscreen_target_episode_index") ?: return@LaunchedEffect
        reopenFullscreenAfterSwitch = savedStateHandle?.remove<Boolean>("fullscreen_reenter") == true
        val playInfo = state.detail?.getPlayLines()?.getOrNull(state.selectedSourceIndex) ?: return@LaunchedEffect
        if (targetIndex in playInfo.episodes.indices) {
            viewModel.playEpisode(playInfo, targetIndex)
        }
    }

    LaunchedEffect(state.playableMedia, reopenFullscreenAfterSwitch, state.selectedEpisodeIndex) {
        val playableMedia = state.playableMedia ?: return@LaunchedEffect
        if (!reopenFullscreenAfterSwitch || playableMedia.needsParse() || playableMedia.url.isBlank()) return@LaunchedEffect
        val episodeName = state.detail
            ?.getPlayLines()
            ?.getOrNull(state.selectedSourceIndex)
            ?.episodes
            ?.getOrNull(state.selectedEpisodeIndex)
            ?.name
            .orEmpty()
        reopenFullscreenAfterSwitch = false
        openFullscreen(playableMedia, episodeName, state.resumePositionMs)
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
            val sources = detail.getPlayLines()
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
                openingFullscreen,
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
    detail: com.loong.longtv.engine.source.model.VodInfo,
    playableMedia: PlayableMedia?,
    playerLoading: Boolean,
    resumePositionMs: Long,
    selectedSource: PlayLine?,
    selectedEpisodeIndex: Int,
    sharedSessionReleaseDelayMs: Long,
    onBack: () -> Unit,
    onOpenFullscreen: (PlayableMedia, String, Long) -> Unit,
    onProgressChanged: (Long, Long) -> Unit,
    onPlaybackEnded: () -> Unit = {}
) {
    val context = LocalContext.current
    val audioManager = remember(context) { context.getSystemService(AUDIO_SERVICE) as AudioManager }
    val activity = context.findActivity()
    val haptic = LocalHapticFeedback.current
    val title = detail.name.safeText("未命名视频")
    val selectedEpisode = selectedSource?.episodes?.getOrNull(selectedEpisodeIndex)
    val episodeName = selectedEpisode?.name.orEmpty()
    val previewSessionKey = listOf(
        selectedSource?.flag.orEmpty(),
        selectedEpisodeIndex.toString(),
        selectedEpisode?.url.orEmpty(),
        playableMedia?.url.orEmpty()
    ).joinToString("#")
    val controlState = key(previewSessionKey) { rememberPlayerControlState() }
    val interactionState = rememberPlayerInteractionState(initialPositionMs = resumePositionMs)
    val topBarTitle = buildString {
        append(title)
        if (episodeName.isNotBlank()) append(" · ").append(episodeName)
    }
    var currentPositionMs by remember(previewSessionKey, resumePositionMs) { mutableLongStateOf(resumePositionMs) }
    var durationMs by remember(previewSessionKey) { mutableLongStateOf(0L) }
    var isPlaying by remember(previewSessionKey) { mutableStateOf(false) }
    var isBuffering by remember(previewSessionKey) { mutableStateOf(false) }
    var playerReady by remember(previewSessionKey) { mutableStateOf(false) }
    var progressBarWidthPx by remember(previewSessionKey) { mutableStateOf(1f) }
    var brightnessLevel by remember(previewSessionKey) {
        mutableStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f)
    }
    var volumePercent by remember(previewSessionKey) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        mutableStateOf(((audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f) / maxVolume).roundToInt())
    }

    LaunchedEffect(previewSessionKey) {
        interactionState.clearForNewMedia(resumePositionMs)
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
        val target = (maxVolume * (clamped / 100f)).roundToInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        volumePercent = clamped
        interactionState.showAdjustment(PhoneGestureType.VOLUME, clamped)
    }

    fun positionFromBarOffset(offsetX: Float): Long {
        if (durationMs <= 0 || progressBarWidthPx <= 0f) return currentPositionMs
        val percent = (offsetX / progressBarWidthPx).coerceIn(0f, 1f)
        return (durationMs * percent).roundToLong()
    }

    fun seekToPosition(positionMs: Long) {
        val duration = durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE
        currentPositionMs = positionMs.coerceIn(0L, duration)
        interactionState.commitScrub(currentPositionMs)
        controlState.seekTo(currentPositionMs)
        onProgressChanged(currentPositionMs, durationMs)
    }

    fun togglePlayPauseFromGesture() {
        val targetIsPlaying = !isPlaying
        controlState.togglePlayPause()
        interactionState.showPlayPause(targetIsPlaying)
    }

    fun togglePlayPauseFromControl() {
        controlState.togglePlayPause()
        interactionState.registerInteraction()
    }

    Box(
        modifier = modifier
            .background(AppCardOverlay)
    ) {
        if (playableMedia?.url?.isNotBlank() == true && !playableMedia.needsParse()) {
            VideoPlayerView(
                modifier = Modifier.fillMaxSize(),
                videoUrl = playableMedia.url,
                requestHeaders = playableMedia.headers,
                initialPositionMs = resumePositionMs,
                repeat = false,
                enableNativeController = false,
                sharedSessionEnabled = true,
                sharedSessionReleaseDelayMs = sharedSessionReleaseDelayMs,
                controlState = controlState,
                onProgressChanged = { position, duration ->
                    currentPositionMs = position
                    durationMs = duration
                    interactionState.syncPlaybackPosition(position)
                    onProgressChanged(position, duration)
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
                onPlaybackEnded = onPlaybackEnded,
                onSurfaceTap = null
            )
        } else {
            SmartAsyncImage(
                model = detail.pic,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                fallbackText = title
            )
        }

        val playableReady = playableMedia?.url?.isNotBlank() == true && !playableMedia.needsParse()
        val seekFeedbackVisible = interactionState.gestureInProgress ||
            interactionState.isScrubbing ||
            interactionState.feedbackOverlay != null
        val showPlayerLoading = playableReady &&
            !isPlaying &&
            !seekFeedbackVisible &&
            ((playerLoading && !playerReady) || !playerReady || isBuffering)
        if (showPlayerLoading) {
            PlayerLoadingOverlay(
                modifier = Modifier.matchParentSize()
            )
        }

        if (playableMedia?.url?.isNotBlank() == true && !playableMedia.needsParse()) {
            PhoneFullscreenGestureLayer(
                modifier = Modifier.matchParentSize(),
                resetKey = previewSessionKey,
                durationMs = durationMs,
                currentPositionMs = currentPositionMs,
                brightnessPercent = (brightnessLevel * 100f).roundToInt().coerceIn(0, 100),
                volumePercent = volumePercent,
                reservedBottom = 0.dp,
                onSingleTap = interactionState::toggleControls,
                onDoubleTap = ::togglePlayPauseFromGesture,
                onGestureStart = {
                    interactionState.beginGesture()
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
                    controlState.seekTo(state.targetPositionMs)
                    onProgressChanged(state.targetPositionMs, durationMs)
                },
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

        PlayerFeedbackPopup(overlay = interactionState.feedbackOverlay)

        val readyPlayableMedia = playableMedia
            ?.takeIf { it.url.isNotBlank() && !it.needsParse() }

        if (interactionState.controlsVisible) {
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
                        .padding(end = 10.dp)
                        .clickable(onClick = ::togglePlayPauseFromControl)
                        .padding(4.dp)
                )
                Text(
                    text = when {
                        playableMedia == null -> "--:--"
                        playableMedia.needsParse() -> "--:--"
                        else -> formatPlayerTime(if (interactionState.isScrubbing) interactionState.scrubPositionMs else currentPositionMs)
                    },
                    color = AppTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 10.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
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
                        }
                        .onSizeChanged { progressBarWidthPx = it.width.toFloat().coerceAtLeast(1f) },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .height(5.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(playbackProgress(if (interactionState.isScrubbing) interactionState.scrubPositionMs else currentPositionMs, durationMs))
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BrandPrimary, Color(0xFFFF7F58))
                                )
                            )
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth(playbackProgress(if (interactionState.isScrubbing) interactionState.scrubPositionMs else currentPositionMs, durationMs)),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFFE7ECF3))
                        )
                    }
                }
                Text(
                    text = when {
                        playableMedia == null -> "--:--"
                        playableMedia.needsParse() -> "--:--"
                        durationMs > 0 -> formatPlayerTime(durationMs)
                        else -> "--:--"
                    },
                    color = AppTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 10.dp)
                )
                if (readyPlayableMedia != null) {
                    FullscreenIconButton(
                        modifier = Modifier.padding(start = 10.dp),
                        onClick = {
                            onOpenFullscreen(
                                readyPlayableMedia,
                                episodeName,
                                if (interactionState.isScrubbing) interactionState.scrubPositionMs else currentPositionMs
                            )
                        }
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
    Canvas(modifier = modifier.size(16.dp)) {
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
    detail: com.loong.longtv.engine.source.model.VodInfo,
    selectedSource: PlayLine?,
    playableMedia: PlayableMedia?,
    resumePositionMs: Long,
    phoneLayout: Boolean,
    onToggleFavorite: () -> Unit,
    onPlayCurrent: () -> Unit,
    onRetryCurrent: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = detail.name.safeText("未命名视频")
    val typeName = detail.typeName.safeText()
    val year = detail.year.safeText()
    val area = detail.area.safeText()
    val remarks = detail.remarks.safeText()
    val content = detail.content.safeDescriptionText("暂无简介\nID: ${detail.id.safeText()}")
    var showFullDescription by remember(title, content) { mutableStateOf(false) }
    var phoneDescriptionExpanded by remember(title, content) { mutableStateOf(false) }

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
            PhoneDescriptionSection(
                content = content,
                expanded = phoneDescriptionExpanded,
                onToggle = { phoneDescriptionExpanded = !phoneDescriptionExpanded }
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
                if (selectedSource != null && playableMedia == null && selectedSource.episodes.isNotEmpty()) {
                    TvDetailActionButton(
                        text = if (resumePositionMs > 0) "继续观看" else "播放当前集",
                        onClick = onPlayCurrent,
                        prominent = true
                    )
                }
                if (playableMedia != null && !playableMedia.needsParse() && playableMedia.url.isNotBlank()) {
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
                if (selectedSource != null && selectedSource.episodes.isNotEmpty()) {
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

    if (showFullDescription) {
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
    val shape = AppThemeTokens.buttonShape()
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

private fun String?.safeDescriptionText(fallback: String = ""): String {
    val raw = this.orEmpty().trim()
    if (raw.isBlank()) return fallback
    val decoded = if (raw.contains('<') || raw.contains('&')) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(raw).toString()
        }
    } else {
        raw
    }
    return decoded
        .replace('\u00A0', ' ')
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString("\n")
        .ifBlank { fallback }
}

@Composable
private fun PhoneDescriptionSection(
    content: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Text(
        text = content,
        color = AppTextMuted,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        maxLines = if (expanded) Int.MAX_VALUE else 3,
        overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis
    )
    Text(
        text = if (expanded) "收起" else "展开",
        color = BrandPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .padding(top = 8.dp)
            .clickable(onClick = onToggle)
    )
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
    selectedSource: PlayLine?,
    selectedEpisodeIndex: Int,
    playableMedia: PlayableMedia?,
    resumePositionMs: Long
) {
    val episodeName = selectedSource?.episodes?.getOrNull(selectedEpisodeIndex)?.name.orEmpty()
    val statusLabel = when {
        playableMedia == null -> "待加载"
        playableMedia.needsParse() -> "需换源"
        playableMedia.url.isBlank() -> "未就绪"
        else -> "可播放"
    }
    val statusColor = when {
        playableMedia == null -> AppTextMuted
        playableMedia.needsParse() -> BrandAccent
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
                DetailChip(text = "共 ${selectedSource.episodes.size} 集")
            }
            if (playableMedia != null) {
                DetailChip(text = playableMedia.type.label)
            }
        }
    }
}

@Composable
private fun PhoneDetailActionGroup(
    favorite: Boolean,
    selectedSource: PlayLine?,
    playableMedia: PlayableMedia?,
    error: String?,
    onToggleFavorite: () -> Unit,
    onRetryCurrent: () -> Unit,
    onClearError: () -> Unit
) {
    val canPlay = selectedSource != null && selectedSource.episodes.isNotEmpty()
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
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .then(
                if (!primary) {
                    Modifier.border(
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(8.dp)
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
    selectedSource: PlayLine?,
    selectedEpisodeIndex: Int,
    resumePositionMs: Long,
    phoneLayout: Boolean
) {
    val episodeName = selectedSource?.episodes?.getOrNull(selectedEpisodeIndex)?.name.orEmpty()
    val decisionText = when {
        playableMedia == null -> "选定线路后可直接预览，适合先试播再进全屏。"
        playableMedia.needsParse() -> "当前线路返回的是中转页，建议优先切换其他线路。"
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
            .size(44.dp)
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        ExpandCornersIcon(size = 16.dp, color = Color(0xFFE7ECF3).copy(alpha = 0.82f))
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
    selectedSource: PlayLine?,
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
            DetailChip(text = "共 ${selectedSource.episodes.size} 集")
        }
        if (phoneLayout && playableMedia != null) {
            DetailChip(text = "播放: ${playableMedia.type.label}")
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
    sources: List<PlayLine>,
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
    playInfo: PlayLine?,
    selectedIndex: Int,
    onSelect: (PlayLine, Int) -> Unit
) {
    val episodes = playInfo?.episodes.orEmpty()
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

private fun Episode.displayLabel(index: Int): String {
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
            .clip(RoundedCornerShape(8.dp))
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
                shape = RoundedCornerShape(8.dp)
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
    val shape = RoundedCornerShape(8.dp)
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
    val shape = RoundedCornerShape(8.dp)
    TvSurface(
        onClick = onClick,
        modifier = Modifier
            .width(148.dp)
            .height(46.dp)
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
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (selected) BrandPrimary
                        else Color.White.copy(alpha = 0.10f)
                    )
            )
            Text(
                text = text,
                color = AppTextPrimary,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
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
            .height(44.dp)
            .clip(AppThemeTokens.buttonShape())
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
                shape = AppThemeTokens.buttonShape()
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (selected) BrandSecondary
                        else Color.White.copy(alpha = 0.10f)
                    )
            )
            Text(
                text = text,
                color = AppTextPrimary,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
