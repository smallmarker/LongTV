package com.loong.longtv.ui.tv.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.loong.longtv.ui.shared.search.SearchViewModel
import com.loong.longtv.ui.shared.search.SearchResultContent
import com.loong.longtv.ui.shared.widgets.AppHero
import com.loong.longtv.ui.shared.widgets.AppScreen

@Composable
fun SearchResultTvScreen(
    navigator: NavController,
    keyword: String,
    viewModel: SearchViewModel = hiltViewModel()
) {
    LaunchedEffect(keyword) {
        viewModel.searchContent(keyword)
    }
    val videoMap by viewModel.searchResult.collectAsStateWithLifecycle()
    val loading by viewModel.searchLoading.collectAsStateWithLifecycle()
    val error by viewModel.searchError.collectAsStateWithLifecycle()
    val categories = videoMap.keys.toList()
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }

    LaunchedEffect(videoMap) {
        if (selectedCategory !in categories) {
            selectedCategory = categories.firstOrNull()
        }
    }

    AppScreen {
        AppHero(
            title = "搜索结果",
            subtitle = "关键词 “$keyword” 的聚合结果会按站点分组展示，方便快速切换片源。"
        )
        SearchResultContent(
            navigator = navigator,
            keyword = keyword,
            categories = categories,
            selectedCategory = selectedCategory,
            resultCount = videoMap[selectedCategory].orEmpty().size,
            videos = videoMap[selectedCategory].orEmpty(),
            loading = loading,
            error = error,
            compact = false,
            phoneLayout = false,
            onRetry = { viewModel.searchContent(keyword) },
            onSelectCategory = { selectedCategory = it }
        )
    }
}
