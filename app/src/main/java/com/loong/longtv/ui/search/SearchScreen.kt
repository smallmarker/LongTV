package com.loong.longtv.ui.search

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.loong.longtv.Screen
import com.loong.longtv.data.bean.TagGroup
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.theme.AppCardOverlay
import com.loong.longtv.ui.theme.AppSurface
import com.loong.longtv.ui.theme.AppSurfaceVariant
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.BrandAccent
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.theme.BrandSecondary
import com.loong.longtv.ui.widgets.AppHero
import com.loong.longtv.ui.widgets.AppPageHeader
import com.loong.longtv.ui.widgets.AppScreen
import com.loong.longtv.ui.widgets.SectionHeader
import com.loong.longtv.ui.widgets.TvSurface

/**
 * @author zl
 * @Date 2025/10/20
 **/
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController, viewModel: SearchViewModel = hiltViewModel()) {
    val deviceClass = currentDeviceClass()
    val compact = deviceClass != DeviceClass.TV
    val phoneLayout = deviceClass == DeviceClass.PHONE
    val historyList by viewModel.history.collectAsState()
    val groups by viewModel.searchState.collectAsState()

    val searchAction: (String) -> Unit = { keyword ->
        val trimmed = keyword.trim()
        if (trimmed.isNotBlank()) {
            navController.navigate(Screen.SearchResult.createRoute(trimmed))
        }
    }

    AppScreen {
        if (phoneLayout) {
            AppPageHeader(
                title = "搜索",
                subtitle = "先输入片名，或者从最近搜索和常用入口里一键继续。"
            )
        } else {
            AppHero(
                title = "搜索与发现",
                subtitle = "把常用搜索动作集中到一个工作区里，输入片名、继续最近搜索，或快速查看当前已启用站点。"
            )
        }
        if (compact) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SearchOverviewCard(
                    title = if (phoneLayout) "快速开搜" else "搜索控制台",
                    subtitle = if (phoneLayout) {
                        "当前保留了最近搜索和已启用站点，方便在手机上更快回到结果页。"
                    } else {
                        "输入关键词后可直接发起搜索，下方还会保留最近记录和站点入口。"
                    },
                    historyCount = historyList.size,
                    groupCount = groups.size,
                    phoneLayout = phoneLayout
                )

                SearchInputSection(
                    modifier = Modifier.fillMaxWidth(),
                    phoneLayout = phoneLayout,
                    onSearch = searchAction
                )

                SearchHistoryScreen(
                    modifier = Modifier.fillMaxWidth(),
                    viewModel = viewModel,
                    onHistoryClick = searchAction,
                    compact = true
                )

                TagListScreen(
                    modifier = Modifier.fillMaxWidth(),
                    groups = groups,
                    compact = true,
                    phoneLayout = phoneLayout,
                    onItemClick = searchAction
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.width(320.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    SearchOverviewCard(
                        title = "搜索控制台",
                        subtitle = "键入关键词后可以直接发起搜索，也可以继续最近使用的词条。",
                        historyCount = historyList.size,
                        groupCount = groups.size,
                        phoneLayout = false
                    )
                    SearchInputSection(
                        modifier = Modifier.fillMaxWidth(),
                        phoneLayout = false,
                        onSearch = searchAction
                    )
                    SearchHistoryScreen(
                        modifier = Modifier.fillMaxWidth(),
                        viewModel = viewModel,
                        onHistoryClick = searchAction,
                        compact = false
                    )
                }

                TagListScreen(
                    modifier = Modifier.weight(1f),
                    groups = groups,
                    compact = false,
                    phoneLayout = false,
                    onItemClick = searchAction
                )
            }
        }
    }
}

