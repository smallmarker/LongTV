package com.loong.longtv.ui.shared.live

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.navigation.NavController
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.loong.longtv.engine.source.model.LiveChannel
import com.loong.longtv.ui.shared.detail.PlayerResizeMode
import com.loong.longtv.ui.shared.detail.VideoPlayerView
import com.loong.longtv.ui.shared.widgets.AppButton
import com.loong.longtv.ui.shared.widgets.TvSurface
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.BrandAccent
import com.loong.longtv.ui.theme.BrandPrimary
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun LiveScreenContent(
    navController: NavController,
    uiState: LiveUiState,
    isTv: Boolean,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    var selectedIndex by remember { mutableIntStateOf(0) }
    var channelPanelVisible by remember { mutableStateOf(true) }
    var playbackReloadKey by remember { mutableIntStateOf(0) }
    var playerBuffering by remember { mutableStateOf(false) }
    var playerReady by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var invalidChannelUrls by remember { mutableStateOf<Set<String>>(emptySet()) }
    var liveWindowDriftCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    DisposableEffect(activity, isTv) {
        if (activity == null || isTv) {
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
                WindowInsetsControllerCompat(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    LaunchedEffect(uiState.channels) {
        selectedIndex = uiState.channels.defaultLiveChannelIndex()
        invalidChannelUrls = emptySet()
        liveWindowDriftCounts = emptyMap()
    }

    BackHandler {
        if (channelPanelVisible && uiState.channels.isNotEmpty()) {
            channelPanelVisible = false
        } else {
            navController.popBackStack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val selectedChannel = uiState.channels.getOrNull(selectedIndex)

        LaunchedEffect(selectedChannel?.url, playbackReloadKey) {
            playerReady = false
            playerBuffering = selectedChannel != null
            playbackError = null
        }

        LaunchedEffect(selectedChannel?.url, playerBuffering, playerReady) {
            if (selectedChannel == null || !playerBuffering) return@LaunchedEffect
            delay(LIVE_BUFFER_STALL_FALLBACK_MS)
            if (!playerBuffering) return@LaunchedEffect

            val failedUrls = invalidChannelUrls + selectedChannel.url
            val fallbackIndex = uiState.channels.findFallbackLiveChannelIndex(
                currentIndex = selectedIndex,
                invalidUrls = failedUrls
            )
            if (fallbackIndex != null) {
                invalidChannelUrls = failedUrls
                selectedIndex = fallbackIndex
                playbackReloadKey++
                playerReady = false
                playerBuffering = true
                playbackError = null
            }
        }

        if (selectedChannel != null) {
            key(selectedChannel.url, playbackReloadKey) {
                VideoPlayerView(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { channelPanelVisible = !channelPanelVisible },
                    videoUrl = selectedChannel.url,
                    requestHeaders = emptyMap(),
                    repeat = false,
                    autoPlay = true,
                    showController = false,
                    enableNativeController = false,
                    cacheEnabled = false,
                    liveStream = true,
                    resizeMode = PlayerResizeMode.Fit,
                    onPlaybackStateChanged = { state ->
                        playerBuffering = state == Player.STATE_BUFFERING
                        if (state == Player.STATE_READY) {
                            playerReady = true
                            playbackError = null
                        }
                    },
                    onPlayingChanged = { playing ->
                        if (playing) {
                            playerReady = true
                            playerBuffering = false
                            playbackError = null
                        }
                    },
                    onPlaybackError = { message ->
                        if (message.isBehindLiveWindowError()) {
                            val driftCount = (liveWindowDriftCounts[selectedChannel.url] ?: 0) + 1
                            liveWindowDriftCounts = liveWindowDriftCounts + (selectedChannel.url to driftCount)
                            val shouldFallback = driftCount >= 2
                            val failedUrls = if (shouldFallback) invalidChannelUrls + selectedChannel.url else invalidChannelUrls
                            val fallbackIndex = if (shouldFallback) {
                                uiState.channels.findFallbackLiveChannelIndex(
                                    currentIndex = selectedIndex,
                                    invalidUrls = failedUrls
                                )
                            } else {
                                null
                            }
                            if (fallbackIndex != null) {
                                invalidChannelUrls = failedUrls
                                selectedIndex = fallbackIndex
                                playbackReloadKey++
                                playerReady = false
                                playerBuffering = true
                                playbackError = null
                            } else {
                                playbackReloadKey++
                                playerReady = false
                                playerBuffering = true
                                playbackError = null
                            }
                            return@VideoPlayerView
                        }
                        val failedUrls = invalidChannelUrls + selectedChannel.url
                        invalidChannelUrls = failedUrls
                        val fallbackIndex = uiState.channels.findFallbackLiveChannelIndex(
                            currentIndex = selectedIndex,
                            invalidUrls = failedUrls
                        )
                        if (fallbackIndex != null) {
                            selectedIndex = fallbackIndex
                            playbackReloadKey++
                            playerReady = false
                            playerBuffering = true
                            playbackError = null
                        } else {
                            playerBuffering = false
                            playbackError = message.toLivePlaybackErrorText()
                        }
                    },
                    onSurfaceTap = { channelPanelVisible = !channelPanelVisible }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = if (channelPanelVisible) 0.60f else 0.18f),
                        0.34f to Color.Black.copy(alpha = if (channelPanelVisible) 0.24f else 0.06f),
                        1f to Color.Transparent
                    )
                )
        )

        if (
            selectedChannel != null &&
            uiState.loading.not() &&
            uiState.errorMessage == null &&
            uiState.channels.isNotEmpty() &&
            (playerBuffering || playerReady.not() || playbackError != null)
        ) {
            LivePlaybackLoadingOverlay(
                channel = selectedChannel,
                errorMessage = playbackError
            )
        }

        when {
            uiState.loading -> LiveCenteredState(
                title = "正在载入直播源",
                message = "LongTV 正在准备频道列表...",
                action = null
            )
            uiState.errorMessage != null -> LiveCenteredState(
                title = "直播源加载失败",
                message = uiState.errorMessage,
                action = { AppButton(text = "重试", onClick = onRetry) }
            )
            uiState.sources.isEmpty() -> LiveCenteredState(
                title = "暂无直播源",
                message = "当前配置还没有 lives 直播源。",
                action = null
            )
            uiState.channels.isEmpty() -> LiveCenteredState(
                title = "暂无可播放频道",
                message = "直播源暂时没有返回可播放频道，请稍后重试。",
                action = { AppButton(text = "重试", onClick = onRetry) }
            )
            channelPanelVisible -> LiveChannelOverlay(
                channels = uiState.channels,
                selectedIndex = selectedIndex,
                isTv = isTv,
                onChannelSelected = { index ->
                    uiState.channels.getOrNull(index)?.url?.let { url ->
                        invalidChannelUrls = invalidChannelUrls - url
                        liveWindowDriftCounts = liveWindowDriftCounts - url
                    }
                    selectedIndex = index
                    playbackReloadKey++
                }
            )
        }
    }
}

@Composable
private fun LiveCenteredState(
    title: String,
    message: String?,
    action: (@Composable () -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = AppTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                color = if (title.contains("失败")) AppError else AppTextMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (action != null) {
            Spacer(modifier = Modifier.height(14.dp))
            action()
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LivePlaybackLoadingOverlay(
    channel: LiveChannel,
    errorMessage: String?,
) {
    val cardShape = RoundedCornerShape(10.dp)
    val spinnerRotation by rememberInfiniteTransition(label = "live-loading")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing)
            ),
            label = "live-loading-rotation"
        )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(cardShape)
                .background(Color.Black.copy(alpha = 0.58f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), cardShape)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .size(30.dp)
            ) {
                rotate(spinnerRotation) {
                    drawArc(
                        color = BrandPrimary.copy(alpha = 0.92f),
                        startAngle = -90f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                drawArc(
                    color = Color.White.copy(alpha = 0.14f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(
                text = if (errorMessage == null) "正在缓冲" else "播放异常",
                color = AppTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = errorMessage ?: channel.name,
                color = if (errorMessage == null) AppTextSecondary else AppError,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveChannelOverlay(
    channels: List<LiveChannel>,
    selectedIndex: Int,
    isTv: Boolean,
    onChannelSelected: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val initialSelectedIndex = remember(channels.size) { selectedIndex }
    val selectedFocusRequester = remember { FocusRequester() }

    LaunchedEffect(initialSelectedIndex, channels.size) {
        if (channels.isNotEmpty()) {
            listState.scrollToItem((initialSelectedIndex - 2).coerceIn(0, channels.lastIndex))
            if (isTv) {
                delay(80)
                selectedFocusRequester.requestFocus()
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (isTv) 310.dp else 258.dp)
            .background(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.62f),
                    0.74f to Color.Black.copy(alpha = 0.30f),
                    1f to Color.Transparent
                )
            )
            .padding(
                start = if (isTv) 20.dp else 12.dp,
                top = if (isTv) 18.dp else 14.dp,
                end = if (isTv) 30.dp else 20.dp,
                bottom = if (isTv) 18.dp else 14.dp
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "LongTV 直播",
                color = AppTextPrimary,
                fontSize = if (isTv) 18.sp else 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = "${channels.size} 个频道",
                color = AppTextSecondary,
                fontSize = 9.sp,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(if (isTv) 10.dp else 8.dp))

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    horizontal = if (isTv) 5.dp else 2.dp,
                    vertical = if (isTv) 5.dp else 2.dp
                ),
                verticalArrangement = Arrangement.spacedBy(if (isTv) 4.dp else 3.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(channels, key = { _, channel -> "${channel.name}-${channel.url}" }) { index, channel ->
                    LiveChannelMenuItem(
                        channel = channel,
                        selected = index == selectedIndex,
                        index = index,
                        isTv = isTv,
                        modifier = if (isTv && index == initialSelectedIndex) {
                            Modifier.focusRequester(selectedFocusRequester)
                        } else {
                            Modifier
                        },
                        onClick = { onChannelSelected(index) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveChannelMenuItem(
    channel: LiveChannel,
    selected: Boolean,
    index: Int,
    isTv: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(6.dp)
    TvSurface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp, vertical = 1.dp),
        scale = ClickableSurfaceDefaults.scale(focusedScale = if (isTv) 1.015f else 1.04f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (selected) BrandPrimary.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.12f))
                .border(
                    width = 1.dp,
                    color = if (selected) BrandPrimary.copy(alpha = 0.66f) else Color.White.copy(alpha = 0.14f),
                    shape = shape
                )
                .padding(horizontal = if (isTv) 8.dp else 7.dp, vertical = if (isTv) 6.dp else 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = (index + 1).toString().padStart(2, '0'),
                color = if (selected) BrandPrimary else AppTextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(if (isTv) 20.dp else 18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    color = AppTextPrimary,
                    fontSize = if (isTv) 12.sp else 11.sp,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = channel.group.ifBlank { channel.sourceName.ifBlank { "直播频道" } },
                    color = AppTextSecondary,
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 0.dp)
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(BrandAccent)
                )
            }
        }
    }
}

private fun List<LiveChannel>.defaultLiveChannelIndex(): Int {
    if (isEmpty()) return 0
    return indexOfFirst { channel ->
        val name = channel.name.uppercase()
        name == "CCTV1" || name == "CCTV-1" || name.contains("CCTV1") || name.contains("CCTV-1")
    }.takeIf { it >= 0 } ?: 0
}

private fun List<LiveChannel>.findFallbackLiveChannelIndex(
    currentIndex: Int,
    invalidUrls: Set<String>,
): Int? {
    val current = getOrNull(currentIndex) ?: return null
    val identity = current.liveIdentity()
    val forwardRange = (currentIndex + 1)..lastIndex
    val backwardRange = 0 until currentIndex
    return (forwardRange + backwardRange).firstOrNull { index ->
        val channel = get(index)
        channel.url !in invalidUrls && channel.liveIdentity() == identity
    }
}

private fun LiveChannel.liveIdentity(): String {
    val cctvNumber = Regex("""CCTV[-\s]?(\d+)""", RegexOption.IGNORE_CASE)
        .find(name)
        ?.groupValues
        ?.getOrNull(1)
    if (!cctvNumber.isNullOrBlank()) return "CCTV$cctvNumber"
    return name.filter { it.isLetterOrDigit() }.uppercase().ifBlank { name.uppercase() }
}

private fun String.toLivePlaybackErrorText(): String {
    return when {
        contains("404", ignoreCase = true) -> "当前直播线路已失效（404），请选择其他频道或稍后重试。"
        contains("403", ignoreCase = true) -> "当前直播线路拒绝访问（403），请选择其他频道或稍后重试。"
        isBehindLiveWindowError() -> "直播延迟过大，正在重新同步到最新直播位置。"
        contains("timeout", ignoreCase = true) -> "当前直播线路响应超时，请稍后重试或切换其他频道。"
        contains("Source error", ignoreCase = true) -> "当前直播线路暂时不可用，请稍后重试或切换其他频道。"
        else -> this.ifBlank { "直播播放失败，请稍后重试或切换其他频道。" }
    }
}

private fun String.isBehindLiveWindowError(): Boolean {
    return contains("BehindLiveWindow", ignoreCase = true) ||
        contains("behind live window", ignoreCase = true)
}

private const val LIVE_BUFFER_STALL_FALLBACK_MS = 10_000L

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
