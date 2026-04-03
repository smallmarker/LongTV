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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.loong.longtv.R
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
            HomeCompactHeader(
                siteName = uiState.currentSite?.name,
                subtitle = "首页聚焦片单浏览、搜索与播放。"
            )
            QuickActions(
                navController = navController,
                compact = true,
                modifier = Modifier.padding(top = 14.dp)
            )
        } else {
            HomeHeroHeader(
                siteName = uiState.currentSite?.name,
                subtitle = "直接浏览影视分类和内容片单，减少额外信息干扰。",
                trailing = { QuickActions(navController = navController, compact = false) }
            )
        }

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

@Composable
private fun HomeCompactHeader(
    siteName: String?,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.03f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LongTvLogo(compact = true)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "当前站点：${siteName?.ifBlank { "未选择" } ?: "未选择"}",
                color = AppTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = AppTextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun HomeHeroHeader(
    siteName: String?,
    subtitle: String,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.03f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            LongTvLogo(compact = false)
            Text(
                text = "当前站点：${siteName?.ifBlank { "未选择" } ?: "未选择"}",
                color = AppTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = subtitle,
                color = AppTextMuted,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        trailing()
    }
}

@Composable
private fun LongTvLogo(
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 40.dp else 54.dp)
                .clip(RoundedCornerShape(if (compact) 14.dp else 18.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .padding(if (compact) 2.dp else 3.dp)
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "LongTV Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(if (compact) 12.dp else 15.dp))
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "LongTV",
                color = AppTextPrimary,
                fontSize = if (compact) 18.sp else 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = if (compact) "你的影视盒子" else "更轻快地发现和播放内容",
                color = if (compact) AppTextMuted else Color.White.copy(alpha = 0.72f),
                fontSize = if (compact) 11.sp else 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
            icon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = Color.White
                )
            },
            label = "搜索",
            compact = compact,
            onClick = { navController.navigate(Screen.Search.route) }
        )
        QuickAction(
            icon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "历史",
                    tint = Color.White
                )
            },
            label = "历史",
            compact = compact,
            onClick = { navController.navigate(Screen.History.route) }
        )
        QuickAction(
            icon = {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "收藏",
                    tint = Color.White
                )
            },
            label = "收藏",
            compact = compact,
            onClick = { navController.navigate(Screen.Favorite.route) }
        )
        QuickAction(
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = Color.White
                )
            },
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
        if (tabs.isNotEmpty()) {
            HomeTabsRow(
                items = tabs.map { it.type_name.ifBlank { "未命名分类" } },
                selectedIndex = selectedTabIndex,
                onSelect = onTabSelected,
                modifier = Modifier.padding(top = 18.dp)
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
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
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
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (selected) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f)
                )
                .padding(horizontal = if (selected) 14.dp else 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(BrandPrimary)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "当前",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = text.ifBlank { "未命名分类" },
                color = if (selected) Color.White else AppTextPrimary,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
