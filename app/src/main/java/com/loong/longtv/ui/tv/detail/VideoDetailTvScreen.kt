package com.loong.longtv.ui.tv.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.loong.longtv.engine.source.model.PlayLine
import com.loong.longtv.engine.player.PlayableMedia
import com.loong.longtv.ui.shared.detail.VideoDetailUiState
import com.loong.longtv.ui.shared.detail.VideoDetailViewModel
import com.loong.longtv.ui.shared.detail.DetailInfoBlock
import com.loong.longtv.ui.shared.detail.DetailPreviewBlock
import com.loong.longtv.ui.shared.detail.EpisodeGrid
import com.loong.longtv.ui.shared.detail.SourceRow
import com.loong.longtv.ui.shared.detail.VideoDetailRouteContent
import com.loong.longtv.ui.theme.AppBackground
import com.loong.longtv.ui.theme.AppBackgroundElevated
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.BrandPrimary
import androidx.compose.foundation.BorderStroke
import androidx.tv.material3.Text
import android.view.KeyEvent as AndroidKeyEvent

@Composable
fun VideoDetailTvScreen(
    navigator: NavController,
    key: String,
    id: String,
    viewModel: VideoDetailViewModel = hiltViewModel()
) {
    VideoDetailRouteContent(
        navigator = navigator,
        key = key,
        id = id,
        viewModel = viewModel
    ) { state, detail, sources, selectedSource, playableMedia, openingFullscreen, onOpenFullscreen, onProgressChanged, onToggleFavorite, onPlayCurrent, onRetryCurrent, onClearError, onSelectSource, onSelectEpisode ->
        VideoDetailTvContent(
            navigator = navigator,
            state = state,
            detail = detail,
            sources = sources,
            selectedSource = selectedSource,
            playableMedia = playableMedia,
            openingFullscreen = openingFullscreen,
            onOpenFullscreen = onOpenFullscreen,
            onProgressChanged = onProgressChanged,
            onToggleFavorite = onToggleFavorite,
            onPlayCurrent = onPlayCurrent,
            onRetryCurrent = onRetryCurrent,
            onClearError = onClearError,
            onSelectSource = onSelectSource,
            onSelectEpisode = onSelectEpisode
        )
    }
}

@Composable
internal fun VideoDetailTvContent(
    navigator: NavController,
    state: VideoDetailUiState,
    detail: com.loong.longtv.engine.source.model.VodInfo,
    sources: List<PlayLine>,
    selectedSource: PlayLine?,
    playableMedia: PlayableMedia?,
    openingFullscreen: Boolean,
    onOpenFullscreen: (PlayableMedia, String) -> Unit,
    onProgressChanged: (Long, Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayCurrent: () -> Unit,
    onRetryCurrent: () -> Unit,
    onClearError: () -> Unit,
    onSelectSource: (Int) -> Unit,
    onSelectEpisode: (PlayLine, Int) -> Unit
) {
    val episodeName = selectedSource?.episodes?.getOrNull(state.selectedEpisodeIndex)?.name.orEmpty()
    val canOpenFullscreen = playableMedia != null &&
        !playableMedia.needsParse() &&
        playableMedia.url.isNotBlank()
    var previewFocused by remember(playableMedia?.url, state.selectedEpisodeIndex) { mutableStateOf(false) }
    val previewFocusRequester = remember(playableMedia?.url, state.selectedEpisodeIndex) { FocusRequester() }

    LaunchedEffect(canOpenFullscreen, playableMedia?.url, state.selectedEpisodeIndex) {
        if (canOpenFullscreen) {
            previewFocusRequester.requestFocus()
        }
    }

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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            val gap = 20.dp
            val previewWidth = (maxWidth - gap) / 2
            val infoWidth = maxWidth - previewWidth - gap

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    Box(
                        modifier = Modifier
                            .width(previewWidth)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(26.dp))
                            .border(
                                BorderStroke(
                                    if (previewFocused) 2.dp else 1.dp,
                                    if (previewFocused) AppTextSecondary.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f)
                                ),
                                RoundedCornerShape(26.dp)
                            )
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black,
                                        Color.Black.copy(alpha = 0.88f)
                                    )
                                )
                            )
                            .onFocusChanged { previewFocused = it.isFocused }
                            .focusRequester(previewFocusRequester)
                            .focusable(enabled = canOpenFullscreen)
                            .onPreviewKeyEvent { event ->
                                if (!canOpenFullscreen) return@onPreviewKeyEvent false
                                if (event.nativeKeyEvent.action != AndroidKeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                                when (event.nativeKeyEvent.keyCode) {
                                    AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                                    AndroidKeyEvent.KEYCODE_ENTER,
                                    AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                        onOpenFullscreen(playableMedia!!, episodeName)
                                        true
                                    }

                                    else -> false
                                }
                            }
                    ) {
                        DetailPreviewBlock(
                            modifier = Modifier.fillMaxSize(),
                            detail = detail,
                            playableMedia = playableMedia,
                            resumePositionMs = state.resumePositionMs,
                            selectedSource = selectedSource,
                            selectedEpisodeIndex = state.selectedEpisodeIndex,
                            sharedSessionReleaseDelayMs = if (openingFullscreen) 5_000L else 0L,
                            onBack = { navigator.popBackStack() },
                            onOpenFullscreen = onOpenFullscreen,
                            onProgressChanged = onProgressChanged
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(infoWidth)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(26.dp))
                            .border(
                                BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                RoundedCornerShape(26.dp)
                            )
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(22.dp)
                    ) {
                        DetailInfoBlock(
                            modifier = Modifier.fillMaxSize(),
                            navigator = navigator,
                            state = state,
                            detail = detail,
                            selectedSource = selectedSource,
                            playableMedia = playableMedia,
                            resumePositionMs = state.resumePositionMs,
                            phoneLayout = false,
                            onToggleFavorite = onToggleFavorite,
                            onPlayCurrent = onPlayCurrent,
                            onRetryCurrent = onRetryCurrent,
                            onClearError = onClearError
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    TvSectionBlock(
                        title = "播放线路",
                        subtitle = "切换不同来源尝试更稳定的播放效果"
                    ) {
                        SourceRow(
                            sources = sources,
                            selectedIndex = state.selectedSourceIndex,
                            onSelect = onSelectSource,
                            phoneLayout = false
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    TvSectionBlock(
                        title = "选集",
                        subtitle = "快速定位到想看的集数"
                    ) {
                        EpisodeGrid(
                            compact = false,
                            phoneLayout = false,
                            playInfo = selectedSource,
                            selectedIndex = state.selectedEpisodeIndex,
                            onSelect = onSelectEpisode
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSectionBlock(
    title: String,
    subtitle: String,
    framed: Boolean = title != "播放线路",
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (framed) {
                    Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            RoundedCornerShape(24.dp)
                        )
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                } else {
                    Modifier.padding(horizontal = 2.dp)
                }
            )
    ) {
        TvInlineSectionHeader(
            title = title,
            subtitle = subtitle
        )
        Spacer(modifier = Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun TvInlineSectionHeader(
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = if (title == "播放线路") BrandPrimary else AppTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "  $subtitle",
            color = AppTextMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
