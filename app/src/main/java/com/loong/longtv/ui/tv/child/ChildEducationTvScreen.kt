package com.loong.longtv.ui.tv.child

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.loong.longtv.ui.shared.child.ChildEducationViewModel
import com.loong.longtv.ui.shared.popular.PopularScreenContent
import com.loong.longtv.ui.shared.widgets.AppScreen

@Composable
fun ChildEducationTvScreen(
    navController: NavController,
    viewModel: ChildEducationViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value

    AppScreen {
        PopularScreenContent(
            navController = navController,
            uiState = uiState,
            isTv = true,
            loadingText = "正在加载少儿专区...",
            onRetry = viewModel::refresh,
            onTabFocused = viewModel::focusTab,
            onTabSelected = viewModel::selectTab,
            onFilterSelected = viewModel::selectFilter
        )
    }
}
