package com.loong.longtv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.loong.longtv.Screen
import com.loong.longtv.data.bean.CategoryContent
import com.loong.longtv.data.bean.HomeTabItem
import com.loong.longtv.data.bean.Site
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.home.repository.ApiResult
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.settings.spiderFamily
import com.loong.longtv.ui.widgets.AdaptiveSiteSelector
import com.loong.longtv.ui.widgets.AppHero
import com.loong.longtv.ui.widgets.AppPageHeader
import com.loong.longtv.ui.widgets.AppScreen
import com.loong.longtv.ui.widgets.AppButton
import com.loong.longtv.ui.widgets.SectionHeader
import com.loong.longtv.ui.widgets.TvSurface

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState = viewModel.uiState.collectAsState().value
    val deviceClass = currentDeviceClass()
    val compact = deviceClass != DeviceClass.TV
    val phoneLayout = deviceClass == DeviceClass.PHONE

    AppScreen {
        if (phoneLayout) {
            AppPageHeader(
                title = uiState.currentSite?.name ?: "LongTV",
                subtitle = buildString {
                    append("首页聚焦片单浏览和站点切换。")
                    uiState.currentSite?.let {
                        append(" 当前来源：").append(it.spiderFamily().label)
                    }
                }
            )
            QuickActions(
                navController = navController,
                compact = true,
                modifier = Modifier.padding(top = 14.dp)
            )
        } else {
            AppHero(
                title = uiState.currentSite?.name ?: "探索影视内容",
                subtitle = buildString {
                    append("首页、搜索、详情和播放管理统一成更适合 TV 与手机的内容流体验。")
                    uiState.currentSite?.let {
                        append(" 当前来源为 ").append(it.spiderFamily().label)
                    }
                },
                trailing = { QuickActions(navController = navController, compact = false) }
            )
        }

        SiteSelectorRow(
            sites = uiState.sites,
            currentSite = uiState.currentSite,
            onSiteClick = viewModel::selectSite,
            phoneLayout = phoneLayout
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
                phoneLayout = phoneLayout,
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
    QuickActions(navController = navController, compact = false)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalTvMaterial3Api::class)
@Composable
private fun QuickActions(
    navController: NavController,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickAction(
            icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "搜索") },
            label = "搜索",
            compact = compact,
            onClick = { navController.navigate(Screen.Search.route) }
        )
        QuickAction(
            icon = {},
            label = "历史",
            compact = compact,
            onClick = { navController.navigate(Screen.History.route) }
        )
        QuickAction(
            icon = {},
            label = "收藏",
            compact = compact,
            onClick = { navController.navigate(Screen.Favorite.route) }
        )
        QuickAction(
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "设置") },
            label = "设置",
            compact = compact,
            onClick = { navController.navigate(Screen.Settings.route) }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun QuickAction(
    icon: @Composable () -> Unit,
    label: String,
    compact: Boolean,
    onClick: () -> Unit
) {
    TvSurface(
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 12.dp else 14.dp,
                vertical = if (compact) 8.dp else 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
        ) {
            icon()
            Text(
                text = label,
                fontSize = if (compact) 12.sp else 13.sp,
                color = AppTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalTvMaterial3Api::class)
@Composable
private fun SiteSelectorRow(
    sites: List<Site>,
    currentSite: Site?,
    onSiteClick: (String) -> Unit,
    phoneLayout: Boolean
) {
    if (sites.isEmpty()) return

    Column(modifier = Modifier.padding(top = 22.dp)) {
        SectionHeader(
            title = "内容来源",
            subtitle = if (phoneLayout) {
                "手机端使用横向滚动站点条，避免拥挤和换行错乱"
            } else {
                "切换站点，快速对比不同片源与分类"
            }
        )
        AdaptiveSiteSelector(
            sites = sites,
            currentSiteKey = currentSite?.key,
            onSelect = onSiteClick,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalTvMaterial3Api::class)
@Composable
private fun HomeContentSection(
    tabs: List<HomeTabItem>,
    selectedTabIndex: Int,
    currentSite: Site?,
    compact: Boolean,
    phoneLayout: Boolean,
    videoState: ApiResult<CategoryContent?>,
    onRetry: () -> Unit,
    onTabSelected: (Int) -> Unit,
    navController: NavController
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (currentSite != null) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                SectionHeader(
                    title = currentSite.name,
                    subtitle = if (phoneLayout) {
                        "当前站点与分类入口"
                    } else {
                        "按分类浏览、选择线路，并继续观看感兴趣的内容"
                    }
                )
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HomeChip(text = currentSite.spiderFamily().label)
                    if (!phoneLayout) {
                        HomeChip(text = "key: ${currentSite.key}")
                        HomeChip(text = "api: ${currentSite.api}")
                    }
                }
            }
        }

        if (tabs.isNotEmpty()) {
            HomeTabsRow(
                items = tabs.map { it.type_name.ifBlank { "未命名分类" } },
                selectedIndex = selectedTabIndex,
                onSelect = onTabSelected,
                modifier = Modifier.padding(bottom = 4.dp)
            )
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
                    currentSiteKey = currentSite?.key.orEmpty(),
                    compact = compact,
                    phoneLayout = phoneLayout,
                    navController = navController
                )
            }
        }
    }
}

