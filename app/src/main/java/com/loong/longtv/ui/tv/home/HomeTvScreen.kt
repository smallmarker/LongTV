package com.loong.longtv.ui.tv.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.loong.longtv.ui.shared.home.HomeViewModel
import com.loong.longtv.ui.shared.home.HomeHeroHeader
import com.loong.longtv.ui.shared.home.HomeScreenContent
import com.loong.longtv.ui.shared.home.QuickActions
import com.loong.longtv.ui.shared.widgets.AppScreen

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeTvScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value

    AppScreen {
        HomeHeroHeader(
            siteName = uiState.currentSite?.name,
            subtitle = "直接浏览影视分类和内容片单，减少额外信息干扰。",
            trailing = { QuickActions(navController = navController, compact = false) }
        )
        HomeScreenContent(
            navController = navController,
            uiState = uiState,
            compact = false,
            phoneLayout = false,
            onRetry = viewModel::refresh,
            onTabFocused = viewModel::focusTab,
            onTabSelected = viewModel::selectTab
        )
    }
}
