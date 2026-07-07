package com.loong.longtv.ui.shared.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.Text
import com.loong.longtv.nav.Screen.VideoDetail
import com.loong.longtv.ui.shared.home.VideoCard
import com.loong.longtv.ui.theme.AppSurface
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.shared.widgets.AdaptiveChipTabs
import com.loong.longtv.ui.shared.widgets.AppButton
import com.loong.longtv.ui.shared.widgets.SectionHeader
import com.loong.longtv.ui.shared.widgets.TvSurface
import com.loong.longtv.utils.extensions.prefetchSmartImages

/**
 * @author zl
 * @Date 2025/10/21
 **/

@Composable
internal fun SearchResultContent(
    navigator: NavController,
    keyword: String,
    categories: List<String>,
    selectedCategory: String?,
    resultCount: Int,
    videos: List<com.loong.longtv.engine.source.model.VodInfo>,
    loading: Boolean,
    error: String?,
    compact: Boolean,
    phoneLayout: Boolean,
    onRetry: () -> Unit,
    onSelectCategory: (String) -> Unit
) {
    if (!phoneLayout) {
        SearchHeader(
            keyword = keyword,
            selectedCategory = selectedCategory,
            resultCount = resultCount,
            phoneLayout = false
        )
    }

    if (loading) {
        SearchCenterMessage("正在搜索 $keyword ...", Color.White)
    } else if (error != null && videos.isEmpty()) {
        SearchCenterMessage(
            message = error,
            color = Color.Red,
            actionLabel = "重试",
            onAction = onRetry
        )
    } else if (videos.isEmpty()) {
        SearchCenterMessage(
            message = "暂无搜索结果",
            color = Color.Gray,
            actionLabel = "重试",
            onAction = onRetry
        )
    } else if (compact) {
        AdaptiveChipTabs(
            items = categories,
            selectedIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
            onSelect = { index -> categories.getOrNull(index)?.let(onSelectCategory) },
            modifier = Modifier.padding(bottom = 16.dp)
        )
        SearchResultGrid(
            modifier = Modifier.fillMaxSize(),
            videos = videos,
            navigator = navigator,
            phoneLayout = phoneLayout
        )
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LazyColumn(
                modifier = Modifier.width(236.dp),
                contentPadding = PaddingValues(start = 8.dp, top = 6.dp, end = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    TvSurface(onClick = { onSelectCategory(category) }) {
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
                videos = videos,
                navigator = navigator,
                phoneLayout = false
            )
        }
    }
}

@Composable
internal fun SearchResultGrid(
    modifier: Modifier,
    videos: List<com.loong.longtv.engine.source.model.VodInfo>,
    navigator: NavController,
    phoneLayout: Boolean
) {
    val context = LocalContext.current
    LaunchedEffect(videos) {
        prefetchSmartImages(
            context = context,
            models = videos.map { it.pic },
            limit = if (phoneLayout) 18 else 36
        )
    }

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
                    val siteKey = video.siteKey
                    val videoId = video.id
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

@Composable
internal fun SearchHeader(
    keyword: String,
    selectedCategory: String?,
    resultCount: Int,
    phoneLayout: Boolean
) {
    val family = selectedCategory?.substringAfter("[")?.substringBefore("]")?.takeIf { it.isNotBlank() }
    if (phoneLayout) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = keyword,
                    color = AppTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = selectedCategory ?: "全部结果",
                    color = AppTextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            SearchResultMetaChip(text = "${resultCount} 条")
            if (family != null) {
                SearchResultMetaChip(text = family)
            }
        }
    } else {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            SectionHeader(
                title = "搜索: $keyword",
                subtitle = "当前结果 ${resultCount} 条"
            )
            if (selectedCategory != null) {
                Text(
                    text = selectedCategory,
                    color = AppTextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchResultMetaChip(text: String) {
    TvSurface(onClick = {}) {
        Box(
            modifier = Modifier
                .height(30.dp)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = AppTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
internal fun SearchCenterMessage(
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