@Composable
private fun HomeTabsRow(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex, items.size) {
        if (items.isNotEmpty()) {
            listState.animateScrollToItem(selectedIndex.coerceIn(0, items.lastIndex))
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(items) { index, item ->
            HomeTabChip(
                text = item,
                selected = index == selectedIndex,
                onClick = { onSelect(index) }
            )
        }
    }
}

@Composable
private fun HomeTabChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TvSurface(onClick = onClick) {
        Box(
            modifier = Modifier
                .scale(if (selected) 1.04f else 1f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .background(
                    if (selected) BrandPrimary.copy(alpha = 0.22f) else Color.Transparent
                )
                .border(
                    width = if (selected) 1.4.dp else 1.dp,
                    color = if (selected) BrandPrimary.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.08f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                )
                .padding(
                    horizontal = if (selected) 14.dp else 12.dp,
                    vertical = if (selected) 9.dp else 8.dp
                )
        ) {
            Text(
                text = text.ifBlank { "未命名分类" },
                color = if (selected) BrandPrimary else AppTextPrimary,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

@Composable
private fun VideoListScreen(
    categoryContent: CategoryContent?,
    selectedTab: HomeTabItem?,
    currentSiteKey: String,
    compact: Boolean,
    phoneLayout: Boolean,
    navController: NavController
) {
    val videos = categoryContent?.list.orEmpty()
    val gridState = rememberLazyGridState()

    LaunchedEffect(selectedTab?.type_id, videos.size) {
        if (videos.isNotEmpty()) {
            gridState.animateScrollToItem(0)
        }
    }

    if (videos.isEmpty()) {
        EmptyScreen("${selectedTab?.type_name ?: "当前分类"}暂无内容")
        return
    }

    LazyVerticalGrid(
        state = gridState,
        columns = when {
            phoneLayout -> GridCells.Fixed(2)
            compact -> GridCells.Adaptive(minSize = 170.dp)
            else -> GridCells.Adaptive(minSize = 200.dp)
        },
        modifier = Modifier
            .fillMaxHeight()
            .padding(top = 8.dp),
        contentPadding = PaddingValues(bottom = if (phoneLayout) 28.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(if (phoneLayout) 12.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(if (phoneLayout) 12.dp else 16.dp)
    ) {
        items(videos) { video ->
            TvSurface(
                onClick = {
                    val siteKey = video.safeSiteKey(currentSiteKey)
                    val videoId = video.safeVideoId()
                    if (siteKey.isNotBlank() && videoId.isNotBlank()) {
                        navController.navigate(Screen.VideoDetail.createRoute(siteKey, videoId))
                    }
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
        Text(text = message, color = AppTextPrimary, fontSize = 16.sp)
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
        Text(text = message, color = AppError, fontSize = 16.sp)
        AppButton(
            text = "重试",
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

private fun com.loong.longtv.data.bean.Video.safeSiteKey(fallback: String = ""): String {
    return runCatching { key }.getOrNull().orEmpty().ifBlank { fallback }
}

private fun com.loong.longtv.data.bean.Video.safeVideoId(): String {
    return runCatching { vod_id }.getOrNull().orEmpty()
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
        Text(text = message, color = AppTextMuted, fontSize = 16.sp)
    }
}
