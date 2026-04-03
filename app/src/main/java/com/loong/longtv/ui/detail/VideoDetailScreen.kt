package com.loong.longtv.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.loong.longtv.Screen
import com.loong.longtv.data.bean.PlayInfo
import com.loong.longtv.data.player.PlayableMedia
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.theme.AppCardOverlay
import com.loong.longtv.ui.theme.AppBackground
import com.loong.longtv.ui.theme.AppBackgroundElevated
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.theme.BrandSecondary
import com.loong.longtv.ui.theme.BrandAccent
import com.loong.longtv.ui.widgets.AppButton
import com.loong.longtv.ui.widgets.SectionHeader
import com.loong.longtv.ui.widgets.SmartAsyncImage
import com.loong.longtv.ui.widgets.TvSurface

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoDetailScreen(
    navigator: NavController,
    key: String,
    id: String,
    viewModel: VideoDetailViewModel = hiltViewModel()
) {
    val deviceClass = currentDeviceClass()
    val compact = deviceClass != DeviceClass.TV
    val phoneLayout = deviceClass == DeviceClass.PHONE
    val horizontalPadding = when (deviceClass) {
        DeviceClass.PHONE -> 16.dp
        DeviceClass.TABLET -> 20.dp
        DeviceClass.TV -> 24.dp
    }
    val verticalPadding = when (deviceClass) {
        DeviceClass.PHONE -> 16.dp
        DeviceClass.TABLET -> 18.dp
        DeviceClass.TV -> 20.dp
    }
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                AppBackground,
                                AppBackgroundElevated,
                                AppBackground
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compact) 238.dp else 300.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black,
                                    Color.Black.copy(alpha = 0.88f)
                                )
                            )
                        )
                ) {
                    DetailPreviewBlock(
                        modifier = Modifier.fillMaxSize(),
                        detail = detail,
                        playableMedia = playableMedia,
                        resumePositionMs = state.resumePositionMs,
                        selectedSource = selectedSource,
                        selectedEpisodeIndex = state.selectedEpisodeIndex,
                        onBack = { navigator.popBackStack() },
                        onOpenFullscreen = ::openFullscreen,
                        onProgressChanged = viewModel::saveProgress
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                ) {
                    if (compact) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            DetailInfoBlock(
                                navigator = navigator,
                                state = state,
                                detail = detail,
                                selectedSource = selectedSource,
                                playableMedia = playableMedia,
                                resumePositionMs = state.resumePositionMs,
                                phoneLayout = phoneLayout,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onPlayCurrent = {
                                    if (selectedSource != null) {
                                        viewModel.playEpisode(selectedSource, state.selectedEpisodeIndex)
                                    }
                                },
                                onRetryCurrent = viewModel::retryCurrentEpisode,
                                onClearError = viewModel::clearError
                            )
                        }
                    } else {
                        DetailInfoBlock(
                            navigator = navigator,
                            state = state,
                            detail = detail,
                            selectedSource = selectedSource,
                            playableMedia = playableMedia,
                            resumePositionMs = state.resumePositionMs,
                            phoneLayout = false,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onPlayCurrent = {
                                if (selectedSource != null) {
                                    viewModel.playEpisode(selectedSource, state.selectedEpisodeIndex)
                                }
                            },
                            onRetryCurrent = viewModel::retryCurrentEpisode,
                            onClearError = viewModel::clearError
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(
                        title = "播放线路",
                        subtitle = if (phoneLayout) "先切换来源，再决定是否播放当前集" else "切换不同来源尝试更稳定的播放效果"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SourceRow(
                        sources = sources,
                        selectedIndex = state.selectedSourceIndex,
                        onSelect = viewModel::selectSource,
                        phoneLayout = phoneLayout
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(
                        title = "选集",
                        subtitle = if (phoneLayout) "手机端默认按网格展示，便于快速点选" else "快速定位到想看的集数"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    EpisodeGrid(
                        compact = compact,
                        phoneLayout = phoneLayout,
                        playInfo = selectedSource,
                        selectedIndex = state.selectedEpisodeIndex,
                        onSelect = { playInfo, episodeIndex ->
                            viewModel.playEpisode(playInfo, episodeIndex)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailPreviewBlock(
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
    val title = detail.vod_name.safeText("未命名视频")
    val episodeName = selectedSource?.urlInfos?.getOrNull(selectedEpisodeIndex)?.name.orEmpty()
    var currentPositionMs by remember(playableMedia?.url, resumePositionMs) { mutableLongStateOf(resumePositionMs) }
    var durationMs by remember(playableMedia?.url) { mutableLongStateOf(0L) }
    var isPlaying by remember(playableMedia?.url) { mutableStateOf(false) }
    var isBuffering by remember(playableMedia?.url) { mutableStateOf(false) }

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
                onProgressChanged = { position, duration ->
                    currentPositionMs = position
                    durationMs = duration
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

        PreviewBackButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp),
            onClick = onBack
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
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = when {
                    playableMedia == null -> "--:--"
                    playableMedia.requiresParse -> "--:--"
                    else -> formatPlayerTime(currentPositionMs)
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
private fun DetailInfoBlock(
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

    Column(modifier = modifier) {
        Text(
            text = title,
            color = AppTextPrimary,
            fontSize = if (phoneLayout) 22.sp else 24.sp,
            maxLines = if (phoneLayout) 2 else 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DetailChip(text = state.spiderFamily.label)
            DetailChip(text = state.siteName.ifBlank { state.siteKey })
            if (!phoneLayout) {
                DetailChip(text = "key: ${state.siteKey}")
                DetailChip(text = "api: ${state.siteApi}")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = listOf(typeName, year, area, remarks)
                .filter { it.isNotBlank() }
                .joinToString(" / "),
            color = AppTextSecondary,
            fontSize = if (phoneLayout) 13.sp else 14.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = content,
            color = AppTextMuted,
            fontSize = if (phoneLayout) 13.sp else 14.sp,
            lineHeight = if (phoneLayout) 19.sp else 21.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        PlaybackDecisionCard(
            playableMedia = playableMedia,
            selectedSource = selectedSource,
            selectedEpisodeIndex = state.selectedEpisodeIndex,
            resumePositionMs = resumePositionMs,
            phoneLayout = phoneLayout
        )
        Spacer(modifier = Modifier.height(16.dp))
        CurrentPlaybackInfo(
            selectedSource = selectedSource,
            selectedEpisodeIndex = state.selectedEpisodeIndex,
            playableMedia = playableMedia,
            resumePositionMs = resumePositionMs,
            phoneLayout = phoneLayout
        )
        Spacer(modifier = Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppButton(text = if (state.favorite) "取消收藏" else "收藏", onClick = onToggleFavorite)
            if (selectedSource != null && playableMedia == null && selectedSource.urlInfos.isNotEmpty()) {
                AppButton(text = if (resumePositionMs > 0) "继续观看" else "播放当前集", onClick = onPlayCurrent)
            }
            if (playableMedia != null && !playableMedia.requiresParse && playableMedia.url.isNotBlank()) {
                AppButton(text = "重新获取片源", onClick = onRetryCurrent)
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
                    AppButton(text = "重试当前集", onClick = onRetryCurrent)
                    AppButton(text = "清除提示", onClick = onClearError)
                }
            }
        }
    }
}

private fun String?.safeText(fallback: String = ""): String {
    return this.orEmpty().takeIf { it.isNotBlank() } ?: fallback
}

@Composable
private fun DetailChip(text: String) {
    TvSurface(onClick = {}) {
        Text(
            text = text,
            color = AppTextPrimary,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
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
private fun PreviewBackButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val stroke = size.minDimension * 0.14f
            val centerY = size.height / 2f
            val leftX = size.width * 0.22f
            val rightX = size.width * 0.76f
            drawLine(
                color = AppTextPrimary,
                start = androidx.compose.ui.geometry.Offset(rightX, size.height * 0.16f),
                end = androidx.compose.ui.geometry.Offset(leftX, centerY),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = AppTextPrimary,
                start = androidx.compose.ui.geometry.Offset(leftX, centerY),
                end = androidx.compose.ui.geometry.Offset(rightX, size.height * 0.84f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CurrentPlaybackInfo(
    selectedSource: PlayInfo?,
    selectedEpisodeIndex: Int,
    playableMedia: PlayableMedia?,
    resumePositionMs: Long,
    phoneLayout: Boolean
) {
    val episodeName = selectedSource?.urlInfos?.getOrNull(selectedEpisodeIndex)?.name.orEmpty()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selectedSource != null) {
            DetailChip(text = "线路: ${selectedSource.flag}")
            DetailChip(text = "集数: ${episodeName.ifBlank { (selectedEpisodeIndex + 1).toString() }}")
            if (!phoneLayout) {
                DetailChip(text = "共 ${selectedSource.urlInfos.size} 集")
            }
        }
        if (playableMedia != null) {
            DetailChip(text = "播放: ${playableMedia.resolutionType.label}")
            if (playableMedia.resolutionNote.isNotBlank()) {
                DetailChip(text = playableMedia.resolutionNote)
            }
        }
        if (resumePositionMs > 0) {
            DetailChip(text = "进度: ${formatPlayerTime(resumePositionMs)}")
        }
    }
}

@Composable
private fun CenterMessage(message: String, color: Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = color)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SourceRow(
    sources: List<PlayInfo>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    phoneLayout: Boolean
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sources.forEachIndexed { index, source ->
            AppButton(
                text = if (index == selectedIndex) {
                    if (phoneLayout) "当前: ${source.flag}" else "• ${source.flag}"
                } else {
                    source.flag
                },
                onClick = { onSelect(index) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EpisodeGrid(
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
            episodes.chunked(2).forEachIndexed { rowIndex, rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEachIndexed { columnIndex, episode ->
                        val actualIndex = rowIndex * 2 + columnIndex
                        AppButton(
                            text = if (actualIndex == selectedIndex) "当前 ${episode.name}" else episode.name,
                            onClick = { playInfo?.let { onSelect(it, actualIndex) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
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
                AppButton(
                    text = if (index == selectedIndex) "• ${episode.name}" else episode.name,
                    onClick = { playInfo?.let { onSelect(it, index) } }
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(140.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            itemsIndexed(episodes) { index, episode ->
                TvSurface(onClick = { playInfo?.let { onSelect(it, index) } }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (index == selectedIndex) "• ${episode.name}" else episode.name,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
