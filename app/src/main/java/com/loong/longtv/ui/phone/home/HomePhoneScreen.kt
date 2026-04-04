package com.loong.longtv.ui.phone.home

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.loong.longtv.ui.shared.home.HomeViewModel
import com.loong.longtv.ui.shared.home.HomeCompactHeader
import com.loong.longtv.ui.shared.home.HomeScreenContent
import com.loong.longtv.ui.shared.home.QuickActions
import com.loong.longtv.ui.shared.widgets.AppScreen

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomePhoneScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value

    AppScreen {
        HomeCompactHeader(
            siteName = uiState.currentSite?.name,
            subtitle = "首页聚焦片单浏览、搜索与播放。"
        )
        QuickActions(
            navController = navController,
            compact = true,
            modifier = Modifier.padding(top = 14.dp)
        )
        HomeScreenContent(
            navController = navController,
            uiState = uiState,
            compact = true,
            phoneLayout = true,
            onRetry = viewModel::refresh,
            onTabSelected = viewModel::selectTab
        )
    }
}
