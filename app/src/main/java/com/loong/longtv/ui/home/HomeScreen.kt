package com.loong.longtv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.loong.longtv.Screen
import com.loong.longtv.data.bean.CategoryContent
import com.loong.longtv.data.bean.HomeTabItem
import com.loong.longtv.data.bean.Site
import com.loong.longtv.ui.common.isCompactScreen
import com.loong.longtv.ui.home.repository.ApiResult
import com.loong.longtv.ui.settings.spiderFamily
import com.loong.longtv.ui.widgets.AppButton
import com.loong.longtv.ui.widgets.TvSurface

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState = viewModel.uiState.collectAsState().value
    val compact = isCompactScreen()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        QuickActions(navController)

        SiteSelectorRow(
            sites = uiState.sites,
            currentSite = uiState.currentSite,
            onSiteClick = viewModel::selectSite
        )

        when (val state = uiState.homeState) {
            is ApiResult.Loading -> LoadingScreen("正在加载站点首页...")
            is ApiResult.Error -> ErrorScreen(
                message = state.message ?: "站点首页加载失败",
                onRetry = viewModel::refresh
            )

            is ApiResult.Success -> HomeContentSection(
                tabs = uiState.tabs,
                selectedTabIndex = uiState.selectedTabIndex,
                currentSite = uiState.currentSite,
                compact = compact,
                videoState = uiState.videoState,
                onRetry = viewModel::refresh,
                onTabSelected = viewModel::selectTab,
                navController = navController
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalTvMaterial3Api::class)
@Composable
private fun QuickActions(navController: NavController) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickAction(
            icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "搜索") },
            label = "搜索",
            onClick = { navController.navigate(Screen.Search.route) }
        )
        QuickAction(
            icon = {},
            label = "历史",
            onClick = { navController.navigate(Screen.History.route) }
        )
        QuickAction(
            icon = {},
            label = "收藏",
            onClick = { navController.navigate(Screen.Favorite.route) }
        )
        QuickAction(
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "设置") },
            label = "设置",
            onClick = { navController.navigate(Screen.Settings.route) }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun QuickAction(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    TvSurface(
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Text(text = label, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalTvMaterial3Api::class)
@Composable
private fun SiteSelectorRow(
    sites: List<Site>,
    currentSite: Site?,
    onSiteClick: (String) -> Unit
) {
    if (sites.isEmpty()) return

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "当前站点",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        FlowRow(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sites.forEach { site ->
                val selected = site.key == currentSite?.key
                TvSurface(onClick = { onSiteClick(site.key) }) {
                    Text(
                        text = site.name,
                        modifier = Modifier
                            .background(if (selected) Color(0xFF1E4B7A) else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalTvMaterial3Api::class)
@Composable
private fun HomeContentSection(
    tabs: List<HomeTabItem>,
    selectedTabIndex: Int,
    currentSite: Site?,
    compact: Boolean,
    videoState: ApiResult<CategoryContent?>,
    onRetry: () -> Unit,
    onTabSelected: (Int) -> Unit,
    navController: NavController
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (currentSite != null) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text(
                    text = currentSite.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HomeChip(text = currentSite.spiderFamily().label)
                    HomeChip(text = "key: ${currentSite.key}")
                    HomeChip(text = "api: ${currentSite.api}")
                }
            }
        }

        if (tabs.isNotEmpty()) {
            if (compact) {
                CompactTabRow(
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = onTabSelected
                )
            } else {
                TabRow(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    selectedTabIndex = selectedTabIndex
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == selectedTabIndex,
                            onFocus = { onTabSelected(index) }
                        ) {
                            Text(
                                text = tab.type_name.ifBlank { "未命名分类" },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            when (videoState) {
                is ApiResult.Loading -> LoadingScreen("正在加载内容...")
                is ApiResult.Error -> ErrorScreen(
                    message = videoState.message ?: "内容加载失败",
                    onRetry = onRetry
                )

                is ApiResult.Success -> VideoListScreen(
                    categoryContent = videoState.data,
                    selectedTab = tabs.getOrNull(selectedTabIndex),
                    compact = compact,
                    navController = navController
                )
            }
        }
    }
}

@Composable
private fun HomeChip(text: String) {
    TvSurface(onClick = {}) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactTabRow(
    tabs: List<HomeTabItem>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedTabIndex
            TvSurface(onClick = { onTabSelected(index) }) {
                Text(
                    text = tab.type_name.ifBlank { "未命名分类" },
                    modifier = Modifier
                        .background(if (selected) Color(0xFF1E4B7A) else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun VideoListScreen(
    categoryContent: CategoryContent?,
    selectedTab: HomeTabItem?,
    compact: Boolean,
    navController: NavController
) {
    val videos = categoryContent?.list.orEmpty()

    if (videos.isEmpty()) {
        EmptyScreen("${selectedTab?.type_name ?: "当前分类"}暂无内容")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = if (compact) 150.dp else 200.dp),
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(videos) { video ->
            TvSurface(
                onClick = {
                    navController.navigate(Screen.VideoDetail.createRoute(video.key, video.vod_id))
                }
            ) {
                VideoCard(video = video)
            }
        }
    }
}

@Composable
private fun LoadingScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = Color.Red, fontSize = 16.sp)
        AppButton(
            text = "重试",
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun EmptyScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = Color.White, fontSize = 16.sp)
    }
}
