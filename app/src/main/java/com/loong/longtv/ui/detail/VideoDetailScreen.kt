package com.loong.longtv.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.loong.longtv.ui.common.isCompactScreen
import com.loong.longtv.ui.widgets.AppButton
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
    val compact = isCompactScreen()
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
                    .background(Color.Black)
                    .padding(24.dp)
                    .then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            ) {
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
                Text(text = "播放线路", color = Color.White, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                SourceRow(
                    sources = sources,
                    selectedIndex = state.selectedSourceIndex,
                    onSelect = viewModel::selectSource
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "选集", color = Color.White, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                EpisodeGrid(
                    compact = compact,
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
        SmartAsyncImage(
            model = detail.vod_pic,
            contentDescription = detail.vod_name,
            modifier = previewModifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            fallbackText = detail.vod_name.ifBlank { "暂无封面" }
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
    onToggleFavorite: () -> Unit,
    onOpenFullscreen: (PlayableMedia, String) -> Unit,
    onPlayCurrent: () -> Unit,
    onRetryCurrent: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = detail.vod_name, color = Color.White, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DetailChip(text = state.spiderFamily.label)
            DetailChip(text = "site: ${state.siteName.ifBlank { state.siteKey }}")
            DetailChip(text = "key: ${state.siteKey}")
            DetailChip(text = "api: ${state.siteApi}")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = listOf(detail.type_name, detail.vod_year, detail.vod_area, detail.vod_remarks)
                .filter { it.isNotBlank() }
                .joinToString(" / "),
            color = Color.LightGray,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = detail.vod_content.ifBlank { "暂无简介\nID: ${detail.vod_id}" },
            color = Color.Gray,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        CurrentPlaybackInfo(
            selectedSource = selectedSource,
            selectedEpisodeIndex = state.selectedEpisodeIndex,
            playableMedia = playableMedia
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
            Text(text = state.error ?: "", color = Color.Red, fontSize = 13.sp)
            if (selectedSource != null && selectedSource.urlInfos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppButton(text = "重试当前集", onClick = onRetryCurrent)
                    AppButton(text = "清除提示", onClick = onClearError)
                }
            }
        }
    }
}

@Composable
private fun DetailChip(text: String) {
    TvSurface(onClick = {}) {
        Text(
            text = text,
            color = Color.White,
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
    playableMedia: PlayableMedia?
) {
    val episodeName = selectedSource?.urlInfos?.getOrNull(selectedEpisodeIndex)?.name.orEmpty()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selectedSource != null) {
            DetailChip(text = "线路: ${selectedSource.flag}")
            DetailChip(text = "集数: ${episodeName.ifBlank { (selectedEpisodeIndex + 1).toString() }}")
            DetailChip(text = "共 ${selectedSource.urlInfos.size} 集")
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
    onSelect: (Int) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        sources.forEachIndexed { index, source ->
            AppButton(
                text = if (index == selectedIndex) "• ${source.flag}" else source.flag,
                onClick = { onSelect(index) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EpisodeGrid(
    compact: Boolean,
    playInfo: PlayInfo?,
    selectedIndex: Int,
    onSelect: (PlayInfo, Int) -> Unit
) {
    val episodes = playInfo?.urlInfos.orEmpty()
    if (episodes.isEmpty()) {
        Text(text = "当前线路暂无可播放集数", color = Color.Gray, fontSize = 14.sp)
        return
    }

    if (compact) {
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
            modifier = Modifier.fillMaxWidth()
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
