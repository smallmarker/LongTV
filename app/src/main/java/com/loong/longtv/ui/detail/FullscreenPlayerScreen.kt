package com.loong.longtv.ui.detail

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.tv.material3.Text
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.theme.AppCardOverlay
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.widgets.AppButton
import kotlinx.coroutines.delay

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
    val deviceClass = currentDeviceClass()
    val compactLayout = deviceClass != DeviceClass.TV
    val phoneLayout = deviceClass == DeviceClass.PHONE
    var currentPositionMs by remember(initialPositionMs) { mutableLongStateOf(initialPositionMs) }
    var overlayVisible by remember(title, videoUrl) { mutableStateOf(true) }
    var playbackError by remember(title, videoUrl) { mutableStateOf<String?>(null) }
    val episodeLabel = if (episodeCount > 0) "第 ${episodeIndex + 1} / $episodeCount 集" else "单集播放"

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

    LaunchedEffect(overlayVisible, title, videoUrl) {
        if (overlayVisible) {
            delay(3_000)
            overlayVisible = false
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
            showController = true,
            onProgressChanged = { position, _ ->
                currentPositionMs = position
            },
            onControllerVisibilityChanged = { visible ->
                if (visible) overlayVisible = true
            },
            onPlaybackError = { message ->
                playbackError = message
                overlayVisible = true
            }
        )

        if (overlayVisible) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xD907101C),
                                Color(0x9807101C),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(
                        horizontal = if (compactLayout) 14.dp else 20.dp,
                        vertical = if (compactLayout) 12.dp else 18.dp
                    )
                    .align(Alignment.TopStart)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppButton(text = "返回", onClick = ::exitFullscreen)
                    if (episodeCount > 1) {
                        AppButton(
                            text = "上一集",
                            onClick = { switchEpisode(episodeIndex - 1) },
                            enabled = episodeIndex > 0
                        )
                        AppButton(
                            text = "下一集",
                            onClick = { switchEpisode(episodeIndex + 1) },
                            enabled = episodeIndex < episodeCount - 1
                        )
                    }
                }

                Row(
                    modifier = Modifier.padding(top = if (compactLayout) 10.dp else 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title.ifBlank { "全屏播放" },
                            color = AppTextPrimary,
                            fontSize = if (compactLayout) 18.sp else 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (phoneLayout) {
                                "点按播放器显示控制器"
                            } else {
                                "点按播放器唤出控制器，上滑可暂时显示系统栏"
                            },
                            color = AppTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    if (!phoneLayout) {
                        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = episodeLabel,
                                color = AppTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = formatPlayerTime(currentPositionMs),
                                color = BrandPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                if (phoneLayout) {
                    FlowRow(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InfoBadge(text = episodeLabel)
                        InfoBadge(text = formatPlayerTime(currentPositionMs), highlight = true)
                    }
                }
            }
        }

        if (playbackError != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppCardOverlay)
                    .padding(
                        horizontal = if (compactLayout) 18.dp else 24.dp,
                        vertical = if (compactLayout) 18.dp else 22.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = playbackError ?: "播放失败",
                    color = AppError,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "当前地址可能不是直链，或站点还需要进一步解析。",
                    color = AppTextMuted,
                    fontSize = 12.sp
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
                    AppButton(
                        text = "返回详情",
                        onClick = {
                            playbackError = null
                            exitFullscreen()
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .fillMaxHeight(0.16f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xA507101C)
                        )
                    )
                )
        )
    }
}

private fun formatPlayerTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoBadge(
    text: String,
    highlight: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (highlight) BrandPrimary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (highlight) BrandPrimary else AppTextPrimary,
            fontSize = 11.sp
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
