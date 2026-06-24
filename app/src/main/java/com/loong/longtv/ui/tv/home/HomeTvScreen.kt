package com.loong.longtv.ui.tv.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.loong.longtv.ui.shared.home.HomeViewModel
import com.loong.longtv.ui.shared.home.HomeScreenContent
import com.loong.longtv.ui.shared.widgets.AppScreen

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeTvScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value

    AppScreen {
        HomeScreenContent(
            navController = navController,
            uiState = uiState,
            isTv = true,
            onRetry = viewModel::refresh
        )
    }
}
