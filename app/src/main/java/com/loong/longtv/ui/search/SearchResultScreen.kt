package com.loong.longtv.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.tv.material3.Text
import com.loong.longtv.Screen.VideoDetail
import com.loong.longtv.ui.home.VideoCard
import com.loong.longtv.ui.common.isCompactScreen
import com.loong.longtv.ui.widgets.AppButton
import com.loong.longtv.ui.widgets.TvSurface

/**
 * @author zl
 * @Date 2025/10/21
 **/

@Composable
fun SearchResultScreen(
    navigator: NavController,
    keyword: String,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val compact = isCompactScreen()
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

    Column {
        SearchHeader(
            keyword = keyword,
            selectedCategory = selectedCategory,
            resultCount = videoMap[selectedCategory].orEmpty().size
        )

        when {
            loading -> {
                SearchCenterMessage("正在搜索 $keyword ...", Color.White)
                return@Column
            }

            error != null && videoMap.isEmpty() -> {
                SearchCenterMessage(
                    message = error ?: "搜索失败",
                    color = Color.Red,
                    actionLabel = "重试",
                    onAction = { viewModel.searchContent(keyword) }
                )
                return@Column
            }

            videoMap.isEmpty() -> {
                SearchCenterMessage(
                    message = "暂无搜索结果",
                    color = Color.Gray,
                    actionLabel = "重试",
                    onAction = { viewModel.searchContent(keyword) }
                )
                return@Column
            }
        }

        if (compact) {
            CompactCategoryRow(
                categories = categories,
                selectedCategory = selectedCategory,
                onSelect = { selectedCategory = it }
            )
            SearchResultGrid(
                modifier = Modifier.fillMaxSize(),
                videos = videoMap[selectedCategory].orEmpty(),
                navigator = navigator
            )
        } else {
            Row {
                LazyColumn(
                    modifier = Modifier.width(200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        TvSurface(onClick = {}) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = category,
                                    color = if (selectedCategory == category) Color.White else Color.LightGray
                                )
                            }
                        }
                    }
                }

                SearchResultGrid(
                    modifier = Modifier.weight(1f),
                    videos = videoMap[selectedCategory].orEmpty(),
                    navigator = navigator
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactCategoryRow(
    categories: List<String>,
    selectedCategory: String?,
    onSelect: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            TvSurface(onClick = { onSelect(category) }) {
                Text(
                    text = category,
                    color = if (selectedCategory == category) Color.White else Color.LightGray,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchResultGrid(
    modifier: Modifier,
    videos: List<com.loong.longtv.data.bean.Video>,
    navigator: NavController
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Adaptive(150.dp)
    ) {
        items(videos) { video ->
            TvSurface(
                onClick = {
                    navigator.navigate(VideoDetail.createRoute(video.key, video.vod_id))
                }
            ) {
                VideoCard(video = video)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHeader(
    keyword: String,
    selectedCategory: String?,
    resultCount: Int
) {
    val family = selectedCategory?.substringAfter("[")?.substringBefore("]")?.takeIf { it.isNotBlank() }
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = "搜索: $keyword",
            color = Color.White,
            fontSize = 18.sp
        )
        if (selectedCategory != null) {
            Text(
                text = selectedCategory,
                color = Color.LightGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }
        if (family != null) {
            FlowRow(
                modifier = Modifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TvSurface(onClick = {}) {
                    Text(
                        text = "Spider: $family",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
                TvSurface(onClick = {}) {
                    Text(
                        text = "结果: $resultCount",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchCenterMessage(
    message: String,
    color: Color,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, color = color, fontSize = 16.sp)
            if (actionLabel != null && onAction != null) {
                AppButton(
                    text = actionLabel,
                    onClick = onAction,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
