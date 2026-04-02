package com.loong.longtv.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.loong.longtv.Screen
import com.loong.longtv.data.bean.PlayInfo
import com.loong.longtv.data.player.PlayableMedia
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.widgets.AppButton
import com.loong.longtv.ui.widgets.AppScreen
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

            AppScreen(
                modifier = Modifier.then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            ) {
                SectionHeader(
                    title = detail.vod_name.safeText("未命名视频"),
                    subtitle = if (phoneLayout) {
                        "手机端优先展示播放决策信息，减少无关技术字段干扰。"
                    } else {
                        "查看剧集信息、切换线路并进入全屏播放"
                    }
                )
                Spacer(modifier = Modifier.height(18.dp))
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        DetailPreviewBlock(
                            compact = compact,
                            detail = detail,
                            playableMedia = playableMedia,
                            resumePositionMs = state.resumePositionMs,
                            onProgressChanged = viewModel::saveProgress
                        )
                        DetailInfoBlock(
                            navigator = navigator,
                            state = state,
                            detail = detail,
                            selectedSource = selectedSource,
                            playableMedia = playableMedia,
                            phoneLayout = phoneLayout,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onOpenFullscreen = ::openFullscreen,
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
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        DetailPreviewBlock(
                            compact = compact,
                            detail = detail,
                            playableMedia = playableMedia,
                            resumePositionMs = state.resumePositionMs,
                            onProgressChanged = viewModel::saveProgress
                        )
                        DetailInfoBlock(
                            navigator = navigator,
                            modifier = Modifier.weight(1f),
                            state = state,
                            detail = detail,
                            selectedSource = selectedSource,
                            playableMedia = playableMedia,
                            phoneLayout = false,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onOpenFullscreen = ::openFullscreen,
                            onPlayCurrent = {
                                if (selectedSource != null) {
                                    viewModel.playEpisode(selectedSource, state.selectedEpisodeIndex)
                                }
                            },
                            onRetryCurrent = viewModel::retryCurrentEpisode,
                            onClearError = viewModel::clearError
                        )
                    }
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

@Composable
private fun DetailPreviewBlock(
    compact: Boolean,
    detail: com.loong.longtv.data.bean.Video,
    playableMedia: PlayableMedia?,
    resumePositionMs: Long,
    onProgressChanged: (Long, Long) -> Unit
) {
    val previewModifier = if (compact) {
        Modifier
            .fillMaxWidth()
            .height(220.dp)
    } else {
        Modifier
            .width(420.dp)
            .height(236.dp)
    }
    if (playableMedia?.url?.isNotBlank() == true && !playableMedia.requiresParse) {
        VideoPlayerView(
            modifier = previewModifier,
            videoUrl = playableMedia.url,
            requestHeaders = playableMedia.headers,
            initialPositionMs = resumePositionMs,
            onProgressChanged = onProgressChanged
        )
    } else {
        val title = detail.vod_name.safeText("未命名视频")
        SmartAsyncImage(
            model = detail.vod_pic,
            contentDescription = title,
            modifier = previewModifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            fallbackText = title
        )
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
    phoneLayout: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenFullscreen: (PlayableMedia, String) -> Unit,
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
        CurrentPlaybackInfo(
            selectedSource = selectedSource,
            selectedEpisodeIndex = state.selectedEpisodeIndex,
            playableMedia = playableMedia,
            phoneLayout = phoneLayout
        )
        Spacer(modifier = Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppButton(text = if (state.favorite) "取消收藏" else "收藏", onClick = onToggleFavorite)
            if (selectedSource != null && playableMedia == null && selectedSource.urlInfos.isNotEmpty()) {
                AppButton(text = "播放当前集", onClick = onPlayCurrent)
            }
            if (playableMedia != null && !playableMedia.requiresParse && playableMedia.url.isNotBlank()) {
                val episodeName = selectedSource?.urlInfos?.getOrNull(state.selectedEpisodeIndex)?.name.orEmpty()
                AppButton(
                    text = "全屏播放",
                    onClick = { onOpenFullscreen(playableMedia, episodeName) }
                )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CurrentPlaybackInfo(
    selectedSource: PlayInfo?,
    selectedEpisodeIndex: Int,
    playableMedia: PlayableMedia?,
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
