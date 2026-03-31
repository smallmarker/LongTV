package com.loong.longtv.ui.detail

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.compose.BackHandler
import androidx.navigation.NavController
import androidx.tv.material3.Text
import com.loong.longtv.ui.widgets.AppButton
import kotlinx.coroutines.delay

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
    var currentPositionMs by remember(initialPositionMs) { mutableLongStateOf(initialPositionMs) }
    var overlayVisible by remember(title, videoUrl) { mutableStateOf(true) }
    var playbackError by remember(title, videoUrl) { mutableStateOf<String?>(null) }

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

    BackHandler {
        exitFullscreen()
    }

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
                if (visible) {
                    overlayVisible = true
                }
            },
            onPlaybackError = { message ->
                playbackError = message
                overlayVisible = true
            }
        )

        if (overlayVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x66000000))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
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

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 112.dp, top = 16.dp, end = 16.dp)
            ) {
                Text(
                    text = title.ifBlank { "全屏播放" },
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    text = "上滑可临时显示系统栏，点播放器可显示控制器",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (playbackError != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xCC101010))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = playbackError ?: "播放失败",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
