package com.loong.longtv.ui.phone.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
import com.loong.longtv.ui.shared.widgets.SectionHeader

@Composable
fun VideoDetailPhoneScreen(
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
        VideoDetailPhoneContent(
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
internal fun VideoDetailPhoneContent(
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
                .height(238.dp)
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
                sharedSessionReleaseDelayMs = if (openingFullscreen) 5_000L else 0L,
                onBack = { navigator.popBackStack() },
                onOpenFullscreen = onOpenFullscreen,
                onProgressChanged = onProgressChanged
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            DetailInfoBlock(
                navigator = navigator,
                state = state,
                detail = detail,
                selectedSource = selectedSource,
                playableMedia = playableMedia,
                resumePositionMs = state.resumePositionMs,
                phoneLayout = true,
                onToggleFavorite = onToggleFavorite,
                onPlayCurrent = onPlayCurrent,
                onRetryCurrent = onRetryCurrent,
                onClearError = onClearError
            )

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader(
                title = "播放线路"
            )
            Spacer(modifier = Modifier.height(12.dp))
            SourceRow(
                sources = sources,
                selectedIndex = state.selectedSourceIndex,
                onSelect = onSelectSource,
                phoneLayout = true
            )

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader(
                title = "选集"
            )
            Spacer(modifier = Modifier.height(12.dp))
            EpisodeGrid(
                compact = true,
                phoneLayout = true,
                playInfo = selectedSource,
                selectedIndex = state.selectedEpisodeIndex,
                onSelect = onSelectEpisode
            )
        }
    }
}