@Composable
private fun SearchInputSection(
    modifier: Modifier = Modifier,
    phoneLayout: Boolean,
    onSearch: (String) -> Unit
) {
    SearchSectionCard(modifier = modifier) {
        SectionHeader(
            title = if (phoneLayout) "输入关键词" else "开始搜索",
            subtitle = if (phoneLayout) "直接输入片名、演员或关键词" else "支持片名、演员、导演等关键词"
        )

        Spacer(modifier = Modifier.height(12.dp))

        SearchKeyboard(
            modifier = Modifier.fillMaxWidth(),
            onSearch = onSearch,
            onInputChange = {}
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchHistoryScreen(
    modifier: Modifier,
    viewModel: SearchViewModel,
    onHistoryClick: (String) -> Unit,
    compact: Boolean
) {
    val historyList by viewModel.history.collectAsState()

    SearchSectionCard(modifier = modifier) {
        SectionHeader(
            title = "搜索历史",
            subtitle = if (historyList.isEmpty()) "还没有历史记录" else "继续上次搜过的关键词",
            action = if (historyList.isNotEmpty()) {
                {
                    SearchActionChip(
                        text = "清空历史",
                        onClick = viewModel::clear
                    )
                }
            } else {
                null
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (historyList.isEmpty()) {
            SearchEmptyHint(
                title = "还没有搜索记录",
                subtitle = if (compact) {
                    "先搜一次影片，常用关键词会自动保留在这里。"
                } else {
                    "发起一次搜索后，最近使用的关键词会显示在这里，方便快速继续。"
                }
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                historyList.forEach { history ->
                    SearchActionChip(
                        text = history.keyword,
                        onClick = { onHistoryClick(history.keyword) }
                    )
                }
            }
        }
    }
}

@Composable
fun TagListScreen(
    modifier: Modifier = Modifier,
    groups: List<TagGroup>,
    compact: Boolean = false,
    phoneLayout: Boolean = false,
    onItemClick: (String) -> Unit
) {
    if (compact) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (groups.isEmpty()) {
                SearchSectionCard {
                    SearchEmptyHint(
                        title = "暂无推荐入口",
                        subtitle = "启用可搜索站点后，这里会展示常用搜索入口。"
                    )
                }
            } else {
                groups.forEach { group ->
                    TagItem(group, phoneLayout = phoneLayout) { wd ->
                        onItemClick(wd)
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (groups.isEmpty()) {
                item {
                    SearchSectionCard {
                        SearchEmptyHint(
                            title = "暂无推荐入口",
                            subtitle = "当前没有可展示的搜索分组，请先检查站点启用状态。"
                        )
                    }
                }
            }
            items(groups) { group ->
                TagItem(group, phoneLayout = phoneLayout) { wd ->
                    onItemClick(wd)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagItem(group: TagGroup, phoneLayout: Boolean = false, onClick: (String) -> Unit) {
    SearchSectionCard {
        SectionHeader(
            title = group.title,
            subtitle = when (group.title) {
                "最近搜索" -> "保留近期关键词，继续搜索更快"
                "已启用站点" -> if (phoneLayout) "查看当前可用搜索来源" else "当前已接入并支持搜索的站点"
                else -> if (phoneLayout) "轻触即可发起搜索" else "发现更多相关入口"
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            group.tags.forEach { tag ->
                SearchActionChip(
                    text = tag,
                    emphasize = group.title == "最近搜索",
                    onClick = { onClick(tag) }
                )
            }
        }
    }
}

@Composable
private fun SearchOverviewCard(
    title: String,
    subtitle: String,
    historyCount: Int,
    groupCount: Int,
    phoneLayout: Boolean
) {
    SearchSectionCard(
        accentBrush = Brush.linearGradient(
            colors = listOf(
                BrandPrimary.copy(alpha = 0.20f),
                BrandSecondary.copy(alpha = 0.14f),
                AppCardOverlay
            )
        )
    ) {
        Text(
            text = title,
            color = AppTextPrimary,
            fontSize = if (phoneLayout) 20.sp else 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = AppTextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SearchMetricCard(
                modifier = Modifier.weight(1f),
                label = "最近搜索",
                value = historyCount.toString()
            )
            SearchMetricCard(
                modifier = Modifier.weight(1f),
                label = "入口分组",
                value = groupCount.toString()
            )
        }
    }
}

@Composable
private fun SearchMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(AppSurface.copy(alpha = 0.92f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                color = AppTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = AppTextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SearchSectionCard(
    modifier: Modifier = Modifier,
    accentBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            AppSurfaceVariant.copy(alpha = 0.96f),
            AppSurface.copy(alpha = 0.88f)
        )
    ),
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(accentBrush)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        content()
    }
}

@Composable
private fun SearchActionChip(
    text: String,
    onClick: () -> Unit,
    emphasize: Boolean = false
) {
    TvSurface(
        onClick = onClick,
        modifier = Modifier,
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (emphasize) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                BrandAccent.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.Transparent)
                        )
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = if (emphasize) AppTextPrimary else AppTextSecondary,
                fontSize = 13.sp,
                fontWeight = if (emphasize) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun SearchEmptyHint(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                color = AppTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = AppTextMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}
