package com.loong.longtv.ui.tv.live

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.loong.longtv.ui.shared.live.LiveScreenContent
import com.loong.longtv.ui.shared.live.LiveViewModel

@Composable
fun LiveTvScreen(
    navController: NavController,
    viewModel: LiveViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsState().value

    LiveScreenContent(
        navController = navController,
        uiState = uiState,
        isTv = true,
        onRetry = viewModel::refresh
    )
}
