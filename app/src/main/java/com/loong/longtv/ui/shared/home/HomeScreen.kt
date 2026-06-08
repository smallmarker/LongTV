package com.loong.longtv.ui.shared.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.loong.longtv.R
import com.loong.longtv.engine.source.model.Category
import com.loong.longtv.engine.source.model.SiteConfig
import com.loong.longtv.engine.source.model.VodInfo
import com.loong.longtv.nav.Screen
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.shared.widgets.AppButton
import com.loong.longtv.ui.shared.widgets.TvSurface
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.BrandPrimary

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun HomeScreenContent(
    navController: NavController,
    uiState: HomeUiState,
    compact: Boolean,
    phoneLayout: Boolean,
    onRetry: () -> Unit,
    onTabFocused: ((Int) -> Unit)? = null,
    onTabSelected: (Int) -> Unit
) {
    when {
        uiState.loading -> LoadingScreen("正在加载站点首页...")
        uiState.errorMessage != null -> ErrorScreen(
            message = uiState.errorMessage,
            onRetry = onRetry
        )
        else -> HomeContentSection(
            tabs = uiState.tabs,
            selectedTabIndex = uiState.selectedTabIndex,
            currentSite = uiState.currentSite,
            compact = compact,
            phoneLayout = phoneLayout,
            videoPagingFlow = uiState.videoPagingFlow,
            onRetry = onRetry,
            onTabFocused = onTabFocused,
            onTabSelected = onTabSelected,
            navController = navController
        )
    }
}

@Composable
internal fun HomeCompactHeader(
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
internal fun HomeHeroHeader(
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
internal fun LongTvLogo(
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
internal fun QuickActions(navController: NavController) {
    QuickActions(navController = navController, compact = false)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalTvMaterial3Api::class)
@Composable
internal fun QuickActions(
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
internal fun HomeContentSection(
    tabs: List<Category>,
    selectedTabIndex: Int,
    currentSite: SiteConfig?,
    compact: Boolean,
    phoneLayout: Boolean,
    videoPagingFlow: kotlinx.coroutines.flow.Flow<PagingData<VodInfo>>,
    onRetry: () -> Unit,
    onTabFocused: ((Int) -> Unit)? = null,
    onTabSelected: (Int) -> Unit,
    navController: NavController
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (tabs.isNotEmpty()) {
            HomeTabsRow(
                items = tabs.map { it.name.ifBlank { "未命名分类" } },
                selectedIndex = selectedTabIndex,
                onFocusSelect = onTabFocused,
                onSelect = onTabSelected,
                modifier = Modifier.padding(top = 18.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            VideoListScreen(
                pagingFlow = videoPagingFlow,
                selectedTab = tabs.getOrNull(selectedTabIndex),
                currentSiteKey = currentSite?.key.orEmpty(),
                compact = compact,
                phoneLayout = phoneLayout,
                onRetry = onRetry,
                navController = navController
            )
        }
    }
}

@Composable
private fun HomeTabsRow(
    items: List<String>,
    selectedIndex: Int,
    onFocusSelect: ((Int) -> Unit)?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val isTv = currentDeviceClass() == DeviceClass.TV

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
                onFocused = {
                    if (isTv && index != selectedIndex) {
                        onFocusSelect?.invoke(index)
                    }
                },
                onClick = { onSelect(index) }
            )
        }
    }
}

@Composable
private fun HomeTabChip(
    text: String,
    selected: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    TvSurface(
        onClick = onClick,
        modifier = Modifier.onFocusChanged { focusState ->
            if (focusState.isFocused) {
                onFocused()
            }
        }
    ) {
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
    pagingFlow: kotlinx.coroutines.flow.Flow<PagingData<VodInfo>>,
    selectedTab: Category?,
    currentSiteKey: String,
    compact: Boolean,
    phoneLayout: Boolean,
    onRetry: () -> Unit,
    navController: NavController
) {
    val pagingItems = pagingFlow.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()
    var pullOffsetPx by remember { mutableFloatStateOf(0f) }
    val pullRefreshThresholdPx = 180f
    val nestedScrollConnection = remember(phoneLayout, gridState.firstVisibleItemIndex, pagingItems.loadState.refresh) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!phoneLayout || pagingItems.loadState.refresh is LoadState.Loading) return Offset.Zero
                val atTop = gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                return if (available.y > 0 && atTop) {
                    pullOffsetPx += available.y * 0.35f
                    Offset(0f, available.y * 0.35f)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (pullOffsetPx >= pullRefreshThresholdPx) {
                    pagingItems.refresh()
                }
                pullOffsetPx = 0f
                return Velocity.Zero
            }
        }
    }
    val refreshState = pagingItems.loadState.refresh
    val appendState = pagingItems.loadState.append

    LaunchedEffect(selectedTab?.id) {
        if (pagingItems.itemCount > 0) {
            gridState.animateScrollToItem(0)
        }
    }

    if (refreshState is LoadState.Loading && pagingItems.itemCount == 0) {
        LoadingScreen("正在加载内容...")
        return
    }

    if (refreshState is LoadState.Error && pagingItems.itemCount == 0) {
        ErrorScreen(
            message = refreshState.error.message ?: "内容加载失败",
            onRetry = {
                onRetry()
                pagingItems.retry()
            }
        )
        return
    }

    if (refreshState is LoadState.NotLoading && pagingItems.itemCount == 0) {
        EmptyScreen("${selectedTab?.name ?: "当前分类"}暂无内容")
        return
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .then(
                if (phoneLayout) {
                    Modifier.nestedScroll(nestedScrollConnection)
                } else {
                    Modifier
                }
            )
    ) {
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
            items(
                count = pagingItems.itemCount,
                key = pagingItems.itemKey { video -> video.id.ifBlank { "${video.typeId}-${video.name}" } }
            ) { index ->
                val video = pagingItems[index] ?: return@items
                TvSurface(
                    onClick = {
                        val title = video.name.trim()
                        if (title.isNotBlank()) {
                            navController.navigate(Screen.SearchResult.createRoute(title))
                        }
                    }
                ) {
                    VideoCard(video = video)
                }
            }

            if (appendState is LoadState.Loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "正在加载更多...",
                        color = AppTextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else if (appendState is LoadState.Error) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AppButton(
                        text = "加载更多失败，重试",
                        onClick = { pagingItems.retry() },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else if (appendState is LoadState.NotLoading && appendState.endOfPaginationReached && pagingItems.itemCount > 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "没有更多内容了",
                        color = AppTextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        if (phoneLayout && (pullOffsetPx > 0f || refreshState is LoadState.Loading)) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (refreshState is LoadState.Loading) "正在刷新..." else if (pullOffsetPx >= pullRefreshThresholdPx) "松手刷新" else "下拉刷新",
                    color = AppTextPrimary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
internal fun LoadingScreen(message: String) {
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
internal fun ErrorScreen(message: String, onRetry: () -> Unit) {
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

private fun VodInfo.safeSiteKey(fallback: String = ""): String {
    return siteKey.ifBlank { fallback }
}

private fun VodInfo.safeVideoId(): String {
    return id
}

private fun VodInfo.searchKeyword(): String {
    return listOf(name, typeName)
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() && it != "热播" && !it.contains("暂无海报") }
        .orEmpty()
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
