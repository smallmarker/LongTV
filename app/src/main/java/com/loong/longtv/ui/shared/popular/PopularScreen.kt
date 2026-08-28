package com.loong.longtv.ui.shared.popular

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.loong.longtv.engine.source.model.Category
import com.loong.longtv.engine.source.model.Filter
import com.loong.longtv.engine.source.model.FilterValue
import com.loong.longtv.engine.source.model.SiteConfig
import com.loong.longtv.engine.source.model.VodInfo
import com.loong.longtv.nav.Screen
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.shared.home.VideoCard
import com.loong.longtv.ui.shared.widgets.AppButton
import com.loong.longtv.ui.shared.widgets.TvSurface
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppSurface
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.utils.extensions.prefetchSmartImages
import kotlin.math.floor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun PopularScreenContent(
    navController: NavController,
    uiState: PopularUiState,
    isTv: Boolean,
    loadingText: String = "正在加载热门影视...",
    onRetry: () -> Unit,
    onTabFocused: ((Int) -> Unit)? = null,
    onTabSelected: (Int) -> Unit,
    onFilterSelected: (String, String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(if (isTv) 18.dp else 14.dp)
    ) {
        when {
            uiState.loading -> LoadingState(loadingText)
            uiState.errorMessage != null -> ErrorState(uiState.errorMessage, onRetry)
            else -> PopularContentSection(
                tabs = uiState.tabs,
                filters = uiState.filters,
                selectedFilters = uiState.selectedFilters,
                filterPanelVisible = uiState.filterPanelVisible,
                selectedTabIndex = uiState.selectedTabIndex,
                currentSite = uiState.currentSite,
                isTv = isTv,
                videoPagingFlow = uiState.videoPagingFlow,
                onRetry = onRetry,
                onTabFocused = onTabFocused,
                onTabSelected = onTabSelected,
                onFilterSelected = onFilterSelected,
                navController = navController
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PopularContentSection(
    tabs: List<Category>,
    filters: Map<String, List<Filter>>,
    selectedFilters: Map<String, String>,
    filterPanelVisible: Boolean,
    selectedTabIndex: Int,
    currentSite: SiteConfig?,
    isTv: Boolean,
    videoPagingFlow: Flow<PagingData<VodInfo>>,
    onRetry: () -> Unit,
    onTabFocused: ((Int) -> Unit)? = null,
    onTabSelected: (Int) -> Unit,
    onFilterSelected: (String, String) -> Unit,
    navController: NavController
) {
    val selectedTabFocusRequester = remember { FocusRequester() }
    var gridFocused by remember { mutableStateOf(false) }

    BackHandler(enabled = isTv && gridFocused) {
        selectedTabFocusRequester.requestFocus()
        gridFocused = false
    }

    BackHandler(enabled = filterPanelVisible) {
        onTabSelected(selectedTabIndex)
        if (isTv) selectedTabFocusRequester.requestFocus()
        gridFocused = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (tabs.isNotEmpty()) {
            PopularTabsRow(
                tabs = tabs,
                filters = filters,
                filterPanelVisible = filterPanelVisible,
                selectedIndex = selectedTabIndex,
                selectedFocusRequester = selectedTabFocusRequester,
                onFocusSelect = onTabFocused,
                onSelect = onTabSelected
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            PopularVideoGrid(
                pagingFlow = videoPagingFlow,
                selectedTab = tabs.getOrNull(selectedTabIndex),
                currentSite = currentSite,
                isTv = isTv,
                onGridFocusChanged = { gridFocused = it },
                onBackFromGrid = {
                    selectedTabFocusRequester.requestFocus()
                    gridFocused = false
                },
                onRetry = onRetry,
                navController = navController
            )

            if (filterPanelVisible) {
                PopularFilterPanel(
                    filters = filters[tabs.getOrNull(selectedTabIndex)?.id].orEmpty(),
                    selectedFilters = selectedFilters,
                    isTv = isTv,
                    onFilterSelected = onFilterSelected,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
private fun PopularTabsRow(
    tabs: List<Category>,
    filters: Map<String, List<Filter>>,
    filterPanelVisible: Boolean,
    selectedIndex: Int,
    selectedFocusRequester: FocusRequester,
    onFocusSelect: ((Int) -> Unit)?,
    onSelect: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val isTv = currentDeviceClass() == DeviceClass.TV

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        contentPadding = PaddingValues(horizontal = if (isTv) 10.dp else 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(tabs) { index, tab ->
            val hasFilters = filters[tab.id].orEmpty().isNotEmpty()
            val tabModifier = Modifier.onFocusChanged {
                if (isTv && it.isFocused && index != selectedIndex) onFocusSelect?.invoke(index)
            }
            TvSurface(
                onClick = { onSelect(index) },
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                modifier = if (index == selectedIndex) {
                    tabModifier.focusRequester(selectedFocusRequester)
                } else {
                    tabModifier
                }
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (index == selectedIndex) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tab.name.ifBlank { "未命名分类" },
                        color = AppTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (hasFilters) {
                        Text(
                            text = if (index == selectedIndex && filterPanelVisible) "▲" else "▼",
                            color = if (index == selectedIndex && filterPanelVisible) BrandPrimary else AppTextMuted,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PopularVideoGrid(
    pagingFlow: Flow<PagingData<VodInfo>>,
    selectedTab: Category?,
    currentSite: SiteConfig?,
    isTv: Boolean,
    onGridFocusChanged: (Boolean) -> Unit,
    onBackFromGrid: () -> Unit,
    onRetry: () -> Unit,
    navController: NavController
) {
    val pagingItems = pagingFlow.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val refreshState = pagingItems.loadState.refresh
    val appendState = pagingItems.loadState.append
    var focusedIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(pagingItems.itemCount) {
        prefetchSmartImages(
            context = context,
            models = pagingItems.itemSnapshotList.items.map { it.poster },
            limit = if (isTv) 12 else 8
        )
    }

    LaunchedEffect(selectedTab?.id) {
        if (pagingItems.itemCount > 0) gridState.animateScrollToItem(0)
    }

    if (refreshState is LoadState.Loading && pagingItems.itemCount == 0) {
        LoadingState("正在加载内容...")
        return
    }

    if (refreshState is LoadState.Error && pagingItems.itemCount == 0) {
        ErrorState(refreshState.error.message ?: "内容加载失败") {
            onRetry()
            pagingItems.retry()
        }
        return
    }

    if (refreshState is LoadState.NotLoading && pagingItems.itemCount == 0) {
        EmptyState("${selectedTab?.name ?: "当前分类"}暂无内容")
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxHeight()) {
        val density = LocalDensity.current
        val tvGridColumns = if (isTv) {
            with(density) {
                val availableWidth = (maxWidth - 20.dp).toPx()
                val minCellWidth = 150.dp.toPx()
                val spacing = 14.dp.toPx()
                floor((availableWidth + spacing) / (minCellWidth + spacing)).toInt().coerceAtLeast(1)
            }
        } else {
            2
        }

        LazyVerticalGrid(
            state = gridState,
            columns = if (isTv) GridCells.Adaptive(minSize = 150.dp) else GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxHeight()
                .then(
                    if (isTv) {
                        Modifier.onPreviewKeyEvent { event ->
                            if (event.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN) {
                                return@onPreviewKeyEvent false
                            }
                            when (event.nativeKeyEvent.keyCode) {
                                android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                    if (focusedIndex in 0 until tvGridColumns) {
                                        onBackFromGrid()
                                        true
                                    } else {
                                        false
                                    }
                                }

                                android.view.KeyEvent.KEYCODE_BACK,
                                android.view.KeyEvent.KEYCODE_ESCAPE -> {
                                    coroutineScope.launch {
                                        gridState.animateScrollToItem(0)
                                    }
                                    onBackFromGrid()
                                    true
                                }
                                else -> false
                            }
                        }
                    } else {
                        Modifier
                    }
                ),
            contentPadding = PaddingValues(
                start = if (isTv) 10.dp else 0.dp,
                top = if (isTv) 8.dp else 0.dp,
                end = if (isTv) 10.dp else 0.dp,
                bottom = if (isTv) 24.dp else 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (isTv) 14.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isTv) 14.dp else 12.dp)
        ) {
            items(
                count = pagingItems.itemCount,
                key = pagingItems.itemKey { video -> video.id.ifBlank { "${video.typeId}-${video.name}" } }
            ) { index ->
                val video = pagingItems[index] ?: return@items
                TvSurface(
                    modifier = Modifier.onFocusChanged { focusState ->
                        if (focusState.isFocused) focusedIndex = index
                        onGridFocusChanged(focusState.isFocused)
                    },
                    onClick = {
                        if (currentSite?.api == DOUBAN_API) {
                            navController.navigate(Screen.SearchResult.createRoute(video.name))
                        } else {
                            navController.navigate(
                                Screen.VideoDetail.createRoute(video.siteKey.ifBlank { currentSite?.key.orEmpty() }, video.id)
                            )
                        }
                    }
                ) {
                    VideoCard(video = video)
                }
            }

            if (appendState is LoadState.Loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("正在加载更多...", color = AppTextMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
            } else if (appendState is LoadState.Error) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AppButton(text = "加载更多失败，重试", onClick = { pagingItems.retry() }, modifier = Modifier.padding(vertical = 8.dp))
                }
            } else if (appendState is LoadState.NotLoading && appendState.endOfPaginationReached && pagingItems.itemCount > 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("没有更多内容了", color = AppTextMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

private const val DOUBAN_API = "csp_Douban"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PopularFilterPanel(
    filters: List<Filter>,
    selectedFilters: Map<String, String>,
    isTv: Boolean,
    onFilterSelected: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (filters.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AppSurface.copy(alpha = 0.96f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .padding(horizontal = if (isTv) 14.dp else 10.dp, vertical = if (isTv) 12.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (isTv) 10.dp else 8.dp)
    ) {
        for (filter in filters) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isTv) 12.dp else 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = filter.name,
                    color = AppTextMuted,
                    fontSize = if (isTv) 12.sp else 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .width(if (isTv) 42.dp else 34.dp)
                        .padding(top = 7.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (value in filter.value) {
                        FilterChip(
                            value = value,
                            selected = selectedFilters[filter.key] == value.v,
                            isTv = isTv,
                            onClick = { onFilterSelected(filter.key, value.v) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    value: FilterValue,
    selected: Boolean,
    isTv: Boolean,
    onClick: () -> Unit
) {
    TvSurface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) BrandPrimary.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.05f))
                .padding(horizontal = if (isTv) 12.dp else 10.dp, vertical = if (isTv) 7.dp else 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.n,
                color = if (selected) Color.White else AppTextPrimary,
                fontSize = if (isTv) 12.sp else 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LoadingState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = AppTextPrimary, fontSize = 16.sp)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = AppError, fontSize = 16.sp)
        AppButton(text = "重试", onClick = onRetry, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = AppTextMuted, fontSize = 16.sp)
    }
}
