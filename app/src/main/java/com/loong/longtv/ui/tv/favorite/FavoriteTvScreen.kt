package com.loong.longtv.ui.tv.favorite

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.loong.longtv.data.room.entity.FavoriteEntity
import com.loong.longtv.engine.TvBoxEngine
import com.loong.longtv.engine.source.model.VodInfo
import com.loong.longtv.nav.Screen
import com.loong.longtv.ui.shared.VideoPosterAspectRatio
import com.loong.longtv.ui.shared.favorite.FavoriteViewModel
import com.loong.longtv.ui.shared.widgets.AppScreen
import com.loong.longtv.ui.shared.widgets.SmartAsyncImage
import com.loong.longtv.ui.shared.widgets.TvSurface
import com.loong.longtv.ui.theme.AppBackgroundElevated
import com.loong.longtv.ui.theme.AppCardOverlay
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppSurface
import com.loong.longtv.ui.theme.AppSurfaceVariant
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.AppThemeTokens
import com.loong.longtv.ui.theme.BrandAccent
import com.loong.longtv.ui.theme.BrandPrimary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@Composable
fun FavoriteTvScreen(
    navController: NavController,
    viewModel: FavoriteViewModel = hiltViewModel(),
    detailViewModel: FavoriteTvDetailViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val detailMap by detailViewModel.detailMap.collectAsState()
    var editMode by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(TvFavoriteCategory.All) }
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }
    var sortByTime by remember { mutableStateOf(TvFavoriteSortOption.Latest) }
    var requestBatchFocus by remember { mutableStateOf(false) }
    val batchPrimaryFocusRequester = remember { FocusRequester() }

    val filteredFavorites = remember(favorites, selectedTab, sortByTime) {
        favorites
            .filter { selectedTab == TvFavoriteCategory.All || inferTvFavoriteCategory(it) == selectedTab }
            .sortedWith(
                sortByTime.comparator
                    .thenBy { it.title }
            )
    }

    LaunchedEffect(favorites, editMode) {
        val validKeys = favorites.mapTo(linkedSetOf()) { it.favoriteKey }
        selectedKeys = selectedKeys.filterTo(linkedSetOf()) { it in validKeys }
        if (!editMode) {
            selectedKeys = emptySet()
        }
        if (favorites.isEmpty()) {
            editMode = false
        }
    }

    LaunchedEffect(editMode, favorites.isNotEmpty()) {
        if (editMode && favorites.isNotEmpty() && requestBatchFocus) {
            batchPrimaryFocusRequester.requestFocus()
            requestBatchFocus = false
        }
    }

    AppScreen {
        FavoriteTvTopBar(
            editMode = editMode,
            canEdit = favorites.isNotEmpty(),
            onBack = { navController.popBackStack() },
            onToggleEdit = {
                requestBatchFocus = !editMode
                editMode = !editMode
                if (!editMode) {
                    selectedKeys = emptySet()
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (editMode && favorites.isNotEmpty()) {
            FavoriteTvBatchBar(
                primaryFocusRequester = batchPrimaryFocusRequester,
                allSelected = selectedKeys.size == filteredFavorites.size && filteredFavorites.isNotEmpty(),
                selectedCount = selectedKeys.size,
                onToggleSelectAll = {
                    selectedKeys = if (selectedKeys.size == filteredFavorites.size) {
                        emptySet()
                    } else {
                        filteredFavorites.mapTo(linkedSetOf()) { it.favoriteKey }
                    }
                },
                onDelete = {
                    selectedKeys.forEach(viewModel::remove)
                    selectedKeys = emptySet()
                    editMode = false
                }
            )
        } else {
            FavoriteTvCategoryBar(
                selectedTab = selectedTab,
                onSelectTab = {
                    selectedTab = it
                    selectedKeys = emptySet()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            FavoriteTvFilterBar(
                totalCount = filteredFavorites.size,
                timeSort = sortByTime,
                onToggleTimeSort = { sortByTime = sortByTime.nextTime() }
            )
        }

        if (favorites.isEmpty()) {
            FavoriteTvEmptyState()
        } else if (filteredFavorites.isEmpty()) {
            FavoriteTvFilteredEmptyState(selectedTab = selectedTab)
        } else {
            Spacer(modifier = Modifier.height(28.dp))
            FavoriteTvGridSection(
                favorites = filteredFavorites,
                detailMap = detailMap,
                editMode = editMode,
                selectedKeys = selectedKeys,
                onToggleSelected = { key ->
                    selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
                },
                onOpen = { item ->
                    navController.navigate(Screen.VideoDetail.createRoute(item.siteKey, item.videoId))
                },
                onEnterEditMode = { key ->
                    requestBatchFocus = false
                    editMode = true
                    selectedKeys = setOf(key)
                },
                onVisibleItemsChanged = detailViewModel::loadDetails
            )
        }
    }
}

@Composable
private fun FavoriteTvTopBar(
    editMode: Boolean,
    canEdit: Boolean,
    onBack: () -> Unit,
    onToggleEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TvSurface(
            onClick = onBack,
            modifier = Modifier.width(112.dp),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = AppCardOverlay.copy(alpha = 0.86f),
                focusedContainerColor = BrandPrimary.copy(alpha = 0.16f),
                pressedContainerColor = BrandPrimary.copy(alpha = 0.10f),
                disabledContainerColor = AppSurface.copy(alpha = 0.45f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = AppThemeTokens.buttonShape()
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = AppTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "返回",
                        color = AppTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "我的收藏",
            color = AppTextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.width(112.dp),
            horizontalArrangement = Arrangement.End
        ) {
            FavoriteTvTopActionChip(
                text = if (editMode) "完成" else "编辑",
                enabled = canEdit,
                prominent = editMode,
                onClick = onToggleEdit
            )
        }
    }
}

@Composable
private fun FavoriteTvCategoryBar(
    selectedTab: TvFavoriteCategory,
    onSelectTab: (TvFavoriteCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), AppThemeTokens.cardShape())
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = AppThemeTokens.cardShape()
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (category in TvFavoriteCategory.entries) {
            FavoriteTvTabChip(
                category = category,
                selected = selectedTab == category,
                onClick = { onSelectTab(category) }
            )
        }
    }
}

@Composable
private fun FavoriteTvFilterBar(
    totalCount: Int,
    timeSort: TvFavoriteSortOption,
    onToggleTimeSort: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), AppThemeTokens.cardShape())
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = AppThemeTokens.cardShape()
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "共 $totalCount 部收藏",
            color = AppTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FavoriteTvSortChip(text = "最近收藏 ${timeSort.label}", onClick = onToggleTimeSort)
        }
    }
}

