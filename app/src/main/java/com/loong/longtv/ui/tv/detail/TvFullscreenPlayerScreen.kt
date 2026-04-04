package com.loong.longtv.ui.tv.detail

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.loong.longtv.ui.shared.detail.FullscreenPlayerContent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TvFullscreenPlayerScreen(
    navController: NavController,
    title: String,
    videoUrl: String,
    requestHeaders: Map<String, String>,
    initialPositionMs: Long,
    episodeIndex: Int,
    episodeCount: Int,
) {
    FullscreenPlayerContent(
        navController = navController,
        title = title,
        videoUrl = videoUrl,
        requestHeaders = requestHeaders,
        initialPositionMs = initialPositionMs,
        episodeIndex = episodeIndex,
        episodeCount = episodeCount,
        isTv = true
    )
}
