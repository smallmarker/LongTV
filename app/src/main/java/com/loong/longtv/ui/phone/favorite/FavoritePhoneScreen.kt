package com.loong.longtv.ui.phone.favorite

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.loong.longtv.data.room.entity.FavoriteEntity
import com.loong.longtv.nav.Screen
import com.loong.longtv.ui.shared.favorite.FavoriteViewModel
import com.loong.longtv.ui.shared.widgets.AppScreen
import com.loong.longtv.ui.shared.widgets.SmartAsyncImage
import com.loong.longtv.ui.theme.AppBackgroundElevated
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppSurface
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.AppThemeTokens
import com.loong.longtv.ui.theme.BrandAccent
import com.loong.longtv.ui.theme.BrandPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FavoritePhoneScreen(
    navController: NavController,
    viewModel: FavoriteViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    var editMode by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(FavoritePhoneCategory.All) }
    var sortOption by remember { mutableStateOf(FavoriteSortOption.Latest) }
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }
    var visibleCount by remember { mutableIntStateOf(FAVORITE_PAGE_SIZE) }

    val filteredFavorites = remember(favorites, selectedTab, sortOption) {
        favorites
            .filter { selectedTab == FavoritePhoneCategory.All || inferFavoriteCategory(it) == selectedTab }
            .sortedWith(sortOption.comparator)
    }
    val visibleFavorites = remember(filteredFavorites, visibleCount) {
        filteredFavorites.take(visibleCount)
    }

    LaunchedEffect(favorites, editMode) {
        val validKeys = favorites.mapTo(linkedSetOf()) { it.favoriteKey }
        selectedKeys = selectedKeys.filterTo(linkedSetOf()) { it in validKeys }
        if (favorites.isEmpty()) {
            editMode = false
        }
        if (!editMode) {
            selectedKeys = emptySet()
        }
    }

    LaunchedEffect(selectedTab, sortOption, filteredFavorites.size) {
        visibleCount = minOf(FAVORITE_PAGE_SIZE, filteredFavorites.size.coerceAtLeast(FAVORITE_PAGE_SIZE))
    }

    AppScreen {
        FavoritePhoneTopBar(
            editMode = editMode,
            canEdit = favorites.isNotEmpty(),
            onBack = { navController.popBackStack() },
            onToggleEdit = {
                editMode = !editMode
                if (!editMode) {
                    selectedKeys = emptySet()
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (editMode) {
            FavoritePhoneBatchBar(
                allSelected = favorites.isNotEmpty() && selectedKeys.size == favorites.size,
                selectedCount = selectedKeys.size,
                onToggleSelectAll = {
                    selectedKeys = if (selectedKeys.size == favorites.size) {
                        emptySet()
                    } else {
                        favorites.mapTo(linkedSetOf()) { it.favoriteKey }
                    }
                },
                onDelete = {
                    selectedKeys.forEach(viewModel::remove)
                    selectedKeys = emptySet()
                    editMode = false
                }
            )
        } else {
            FavoritePhoneCategoryTabs(
                favorites = favorites,
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            FavoritePhoneStatsBar(
                totalCount = filteredFavorites.size,
                sortOption = sortOption,
                onToggleSort = {
                    sortOption = sortOption.next()
                }
            )
        }

        if (favorites.isEmpty()) {
            FavoritePhoneEmptyState()
        } else if (filteredFavorites.isEmpty()) {
            FavoritePhoneFilteredEmptyState(selectedTab = selectedTab)
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visibleFavorites, key = { it.favoriteKey }) { item ->
                    FavoritePhoneCard(
                        item = item,
                        editMode = editMode,
                        selected = item.favoriteKey in selectedKeys,
                        onOpen = {
                            navController.navigate(Screen.VideoDetail.createRoute(item.siteKey, item.videoId))
                        },
                        onToggleSelected = {
                            selectedKeys = if (item.favoriteKey in selectedKeys) {
                                selectedKeys - item.favoriteKey
                            } else {
                                selectedKeys + item.favoriteKey
                            }
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    FavoritePhoneFooter(
                        hasMore = visibleCount < filteredFavorites.size,
                        remainingCount = (filteredFavorites.size - visibleCount).coerceAtLeast(0),
                        onLoadMore = {
                            visibleCount = minOf(visibleCount + FAVORITE_PAGE_SIZE, filteredFavorites.size)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritePhoneTopBar(
    editMode: Boolean,
    canEdit: Boolean,
    onBack: () -> Unit,
    onToggleEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(AppThemeTokens.buttonShape())
                .clickable(onClick = onBack)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = AppTextPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "返回", color = AppTextSecondary, fontSize = 13.sp)
        }
        Text(
            text = "我的收藏",
            color = AppTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        FavoriteActionChip(
            text = if (editMode) "完成" else "编辑",
            enabled = canEdit,
            prominent = editMode,
            onClick = onToggleEdit
        )
    }
}

@Composable
private fun FavoritePhoneCategoryTabs(
    favorites: List<FavoriteEntity>,
    selectedTab: FavoritePhoneCategory,
    onSelectTab: (FavoritePhoneCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (category in FavoritePhoneCategory.entries) {
            FavoriteTabChip(
                category = category,
                selected = selectedTab == category,
                onClick = { onSelectTab(category) }
            )
        }
    }
}

@Composable
private fun FavoritePhoneStatsBar(
    totalCount: Int,
    sortOption: FavoriteSortOption,
    onToggleSort: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppThemeTokens.cardShape())
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = AppThemeTokens.cardShape()
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "共 $totalCount 部收藏",
            color = AppTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "最近收藏 ${sortOption.label}",
            color = AppTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onToggleSort)
        )
    }
}

@Composable
private fun FavoritePhoneBatchBar(
    allSelected: Boolean,
    selectedCount: Int,
    onToggleSelectAll: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppThemeTokens.cardShape())
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = AppThemeTokens.cardShape()
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (allSelected) "取消全选" else "全选",
            color = AppTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onToggleSelectAll)
        )
        Text(
            text = if (selectedCount > 0) "删除($selectedCount)" else "删除",
            color = if (selectedCount > 0) AppError else AppTextMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(enabled = selectedCount > 0, onClick = onDelete)
        )
    }
}