@Composable
private fun FavoriteTvBatchBar(
    primaryFocusRequester: FocusRequester,
    allSelected: Boolean,
    selectedCount: Int,
    onToggleSelectAll: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), AppThemeTokens.cardShape())
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = AppThemeTokens.cardShape()
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FavoriteTvMiniChip(
            text = if (allSelected) "取消全选" else "全选",
            onClick = onToggleSelectAll,
            modifier = Modifier.focusRequester(primaryFocusRequester)
        )
        FavoriteTvMiniChip(
            text = if (selectedCount > 0) "删除($selectedCount)" else "删除",
            onClick = onDelete,
            enabled = selectedCount > 0,
            warning = selectedCount > 0
        )
    }
}

@Composable
private fun FavoriteTvGridSection(
    favorites: List<FavoriteEntity>,
    detailMap: Map<String, FavoriteTvCardMeta>,
    editMode: Boolean,
    selectedKeys: Set<String>,
    onToggleSelected: (String) -> Unit,
    onOpen: (FavoriteEntity) -> Unit,
    onEnterEditMode: (String) -> Unit,
    onVisibleItemsChanged: (List<FavoriteEntity>) -> Unit
) {
    var currentPage by remember(favorites) { mutableStateOf(0) }
    val pages = remember(favorites) { favorites.chunked(TV_FAVORITE_PAGE_SIZE) }
    val visibleItems = pages.getOrElse(currentPage) { emptyList() }

    LaunchedEffect(pages.size) {
        currentPage = currentPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    }

    LaunchedEffect(visibleItems) {
        onVisibleItemsChanged(visibleItems)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            visibleItems.chunked(TV_FAVORITE_COLUMNS).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    rowItems.forEach { item ->
                        FavoriteTvPosterCard(
                            item = item,
                            meta = detailMap[item.favoriteKey],
                            editMode = editMode,
                            selected = item.favoriteKey in selectedKeys,
                            onToggleSelected = { onToggleSelected(item.favoriteKey) },
                            onOpen = { onOpen(item) },
                            onLongPress = { onEnterEditMode(item.favoriteKey) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(TV_FAVORITE_COLUMNS - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        FavoriteTvPagination(
            currentPage = currentPage,
            pageCount = pages.size,
            onPrevious = { currentPage = (currentPage - 1).coerceAtLeast(0) },
            onNext = { currentPage = (currentPage + 1).coerceAtMost(pages.size - 1) }
        )
    }
}

@Composable
private fun FavoriteTvPosterCard(
    item: FavoriteEntity,
    meta: FavoriteTvCardMeta?,
    editMode: Boolean,
    selected: Boolean,
    onToggleSelected: () -> Unit,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember(item.favoriteKey) { mutableStateOf(false) }

    TvSurface(
        onClick = if (editMode) onToggleSelected else onOpen,
        onLongClick = if (editMode) null else onLongPress,
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer {
                scaleX = if (focused) 1.02f else 1f
                scaleY = if (focused) 1.02f else 1f
            },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = BrandPrimary.copy(alpha = 0.18f),
            pressedContainerColor = BrandPrimary.copy(alpha = 0.12f),
            disabledContainerColor = AppSurface.copy(alpha = 0.45f)
        ),
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (editMode) TV_FAVORITE_EDIT_CARD_HEIGHT else TV_FAVORITE_CARD_HEIGHT)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            when {
                                editMode && selected -> BrandPrimary.copy(alpha = 0.20f)
                                editMode -> AppSurface.copy(alpha = 0.82f)
                                focused -> BrandPrimary.copy(alpha = 0.16f)
                                else -> AppSurfaceVariant.copy(alpha = 0.96f)
                            },
                            when {
                                editMode && selected -> AppSurface.copy(alpha = 0.98f)
                                focused -> AppSurfaceVariant.copy(alpha = 0.98f)
                                else -> AppSurface.copy(alpha = 0.94f)
                            }
                        )
                    ),
                    shape = AppThemeTokens.cardShape()
                )
                .border(
                    width = 1.dp,
                    color = when {
                        selected -> BrandPrimary.copy(alpha = 0.56f)
                        focused -> BrandPrimary.copy(alpha = 0.38f)
                        else -> Color.White.copy(alpha = 0.07f)
                    },
                    shape = AppThemeTokens.cardShape()
                )
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopStart
            ) {
                SmartAsyncImage(
                    model = item.cover,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(VideoPosterAspectRatio)
                        .clip(AppThemeTokens.cardShape()),
                    contentScale = ContentScale.Crop,
                    fallbackText = item.title.ifBlank { "收藏内容" }
                )

                if (editMode) {
                    Box(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        FavoriteTvSelectionBadge(
                            selected = selected,
                            onClick = onToggleSelected
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        start = 14.dp,
                        end = 14.dp,
                        top = 12.dp,
                        bottom = if (editMode) 10.dp else 12.dp
                    ),
                verticalArrangement = if (editMode) Arrangement.spacedBy(5.dp) else Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.title.ifBlank { "未命名内容" },
                    color = if (focused && !editMode) BrandPrimary else AppTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                meta?.scoreEpisodeLine?.let { line ->
                    Text(
                        text = line,
                        color = AppTextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                meta?.remarkLine?.let { line ->
                    Text(
                        text = line,
                        color = AppTextMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!editMode) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = formatTvFavoriteDate(item.createTime),
                        color = if (focused) BrandPrimary else AppTextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteTvSelectionBadge(
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(
                color = if (selected) BrandPrimary else Color.Transparent,
                shape = CircleShape
            )
            .border(
                width = 1.5.dp,
                color = if (selected) BrandPrimary else AppTextMuted.copy(alpha = 0.7f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (selected) "✓" else "",
            color = AppBackgroundElevated,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FavoriteTvPagination(
    currentPage: Int,
    pageCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    if (pageCount <= 1) return

    Column(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .background(Color.White.copy(alpha = 0.06f), AppThemeTokens.cardShape())
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = AppThemeTokens.cardShape()
            )
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "第 ${currentPage + 1} / $pageCount 页",
            color = AppTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FavoriteTvMiniChip(
                text = if (currentPage > 0) "◀ 上一页" else "上一页",
                onClick = onPrevious,
                enabled = currentPage > 0
            )
            FavoriteTvMiniChip(
                text = if (currentPage < pageCount - 1) "下一页 ▶" else "下一页",
                onClick = onNext,
                enabled = currentPage < pageCount - 1
            )
        }
    }
}

@Composable
private fun FavoriteTvEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .background(Color.White.copy(alpha = 0.06f), AppThemeTokens.cardShape())
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = AppThemeTokens.cardShape()
                )
                .padding(horizontal = 28.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "暂无收藏内容",
                color = AppTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "将常看的影片收藏后，这里会以大屏网格方式展示你的片单。",
                color = AppTextMuted,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun FavoriteTvFilteredEmptyState(selectedTab: TvFavoriteCategory) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${selectedTab.label}分类下暂无收藏",
            color = AppTextMuted,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun FavoriteTvTopActionChip(
    text: String,
    enabled: Boolean,
    prominent: Boolean,
    onClick: () -> Unit
) {
    TvSurface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (!enabled) AppSurface.copy(alpha = 0.45f)
            else if (prominent) BrandPrimary.copy(alpha = 0.18f)
            else AppCardOverlay,
            focusedContainerColor = BrandPrimary.copy(alpha = 0.16f),
            pressedContainerColor = BrandPrimary.copy(alpha = 0.10f),
            disabledContainerColor = AppSurface.copy(alpha = 0.45f)
        )
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = if (enabled) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                    shape = AppThemeTokens.buttonShape()
                )
                .padding(horizontal = 14.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (!enabled) AppTextMuted else if (prominent) BrandPrimary else AppTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FavoriteTvTabChip(
    category: TvFavoriteCategory,
    selected: Boolean,
    onClick: () -> Unit
) {
    var focused by remember(category) { mutableStateOf(false) }

    LaunchedEffect(focused) {
        if (focused && !selected) {
            onClick()
        }
    }

    TvSurface(
        onClick = onClick,
        modifier = Modifier.onFocusChanged { focused = it.isFocused },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) BrandPrimary.copy(alpha = 0.18f) else AppSurface.copy(alpha = 0.66f),
            focusedContainerColor = BrandPrimary.copy(alpha = 0.22f),
            pressedContainerColor = BrandPrimary.copy(alpha = 0.16f)
        )
    ) {
        Box(
            modifier = Modifier
                .height(50.dp)
                .heightIn(min = 50.dp, max = 50.dp)
                .border(
                    width = 1.dp,
                    color = if (selected) BrandPrimary.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.07f),
                    shape = AppThemeTokens.buttonShape()
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${category.emoji} ${category.label}",
                color = if (selected) BrandPrimary else AppTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FavoriteTvSortChip(
    text: String,
    onClick: () -> Unit
) {
    TvSurface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AppSurface.copy(alpha = 0.66f),
            focusedContainerColor = BrandPrimary.copy(alpha = 0.16f),
            pressedContainerColor = BrandPrimary.copy(alpha = 0.10f)
        )
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = AppThemeTokens.buttonShape()
                )
                .padding(horizontal = 13.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "▼ $text",
                color = AppTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FavoriteTvMiniChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    warning: Boolean = false
) {
    TvSurface(
        onClick = onClick,
        modifier = modifier,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (!enabled) AppSurface.copy(alpha = 0.42f)
            else if (warning) AppError.copy(alpha = 0.12f)
            else AppSurface.copy(alpha = 0.66f),
            focusedContainerColor = if (warning) AppError.copy(alpha = 0.22f) else BrandPrimary.copy(alpha = 0.16f),
            pressedContainerColor = if (warning) AppError.copy(alpha = 0.18f) else BrandPrimary.copy(alpha = 0.10f),
            disabledContainerColor = AppSurface.copy(alpha = 0.42f)
        )
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = if (warning && enabled) AppError.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.08f),
                    shape = AppThemeTokens.buttonShape()
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (!enabled) AppTextMuted else if (warning) AppError else AppTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private enum class TvFavoriteCategory(val label: String, val emoji: String) {
    All("全部", "📺"),
    Film("影视", "🎬"),
    Anime("动漫", "📚"),
    Variety("综艺", "🎤"),
    Documentary("纪录片", "📰"),
    Playlist("收藏夹", "⭐")
}

private enum class TvFavoriteSortOption(
    val label: String,
    val comparator: Comparator<FavoriteEntity>
) {
    Latest("最近", compareByDescending<FavoriteEntity> { it.createTime }),
    Earliest("最早", compareBy<FavoriteEntity> { it.createTime });

    fun nextTime(): TvFavoriteSortOption = if (this == Latest) Earliest else Latest
}

private fun inferTvFavoriteCategory(item: FavoriteEntity): TvFavoriteCategory {
    val text = "${item.title} ${item.siteKey}".lowercase(Locale.getDefault())
    return when {
        text.contains("动漫") || text.contains("动画") || text.contains("anime") || text.contains("番") -> TvFavoriteCategory.Anime
        text.contains("综艺") || text.contains("晚会") || text.contains("脱口秀") || text.contains("真人秀") -> TvFavoriteCategory.Variety
        text.contains("纪录") || text.contains("documentary") || text.contains("自然") || text.contains("人文") -> TvFavoriteCategory.Documentary
        text.contains("合集") || text.contains("片单") || text.contains("收藏夹") -> TvFavoriteCategory.Playlist
        else -> TvFavoriteCategory.Film
    }
}

private fun formatTvFavoriteDate(timeMillis: Long): String {
    return SimpleDateFormat("yyyy.MM.dd", Locale.CHINA).format(Date(timeMillis))
}

private const val TV_FAVORITE_COLUMNS = 5
private const val TV_FAVORITE_PAGE_SIZE = 5
private val TV_FAVORITE_CARD_HEIGHT = 430.dp
private val TV_FAVORITE_EDIT_CARD_HEIGHT = 372.dp

data class FavoriteTvCardMeta(
    val score: Double? = null,
    val scoreEpisodeLine: String? = null,
    val remarkLine: String? = null
)

@HiltViewModel
class FavoriteTvDetailViewModel @Inject constructor(
    private val engine: TvBoxEngine
) : ViewModel() {

    private val _detailMap = MutableStateFlow<Map<String, FavoriteTvCardMeta>>(emptyMap())
    val detailMap: StateFlow<Map<String, FavoriteTvCardMeta>> = _detailMap.asStateFlow()
    private val loadingKeys = mutableSetOf<String>()
    private val loadingMutex = Mutex()
    private val detailSemaphore = Semaphore(permits = 3)

    fun loadDetails(items: List<FavoriteEntity>) {
        for (item in items) {
            viewModelScope.launch {
                val shouldLoad = loadingMutex.withLock {
                    if (_detailMap.value.containsKey(item.favoriteKey) || !loadingKeys.add(item.favoriteKey)) {
                        false
                    } else {
                        true
                    }
                }
                if (!shouldLoad) return@launch

                val meta = detailSemaphore.withPermit {
                    loadDetailMeta(item)
                }
                _detailMap.update { current -> current + (item.favoriteKey to meta) }
                loadingMutex.withLock {
                    loadingKeys.remove(item.favoriteKey)
                }
            }
        }
    }

    private suspend fun loadDetailMeta(item: FavoriteEntity): FavoriteTvCardMeta {
        val site = engine.currentConfig?.sites?.find { it.key == item.siteKey } ?: return FavoriteTvCardMeta()
        return engine.sourceRepository.getDetail(site, item.videoId)
            .getOrNull()?.toCardMeta() ?: FavoriteTvCardMeta()
    }
}

private fun VodInfo.toCardMeta(): FavoriteTvCardMeta {
    val score = this.score.trim().toDoubleOrNull()?.takeIf { it > 0.0 }
    val episodeCount = getPlayLines().maxOfOrNull { it.episodes.size }?.takeIf { it > 0 }
    val scoreEpisodeLine = buildList {
        score?.let { add("⭐ ${"%.1f".format(java.util.Locale.US, it)}") }
        episodeCount?.let { add("${it}集") }
    }.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    val remarkLine = remarks.trim().ifBlank {
        listOf(typeName.trim(), year.trim())
            .filter { it.isNotBlank() }
            .joinToString(" · ")
    }.ifBlank { null }

    return FavoriteTvCardMeta(
        score = score,
        scoreEpisodeLine = scoreEpisodeLine,
        remarkLine = remarkLine
    )
}
