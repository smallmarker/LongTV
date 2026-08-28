package com.loong.longtv.ui.phone.detail

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.loong.longtv.engine.source.model.PlayLine
import com.loong.longtv.engine.player.PlayableMedia
import com.loong.longtv.ui.shared.detail.VideoDetailUiState
import com.loong.longtv.ui.shared.detail.VideoDetailViewModel
import com.loong.longtv.ui.shared.detail.DetailInfoBlock
import com.loong.longtv.ui.shared.detail.DetailPreviewBlock
import com.loong.longtv.ui.shared.detail.EpisodeGrid
import com.loong.longtv.ui.shared.detail.FullscreenPlayerContent
import com.loong.player.PlayerLoadingOverlay
import com.loong.longtv.ui.shared.detail.SourceRow
import com.loong.longtv.ui.shared.detail.VideoDetailRouteContent
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.shared.widgets.SectionHeader
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

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
    ) { state, detail, sources, selectedSource, playableMedia, _, _, onProgressChanged, onToggleFavorite, onPlayCurrent, onRetryCurrent, onClearError, onSelectSource, onSelectEpisode ->
        VideoDetailPhoneContent(
            navigator = navigator,
            state = state,
            detail = detail,
            sources = sources,
            selectedSource = selectedSource,
            playableMedia = playableMedia,
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
    onProgressChanged: (Long, Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayCurrent: () -> Unit,
    onRetryCurrent: () -> Unit,
    onClearError: () -> Unit,
    onSelectSource: (Int) -> Unit,
    onSelectEpisode: (PlayLine, Int) -> Unit
) {
    val sessionKey = "${playableMedia?.url.orEmpty()}#${state.selectedEpisodeIndex}"
    var currentPositionMs by rememberSaveable(sessionKey) { mutableLongStateOf(state.resumePositionMs) }
    var durationMs by rememberSaveable(sessionKey) { mutableLongStateOf(0L) }
    var fullscreenVisible by rememberSaveable { mutableStateOf(false) }
    var fullscreenExiting by rememberSaveable { mutableStateOf(false) }
    var fullscreenInitialPositionMs by rememberSaveable(sessionKey) { mutableLongStateOf(state.resumePositionMs) }

    fun saveLocalProgress(positionMs: Long, duration: Long) {
        currentPositionMs = positionMs
        durationMs = duration
        onProgressChanged(positionMs, duration)
    }

    fun episodeTitle(): String {
        return selectedSource?.episodes?.getOrNull(state.selectedEpisodeIndex)?.name.orEmpty()
    }

    fun fullscreenTitle(): String {
        val title = detail.name.ifBlank { "全屏播放" }
        val episodeName = episodeTitle()
        return if (episodeName.isBlank()) title else "$title - $episodeName"
    }

    fun playNextEpisodeFromCurrent() {
        val source = selectedSource ?: return
        val targetIndex = state.selectedEpisodeIndex + 1
        if (targetIndex !in source.episodes.indices) return
        onSelectEpisode(source, targetIndex)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                if (!fullscreenVisible) {
                    DetailPreviewBlock(
                        modifier = Modifier.fillMaxSize(),
                        detail = detail,
                        playableMedia = playableMedia,
                        playerLoading = state.playerLoading,
                        resumePositionMs = currentPositionMs,
                        selectedSource = selectedSource,
                        selectedEpisodeIndex = state.selectedEpisodeIndex,
                        sharedSessionReleaseDelayMs = 5_000L,
                        onBack = { navigator.popBackStack() },
                        onOpenFullscreen = { media, _, positionMs ->
                            currentPositionMs = positionMs
                            fullscreenInitialPositionMs = positionMs
                            fullscreenExiting = false
                            fullscreenVisible = media.url.isNotBlank() && !media.needsParse()
                        },
                        onProgressChanged = ::saveLocalProgress,
                        onPlaybackEnded = ::playNextEpisodeFromCurrent
                    )
                }
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
                    resumePositionMs = currentPositionMs,
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

        if (fullscreenVisible) {
            EmbeddedPhoneFullscreenOrientation(
                exiting = fullscreenExiting,
                onExitAnimationFinished = {
                    fullscreenVisible = false
                    fullscreenExiting = false
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                val readyMedia = playableMedia?.takeIf { it.url.isNotBlank() && !it.needsParse() }
                if (readyMedia != null) {
                    FullscreenPlayerContent(
                        navController = navigator,
                        title = fullscreenTitle(),
                        videoUrl = readyMedia.url,
                        requestHeaders = readyMedia.headers,
                        initialPositionMs = fullscreenInitialPositionMs,
                        episodeIndex = state.selectedEpisodeIndex,
                        episodeCount = selectedSource?.episodes?.size ?: 0,
                        isTv = false,
                        manageOrientation = false,
                        onExitFullscreen = { positionMs ->
                            saveLocalProgress(positionMs, durationMs)
                            fullscreenExiting = true
                        },
                        onSwitchEpisode = { targetIndex ->
                            val source = selectedSource
                            if (source != null && targetIndex in source.episodes.indices) {
                                fullscreenExiting = false
                                fullscreenVisible = true
                                onSelectEpisode(source, targetIndex)
                            }
                        },
                        onPlaybackEnded = ::playNextEpisodeFromCurrent,
                        onProgressChangedExternal = ::saveLocalProgress
                    )
                } else {
                    PlayerLoadingOverlay()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmbeddedPhoneFullscreenOrientation(
    exiting: Boolean,
    onExitAnimationFinished: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    DisposableEffect(activity) {
        if (activity == null) {
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
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    LaunchedEffect(exiting, activity) {
        if (!exiting) return@LaunchedEffect
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        delay(450)
        onExitAnimationFinished()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
