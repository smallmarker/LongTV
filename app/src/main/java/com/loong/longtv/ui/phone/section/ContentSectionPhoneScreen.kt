package com.loong.longtv.ui.phone.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.loong.longtv.ui.shared.popular.PopularScreenContent
import com.loong.longtv.ui.shared.section.ContentSectionViewModel
import com.loong.longtv.ui.shared.widgets.AppScreen

@Composable
fun ContentSectionPhoneScreen(
    navController: NavController,
    viewModel: ContentSectionViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value

    AppScreen {
        PopularScreenContent(
            navController = navController,
            uiState = uiState,
            isTv = false,
            loadingText = viewModel.section.loadingText,
            onRetry = viewModel::refresh,
            onTabSelected = viewModel::selectTab,
            onFilterSelected = viewModel::selectFilter
        )
    }
}