@Composable
private fun FavoritePhoneCard(
    item: FavoriteEntity,
    editMode: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggleSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppThemeTokens.cardShape())
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (selected) BrandPrimary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.07f),
                        if (selected) AppSurface.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.04f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = if (selected) BrandPrimary.copy(alpha = 0.52f) else Color.White.copy(alpha = 0.07f),
                shape = AppThemeTokens.cardShape()
            )
            .clickable(onClick = if (editMode) onToggleSelected else onOpen)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (editMode) {
            FavoriteSelectionDot(
                selected = selected,
                onClick = onToggleSelected
            )
        }

        SmartAsyncImage(
            model = item.cover,
            contentDescription = item.title,
            modifier = Modifier
                .width(104.dp)
                .aspectRatio(16f / 9f)
                .clip(AppThemeTokens.cardShape()),
            contentScale = ContentScale.Crop,
            fallbackText = item.title.ifBlank { "收藏内容" }
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = AppTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = favoriteMetaLine(item),
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (!editMode) {
                    Column(
                        modifier = Modifier.width(64.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        FavoritePlayChip(onClick = onOpen)
                    }
                }
            }
            Text(
                text = favoriteTimeLine(item.createTime),
                color = AppTextMuted,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FavoritePhoneEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(AppThemeTokens.cardShape())
                .background(Color.White.copy(alpha = 0.06f))
        )
        Text(
            text = "还没有收藏内容",
            color = AppTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            text = "点进剧集详情页收藏后，这里会按时间整理你的片单。",
            color = AppTextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun FavoritePhoneFilteredEmptyState(selectedTab: FavoritePhoneCategory) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${selectedTab.label}分类下还没有内容",
            color = AppTextMuted,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun FavoritePhoneFooter(
    hasMore: Boolean,
    remainingCount: Int,
    onLoadMore: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppThemeTokens.cardShape())
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(enabled = hasMore, onClick = onLoadMore)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (hasMore) "加载更多 · 还有 $remainingCount 部" else "已加载全部收藏",
            color = if (hasMore) AppTextPrimary else AppTextMuted,
            fontSize = 12.sp,
            fontWeight = if (hasMore) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun FavoriteSelectionDot(
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) BrandPrimary else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (selected) BrandPrimary else AppTextMuted.copy(alpha = 0.7f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(AppBackgroundElevated, CircleShape)
            )
        }
    }
}

