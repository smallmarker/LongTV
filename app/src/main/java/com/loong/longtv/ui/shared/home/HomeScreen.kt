package com.loong.longtv.ui.shared.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.loong.longtv.R
import com.loong.longtv.engine.source.model.VodInfo
import com.loong.longtv.nav.Screen
import com.loong.longtv.ui.shared.widgets.AppButton
import com.loong.longtv.ui.shared.widgets.AppSearchFieldSurface
import com.loong.longtv.ui.shared.widgets.TvSurface
import com.loong.longtv.ui.shared.widgets.appFieldChrome
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppThemeTokens
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.utils.extensions.prefetchSmartImages

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun HomeScreenContent(
    navController: NavController,
    uiState: HomeUiState,
    isTv: Boolean,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val searchFocusRequester = remember { FocusRequester() }
    var searchFocused by remember { mutableStateOf(false) }
    var lastExitBackMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isTv) {
        if (isTv) searchFocusRequester.requestFocus()
    }

    BackHandler(enabled = isTv) {
        if (!searchFocused) {
            searchFocusRequester.requestFocus()
            lastExitBackMs = 0L
        } else {
            val now = SystemClock.elapsedRealtime()
            if (now - lastExitBackMs <= HOME_EXIT_BACK_INTERVAL_MS) {
                context.findActivity()?.finish()
            } else {
                lastExitBackMs = now
                Toast.makeText(context, "再按一次返回退出 LongTV", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = if (isTv) 32.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(if (isTv) AppThemeTokens.TvPageGap else AppThemeTokens.PhonePageGap)
    ) {
        MinimalHomeHeader(
            isTv = isTv,
            onSearch = { navController.navigate(Screen.Search.route) },
            onSettings = { navController.navigate(Screen.Settings.route) },
            searchFocusRequester = searchFocusRequester,
            onSearchFocusChanged = { searchFocused = it }
        )

        if (uiState.errorMessage != null) {
            HomeErrorBanner(message = uiState.errorMessage, onRetry = onRetry)
        } else if (uiState.loading) {
            Text(
                text = "正在准备搜索源...",
                color = AppTextMuted,
                fontSize = if (isTv) 16.sp else 13.sp
            )
        }

        MediaRailSection(
            title = "最近观看",
            actionText = "更多 >",
            emptyText = "还没有观看记录",
            items = uiState.recentWatching,
            isTv = isTv,
            onMore = { navController.navigate(Screen.History.route) },
            onItemClick = { item ->
                navController.navigate(Screen.VideoDetail.createRoute(item.siteKey, item.videoId))
            }
        )

        PopularRailSection(
            items = uiState.popularVideos,
            isTv = isTv,
            onMore = { navController.navigate(Screen.Popular.route) },
            onItemClick = { video ->
                navController.navigate(Screen.VideoDetail.createRoute(video.siteKey, video.id))
            }
        )

        MediaRailSection(
            title = "我的收藏",
            actionText = "更多 >",
            emptyText = "还没有收藏内容",
            items = uiState.favorites,
            isTv = isTv,
            onMore = { navController.navigate(Screen.Favorite.route) },
            onItemClick = { item ->
                navController.navigate(Screen.VideoDetail.createRoute(item.siteKey, item.videoId))
            }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MinimalHomeHeader(
    isTv: Boolean,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    searchFocusRequester: FocusRequester,
    onSearchFocusChanged: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(if (isTv) AppThemeTokens.TvSectionGap else AppThemeTokens.PhoneSectionGap)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(if (isTv) 18.dp else 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (isTv) 12.dp else 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "LongTV Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(if (isTv) 42.dp else 32.dp)
                            .clip(RoundedCornerShape(if (isTv) 12.dp else 9.dp))
                    )
                    Text(
                        text = "LongTV",
                        color = AppTextPrimary,
                        fontSize = if (isTv) 32.sp else 23.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }

                if (isTv) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeaderSearchField(
                            isTv = true,
                            onSearch = onSearch,
                            modifier = Modifier
                                .width(360.dp)
                                .focusRequester(searchFocusRequester)
                                .onFocusChanged { onSearchFocusChanged(it.isFocused) }
                        )
                        Text(
                            text = "支持遥控器语音键输入",
                            color = AppTextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(132.dp)
                        )
                    }
                }
            }

            if (isTv) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TvSurface(
                        onClick = onSettings,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "设置",
                                tint = AppTextPrimary,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clickable(onClick = onSettings),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = AppTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (!isTv) {
            HeaderSearchField(
                isTv = false,
                onSearch = onSearch,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeaderSearchField(
    isTv: Boolean,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppSearchFieldSurface(
        onClick = onSearch,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .height(if (isTv) AppThemeTokens.TvSearchHeight else AppThemeTokens.PhoneSearchHeight)
                .fillMaxWidth()
                .appFieldChrome()
                .padding(horizontal = if (isTv) 14.dp else 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isTv) 10.dp else 9.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = BrandPrimary,
                modifier = Modifier.size(if (isTv) 22.dp else 20.dp)
            )
            Text(
                text = "搜索电影 / 电视剧 / 动漫 / 综艺 / 音乐",
                color = AppTextMuted,
                fontSize = if (isTv) AppThemeTokens.SearchPromptTv else AppThemeTokens.SearchPromptPhone,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PopularRailSection(
    items: List<VodInfo>,
    isTv: Boolean,
    onMore: () -> Unit,
    onItemClick: (VodInfo) -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(items) {
        prefetchSmartImages(
            context = context,
            models = items.map { it.pic },
            limit = if (isTv) 16 else 10
        )
    }

    Column {
        HomeSectionHeader(
            title = "热门影视",
            actionText = "更多 >",
            isTv = isTv,
            onAction = onMore
        )

        if (items.isEmpty()) {
            EmptyLine(text = "暂无热门影视", isTv = isTv)
            return@Column
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isTv) 18.dp else 12.dp),
            contentPadding = PaddingValues(horizontal = if (isTv) 12.dp else 2.dp, vertical = if (isTv) 6.dp else 0.dp)
        ) {
            items(items, key = { "${it.siteKey}:${it.id}:${it.name}" }) { video ->
                TvSurface(onClick = { onItemClick(video) }) {
                    Box(modifier = Modifier.width(if (isTv) AppThemeTokens.TvPosterRailWidth else AppThemeTokens.PhonePosterRailWidth)) {
                        VideoCard(video = video)
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaRailSection(
    title: String,
    actionText: String,
    emptyText: String,
    items: List<HomeMediaItem>,
    isTv: Boolean,
    onMore: () -> Unit,
    onItemClick: (HomeMediaItem) -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(items) {
        prefetchSmartImages(
            context = context,
            models = items.map { it.cover },
            limit = if (isTv) 16 else 10
        )
    }

    Column {
        HomeSectionHeader(
            title = title,
            actionText = if (items.isNotEmpty()) actionText else null,
            isTv = isTv,
            onAction = onMore
        )

        if (items.isEmpty()) {
            EmptyLine(text = emptyText, isTv = isTv)
            return@Column
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isTv) 18.dp else 12.dp),
            contentPadding = PaddingValues(horizontal = if (isTv) 12.dp else 2.dp, vertical = if (isTv) 6.dp else 0.dp)
        ) {
            items(items, key = { "${it.siteKey}:${it.videoId}:${it.title}" }) { item ->
                TvSurface(onClick = { onItemClick(item) }) {
                    Box(modifier = Modifier.width(if (isTv) AppThemeTokens.TvPosterRailWidth else AppThemeTokens.PhonePosterRailWidth)) {
                        VideoCard(video = item.toVodInfo())
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    actionText: String?,
    isTv: Boolean,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = AppTextPrimary,
            fontSize = if (isTv) AppThemeTokens.SectionTitleTv else AppThemeTokens.SectionTitlePhone,
            fontWeight = FontWeight.SemiBold
        )
        if (actionText != null) {
            TvSurface(onClick = onAction) {
                Text(
                    text = actionText,
                    color = AppTextMuted,
                    fontSize = if (isTv) AppThemeTokens.ActionTextTv else AppThemeTokens.ActionTextPhone,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EmptyLine(text: String, isTv: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isTv) 110.dp else 84.dp)
            .padding(top = 12.dp)
            .clip(AppThemeTokens.cardShape())
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, AppThemeTokens.mutedBorder(), AppThemeTokens.cardShape()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = AppTextMuted,
            fontSize = if (isTv) 15.sp else 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun HomeErrorBanner(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppThemeTokens.cardShape())
            .background(AppError.copy(alpha = 0.12f))
            .border(1.dp, AppError.copy(alpha = 0.28f), AppThemeTokens.cardShape())
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = AppError,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        AppButton(text = "重试", onClick = onRetry)
    }
}

private fun HomeMediaItem.toVodInfo(): VodInfo {
    return VodInfo(
        id = videoId,
        name = title,
        pic = cover,
        remarks = meta
    ).also { it.siteKey = siteKey }
}

private const val HOME_EXIT_BACK_INTERVAL_MS = 2_000L

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
