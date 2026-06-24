package com.loong.longtv.ui.phone.popular

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.loong.longtv.ui.shared.popular.PopularScreenContent
import com.loong.longtv.ui.shared.popular.PopularViewModel
import com.loong.longtv.ui.shared.widgets.AppScreen

@Composable
fun PopularPhoneScreen(
    navController: NavController,
    viewModel: PopularViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value

    AppScreen {
        PopularScreenContent(
            navController = navController,
            uiState = uiState,
            isTv = false,
            onRetry = viewModel::refresh,
            onTabSelected = viewModel::selectTab,
            onFilterSelected = viewModel::selectFilter
        )
    }
}
