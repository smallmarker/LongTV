package com.loong.longtv.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.widgets.AdaptiveChipTabs
import com.loong.longtv.ui.widgets.AppButton
import com.loong.longtv.ui.widgets.AppHero
import com.loong.longtv.ui.widgets.AppPageHeader
import com.loong.longtv.ui.widgets.AppScreen
import com.loong.longtv.ui.widgets.SectionHeader
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
    val deviceClass = currentDeviceClass()
    val compact = deviceClass != DeviceClass.TV
    val phoneLayout = deviceClass == DeviceClass.PHONE
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
        if (phoneLayout) {
            AppPageHeader(
                title = keyword,
                subtitle = "手机端结果页使用顶部筛选和双列海报流。"
            )
        } else {
            AppHero(
                title = "搜索结果",
                subtitle = "关键词 “$keyword” 的聚合结果会按站点分组展示，方便快速切换片源。"
            )
        }

        SearchHeader(
            keyword = keyword,
            selectedCategory = selectedCategory,
            resultCount = videoMap[selectedCategory].orEmpty().size,
            phoneLayout = phoneLayout
        )

        if (loading) {
            SearchCenterMessage("正在搜索 $keyword ...", Color.White)
        } else if (error != null && videoMap.isEmpty()) {
            SearchCenterMessage(
                message = error ?: "搜索失败",
                color = Color.Red,
                actionLabel = "重试",
                onAction = { viewModel.searchContent(keyword) }
            )
        } else if (videoMap.isEmpty()) {
            SearchCenterMessage(
                message = "暂无搜索结果",
                color = Color.Gray,
                actionLabel = "重试",
                onAction = { viewModel.searchContent(keyword) }
            )
        } else if (compact) {
            AdaptiveChipTabs(
                items = categories,
                selectedIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                onSelect = { index -> selectedCategory = categories.getOrNull(index) },
                modifier = Modifier.padding(bottom = 16.dp)
            )
            SearchResultGrid(
                modifier = Modifier.fillMaxSize(),
                videos = videoMap[selectedCategory].orEmpty(),
                navigator = navigator,
                phoneLayout = phoneLayout
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                LazyColumn(
                    modifier = Modifier.width(220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        TvSurface(onClick = { selectedCategory = category }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = category,
                                    color = if (selectedCategory == category) AppTextPrimary else AppTextSecondary
                                )
                            }
                        }
                    }
                }

                SearchResultGrid(
                    modifier = Modifier.weight(1f),
                    videos = videoMap[selectedCategory].orEmpty(),
                    navigator = navigator,
                    phoneLayout = false
                )
            }
        }
    }
}

@Composable
private fun SearchResultGrid(
    modifier: Modifier,
    videos: List<com.loong.longtv.data.bean.Video>,
    navigator: NavController,
    phoneLayout: Boolean
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = if (phoneLayout) GridCells.Fixed(2) else GridCells.Adaptive(150.dp),
        contentPadding = PaddingValues(bottom = if (phoneLayout) 28.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(if (phoneLayout) 12.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(if (phoneLayout) 12.dp else 16.dp)
    ) {
        items(videos) { video ->
            TvSurface(
                onClick = {
                    val siteKey = runCatching { video.key }.getOrNull().orEmpty()
                    val videoId = runCatching { video.vod_id }.getOrNull().orEmpty()
                    if (siteKey.isNotBlank() && videoId.isNotBlank()) {
                        navigator.navigate(VideoDetail.createRoute(siteKey, videoId))
                    }
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
    resultCount: Int,
    phoneLayout: Boolean
) {
    val family = selectedCategory?.substringAfter("[")?.substringBefore("]")?.takeIf { it.isNotBlank() }
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        SectionHeader(
            title = if (phoneLayout) "结果" else "搜索: $keyword",
            subtitle = if (phoneLayout) "共 ${resultCount} 条结果" else "当前结果 ${resultCount} 条"
        )
        if (selectedCategory != null) {
            Text(
                text = selectedCategory,
                color = AppTextSecondary,
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
                        color = AppTextPrimary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
                TvSurface(onClick = {}) {
                    Text(
                        text = "结果: $resultCount",
                        color = AppTextPrimary,
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
