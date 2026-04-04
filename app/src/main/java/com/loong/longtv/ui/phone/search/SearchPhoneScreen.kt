package com.loong.longtv.ui.phone.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.loong.longtv.nav.Screen
import com.loong.longtv.ui.shared.search.SearchViewModel
import com.loong.longtv.ui.shared.search.PhoneSearchLayout
import com.loong.longtv.ui.shared.widgets.AppScreen

@Composable
fun SearchPhoneScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val historyList by viewModel.history.collectAsState()
    val groups by viewModel.searchState.collectAsState()
    var keyword by rememberSaveable { mutableStateOf("") }
    val hotKeywords = remember(groups) {
        groups.firstOrNull { it.title == "热门搜索" }?.tags.orEmpty()
    }

    val searchAction: (String) -> Unit = { text ->
        val trimmed = text.trim()
        if (trimmed.isNotBlank()) {
            navController.navigate(Screen.SearchResult.createRoute(trimmed))
        }
    }

    AppScreen {
        PhoneSearchLayout(
            keyword = keyword,
            onKeywordChange = { keyword = it },
            historyList = historyList.map { it.keyword },
            hotKeywords = hotKeywords,
            onBack = { navController.popBackStack() },
            onSearch = searchAction,
            onClearHistory = viewModel::clear,
            onRemoveHistory = viewModel::removeKeyword
        )
    }
}