@Composable
private fun FavoriteTabChip(
    category: FavoritePhoneCategory,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .heightIn(min = 48.dp, max = 48.dp)
            .clip(AppThemeTokens.buttonShape())
            .background(
                if (selected) BrandPrimary.copy(alpha = 0.18f)
                else Color.White.copy(alpha = 0.06f)
            )
            .border(
                width = 1.dp,
                color = if (selected) BrandPrimary.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.06f),
                shape = AppThemeTokens.buttonShape()
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${category.emoji} ${category.label}",
            color = if (selected) BrandPrimary else AppTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FavoritePlayChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(64.dp)
            .clip(AppThemeTokens.buttonShape())
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        BrandPrimary.copy(alpha = 0.22f),
                        BrandAccent.copy(alpha = 0.18f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = BrandPrimary.copy(alpha = 0.32f),
                shape = AppThemeTokens.buttonShape()
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 0.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "▶ 播放",
            color = BrandPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FavoriteActionChip(
    text: String,
    enabled: Boolean,
    prominent: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(AppThemeTokens.buttonShape())
            .background(
                if (!enabled) AppSurface.copy(alpha = 0.45f)
                else if (prominent) BrandPrimary.copy(alpha = 0.18f)
                else Color.White.copy(alpha = 0.06f)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (!enabled) AppTextMuted else if (prominent) BrandPrimary else AppTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private enum class FavoritePhoneCategory(val label: String, val emoji: String) {
    All("全部", "📺"),
    Film("影视", "🎬"),
    Anime("动漫", "📚"),
    Variety("综艺", "🎤"),
    Documentary("纪录片", "📰")
}

private enum class FavoriteSortOption(
    val label: String,
    val comparator: Comparator<FavoriteEntity>
) {
    Latest(
        label = "▼",
        comparator = compareByDescending<FavoriteEntity> { it.createTime }.thenBy { it.title }
    ),
    Earliest(
        label = "▲",
        comparator = compareBy<FavoriteEntity> { it.createTime }.thenBy { it.title }
    );

    fun next(): FavoriteSortOption = if (this == Latest) Earliest else Latest
}

private fun inferFavoriteCategory(item: FavoriteEntity): FavoritePhoneCategory {
    val text = "${item.title} ${item.siteKey}".lowercase(Locale.getDefault())
    return when {
        text.contains("动漫") || text.contains("动画") || text.contains("anime") || text.contains("番") -> FavoritePhoneCategory.Anime
        text.contains("综艺") || text.contains("晚会") || text.contains("脱口秀") || text.contains("真人秀") -> FavoritePhoneCategory.Variety
        text.contains("纪录") || text.contains("documentary") || text.contains("自然") || text.contains("人文") -> FavoritePhoneCategory.Documentary
        else -> FavoritePhoneCategory.Film
    }
}

private fun favoriteMetaLine(item: FavoriteEntity): String {
    val category = inferFavoriteCategory(item)
    val source = item.siteKey.ifBlank { "本地片源" }
    return "⭐ ${category.label} · 片源 $source"
}

private fun favoriteTimeLine(timeMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.CHINA)
    return "收藏于 ${formatter.format(Date(timeMillis))}"
}

private const val FAVORITE_PAGE_SIZE = 12
