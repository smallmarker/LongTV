package com.loong.longtv.ui.tv.search

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.loong.longtv.nav.Screen
import com.loong.longtv.ui.shared.search.SearchViewModel
import com.loong.longtv.ui.shared.search.TvSearchWorkspace
import com.loong.longtv.ui.shared.widgets.AppScreen

@Composable
fun SearchTvScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val historyList by viewModel.history.collectAsState()
    val groups by viewModel.searchState.collectAsState()
    val tvSuggestions by viewModel.tvSuggestions.collectAsState()
    val tvSuggestionsLoading by viewModel.tvSuggestionsLoading.collectAsState()
    var keyword by rememberSaveable { mutableStateOf("") }
    val hotKeywords = remember(groups) {
        groups.firstOrNull { it.title == "热门搜索" }?.tags.orEmpty()
    }
    LaunchedEffect(keyword) {
        viewModel.loadTvSuggestions(keyword)
    }
    AppScreen {
        TvSearchWorkspace(
            modifier = Modifier.padding(top = 4.dp),
            keyword = keyword,
            onKeywordChange = { keyword = it },
            historyList = historyList.map { it.keyword },
            hotKeywords = hotKeywords,
            suggestionList = tvSuggestions,
            suggestionLoading = tvSuggestionsLoading,
            onSearch = { text ->
                val trimmed = text.trim()
                if (trimmed.isNotBlank()) {
                    navController.navigate(Screen.SearchResult.createRoute(trimmed))
                }
            },
            onClearHistory = viewModel::clear,
            onRemoveHistory = viewModel::removeKeyword
        )
    }
}
