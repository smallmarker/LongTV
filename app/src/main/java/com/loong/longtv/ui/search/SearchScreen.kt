package com.loong.longtv.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.loong.longtv.Screen
import com.loong.longtv.data.bean.TagGroup
import com.loong.longtv.ui.common.isCompactScreen
import com.loong.longtv.ui.theme.Purple40
import com.loong.longtv.ui.widgets.TvSurface

/**
 * @author zl
 * @Date 2025/10/20
 **/
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController, viewModel: SearchViewModel = hiltViewModel()) {
    val compact = isCompactScreen()
    if (compact) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SearchKeyboard(
                modifier = Modifier.fillMaxWidth(),
                onSearch = { keyword ->
                    if (keyword.isNotBlank()) {
                        navController.navigate(Screen.SearchResult.createRoute(keyword))
                    }
                },
                onInputChange = {}
            )

            SearchHistoryScreen(
                modifier = Modifier.fillMaxWidth(),
                viewModel = viewModel,
                onHistoryClick = { keyword ->
                    navController.navigate(Screen.SearchResult.createRoute(keyword))
                }
            )

            TagListScreen(
                modifier = Modifier.fillMaxWidth(),
                viewModel = viewModel,
                compact = true
            ) {
                navController.navigate(Screen.SearchResult.createRoute(it))
            }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            SearchHistoryScreen(
                modifier = Modifier.width(200.dp),
                viewModel = viewModel,
                onHistoryClick = { keyword ->
                    navController.navigate(Screen.SearchResult.createRoute(keyword))
                }
            )

            SearchKeyboard(
                modifier = Modifier.width(200.dp),
                onSearch = { keyword ->
                    if (keyword.isNotBlank()) {
                        navController.navigate(Screen.SearchResult.createRoute(keyword))
                    }
                },
                onInputChange = {}
            )

            TagListScreen(
                modifier = Modifier.weight(1f),
                viewModel = viewModel,
                compact = false
            ) {
                navController.navigate(Screen.SearchResult.createRoute(it))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchHistoryScreen(
    modifier: Modifier,
    viewModel: SearchViewModel,
    onHistoryClick: (String) -> Unit
) {

    val historyList = viewModel.history.collectAsState().value

    Column(modifier = modifier) {

        Text(
            modifier = Modifier
                .background(Purple40, RoundedCornerShape(4.dp))
                .padding(4.dp),
            text = "搜索历史",
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            historyList.forEach { history ->
                TvSurface(
                    onClick = {
                        onHistoryClick(history.keyword)
                    }
                ) {
                    Box(
                        modifier = Modifier.padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = history.keyword
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TagListScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel,
    compact: Boolean = false,
    onItemClick: (String) -> Unit
) {
    val groups = viewModel.searchState.collectAsState().value

    if (compact) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groups.forEach { group ->
                TagItem(group) { wd ->
                    onItemClick(wd)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(groups) { group ->
                TagItem(group) { wd ->
                    onItemClick(wd)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagItem(group: TagGroup, onClick: (String) -> Unit) {
    Column {
        Text(
            modifier = Modifier
                .background(Purple40, RoundedCornerShape(4.dp))
                .padding(4.dp),
            text = group.title,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            group.tags.forEach { tag ->
                TvSurface(
                    onClick = { onClick(tag) }
                ) {
                    Box(
                        modifier = Modifier.padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tag
                        )
                    }
                }
            }
        }
    }
}
