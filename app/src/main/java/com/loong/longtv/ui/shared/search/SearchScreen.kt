package com.loong.longtv.ui.shared.search

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.loong.longtv.nav.Screen
import com.loong.longtv.ui.shared.search.TagGroup
import com.loong.longtv.ui.shared.search.SearchViewModel
import com.loong.longtv.ui.theme.AppSurface
import com.loong.longtv.ui.theme.AppSurfaceVariant
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.AppThemeTokens
import com.loong.longtv.ui.theme.BrandAccent
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.shared.widgets.AppHero
import com.loong.longtv.ui.shared.widgets.AppPageHeader
import com.loong.longtv.ui.shared.widgets.AppScreen
import com.loong.longtv.ui.shared.widgets.SectionHeader
import com.loong.longtv.ui.shared.widgets.TvSurface

/**
 * @author zl
 * @Date 2025/10/20
 **/
@Composable
internal fun TvSearchWorkspace(
    modifier: Modifier = Modifier,
    keyword: String,
    onKeywordChange: (String) -> Unit,
    historyList: List<String>,
    hotKeywords: List<String>,
    suggestionList: List<String>,
    suggestionLoading: Boolean,
    onSearch: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRemoveHistory: (String) -> Unit
) {
    val trimmedKeyword = keyword.trim()
    val showSuggestion = trimmedKeyword.isNotBlank()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        TvSearchInputPanel(
            modifier = Modifier
                .width(340.dp)
                .fillMaxHeight(),
            keyword = keyword,
            onKeywordChange = onKeywordChange,
            onSearch = onSearch
        )
        TvHotSearchPanel(
            modifier = Modifier.weight(1f),
            hotKeywords = hotKeywords,
            onSearch = onSearch
        )
        TvSidePanel(
            modifier = Modifier.width(320.dp),
            keyword = trimmedKeyword,
            showSuggestion = showSuggestion,
            historyList = historyList,
            suggestionList = suggestionList,
            suggestionLoading = suggestionLoading,
            onSearch = onSearch,
            onClearHistory = onClearHistory,
            onRemoveHistory = onRemoveHistory
        )
    }
}

@Composable
private fun TvSearchInputPanel(
    modifier: Modifier = Modifier,
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearch: (String) -> Unit
) {
    SearchSectionCard(modifier = modifier) {
        SectionHeader(
            title = "搜索",
            subtitle = "使用下方定制键盘输入"
        )

        Spacer(modifier = Modifier.height(8.dp))

        BasicTextField(
            value = keyword,
            onValueChange = { onKeywordChange(it.toSingleLineSearchInput()) },
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(AppThemeTokens.fieldBackground(), AppThemeTokens.fieldShape())
                .border(
                    1.dp,
                    if (keyword.isBlank()) AppThemeTokens.mutedBorder() else BrandAccent.copy(alpha = 0.28f),
                    AppThemeTokens.fieldShape()
                )
                .padding(horizontal = 14.dp),
            textStyle = TextStyle(
                color = AppTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            ),
            readOnly = true,
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { onSearch(keyword.trim()) }
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (keyword.isBlank()) {
                        Text(
                            text = "输入片名、演员、导演或拼音字母",
                            color = AppTextMuted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchKeyboardAction(
                label = "搜索",
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp),
                fontSize = 13.sp,
                accent = true,
                containerAlpha = if (keyword.isBlank()) 0.55f else 1f,
                onClick = { onSearch(keyword.trim()) }
            )
            SearchKeyboardAction(
                label = "删除",
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp),
                fontSize = 13.sp,
                containerAlpha = if (keyword.isBlank()) 0.45f else 1f,
                onClick = {
                    if (keyword.isNotEmpty()) onKeywordChange(keyword.dropLast(1))
                }
            )
            SearchKeyboardAction(
                label = "清空",
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp),
                fontSize = 13.sp,
                containerAlpha = if (keyword.isBlank()) 0.45f else 1f,
                onClick = { onKeywordChange("") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (keyword.isBlank()) "输入后右侧会切换到联想结果" else "当前输入：$keyword",
            color = AppTextMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Keyboard(
            modifier = Modifier
                .fillMaxWidth()
                .height(296.dp)
        ) { key ->
            onKeywordChange((keyword + key).toSingleLineSearchInput())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TvHotSearchPanel(
    modifier: Modifier = Modifier,
    hotKeywords: List<String>,
    onSearch: (String) -> Unit
) {
    SearchSectionCard(
        modifier = modifier.fillMaxHeight()
    ) {
        SectionHeader(
            title = "热门搜索",
            subtitle = "从当前热词里快速发起搜索"
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (hotKeywords.isEmpty()) {
            SearchEmptyHint(
                title = "暂无热门搜索",
                subtitle = "稍后再来看看，或者先从左侧直接输入关键词。"
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                hotKeywords.take(16).forEachIndexed { index, keyword ->
                    HotKeywordChip(
                        rank = index + 1,
                        keyword = keyword,
                        label = hotLabelFor(index + 1),
                        onClick = { onSearch(keyword) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvSidePanel(
    modifier: Modifier = Modifier,
    keyword: String,
    showSuggestion: Boolean,
    historyList: List<String>,
    suggestionList: List<String>,
    suggestionLoading: Boolean,
    onSearch: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRemoveHistory: (String) -> Unit
) {
    SearchSectionCard(
        modifier = modifier.fillMaxHeight()
    ) {
        SectionHeader(
            title = if (showSuggestion) "猜你想搜" else "搜索记录",
            subtitle = if (showSuggestion) {
                "根据当前输入实时获取联想结果"
            } else {
                "未输入时默认展示最近搜索"
            },
            action = if (!showSuggestion && historyList.isNotEmpty()) {
                {
                    SearchActionChip(
                        text = "清空历史",
                        onClick = onClearHistory
                    )
                }
            } else {
                null
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (showSuggestion) {
            TvSuggestionList(
                items = suggestionList,
                loading = suggestionLoading,
                emptyTitle = "暂无猜你想搜",
                emptySubtitle = "当前没有拿到联想结果，可以继续输入更多字母或直接搜索。",
                onSearch = onSearch
            )
        } else {
            TvHistoryList(
                historyList = historyList,
                onSearch = onSearch,
                onRemoveHistory = onRemoveHistory
            )
        }
    }
}

@Composable
private fun TvSuggestionList(
    items: List<String>,
    loading: Boolean,
    emptyTitle: String,
    emptySubtitle: String,
    onSearch: (String) -> Unit
) {
    if (loading) {
        SearchEmptyHint(
            title = "猜你想搜加载中",
            subtitle = "正在根据当前输入获取联想结果..."
        )
    } else if (items.isEmpty()) {
        SearchEmptyHint(
            title = emptyTitle,
            subtitle = emptySubtitle
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(items) { item ->
                TvKeywordRow(
                    text = item,
                    onClick = { onSearch(item) }
                )
            }
        }
    }
}

@Composable
private fun TvHistoryList(
    historyList: List<String>,
    onSearch: (String) -> Unit,
    onRemoveHistory: (String) -> Unit
) {
    if (historyList.isEmpty()) {
        SearchEmptyHint(
            title = "还没有搜索记录",
            subtitle = "先从左侧输入一次内容，这里会自动保留最近搜索。"
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(historyList) { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvSearchListItem(
                        onClick = { onSearch(item) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item,
                            color = AppTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    SearchMiniButton(
                        text = "X",
                        onClick = { onRemoveHistory(item) },
                        subtle = true
                    )
                }
            }
        }
    }
}

@Composable
private fun TvKeywordRow(
    text: String,
    onClick: () -> Unit
) {
    TvSearchListItem(onClick = onClick) {
        Text(
            text = text,
            color = AppTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "搜索",
            color = AppTextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun TvSearchListItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    TvSurface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                inset = 0.dp,
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, BrandPrimary.copy(alpha = 0.95f)),
                inset = 0.dp,
                shape = shape
            ),
            pressedBorder = Border(
                border = BorderStroke(2.dp, BrandPrimary),
                inset = 0.dp,
                shape = shape
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppSurface.copy(alpha = 0.92f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
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
    compact: Boolean,
    phoneLayout: Boolean
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
            if (phoneLayout) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (history in historyList) {
                        SearchHistoryPill(
                            text = history.keyword,
                            onSearch = { onHistoryClick(history.keyword) },
                            onRemove = { viewModel.removeKeyword(history.keyword) }
                        )
                    }
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (history in historyList) {
                        SearchActionChip(
                            text = history.keyword,
                            onClick = { onHistoryClick(history.keyword) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PhoneSearchLayout(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    historyList: List<String>,
    hotKeywords: List<String>,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRemoveHistory: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PhoneSearchBar(
            keyword = keyword,
            onKeywordChange = onKeywordChange,
            onBack = onBack,
            onSearch = onSearch
        )

        PhoneHistorySection(
            historyList = historyList,
            onSearch = onSearch,
            onClearHistory = onClearHistory,
            onRemoveHistory = onRemoveHistory
        )

        PhoneHotSearchSection(
            hotKeywords = hotKeywords,
            onSearch = onSearch
        )
    }
}

@Composable
private fun PhoneSearchBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onBack: () -> Unit,
    onSearch: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchMiniButton(
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = AppTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            },
            onClick = onBack
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(AppThemeTokens.fieldShape())
                .background(AppThemeTokens.fieldBackground())
                .border(1.dp, AppThemeTokens.mutedBorder(), AppThemeTokens.fieldShape())
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = AppTextMuted,
                    modifier = Modifier.size(18.dp)
                )
                BasicTextField(
                    value = keyword,
                    onValueChange = { onKeywordChange(it.toSingleLineSearchInput()) },
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    textStyle = TextStyle(
                        color = AppTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = { onSearch(keyword.trim()) }
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (keyword.isBlank()) {
                                Text(
                                    text = "搜索电影、电视剧、综艺...",
                                    color = AppTextMuted,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
        SearchMiniButton(
            text = "搜索",
            onClick = { onSearch(keyword.trim()) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhoneHistorySection(
    historyList: List<String>,
    onSearch: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRemoveHistory: (String) -> Unit
) {
    SearchSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "历史搜索",
                color = AppTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (historyList.isNotEmpty()) {
                SearchMiniButton(
                    text = "清空全部",
                    onClick = onClearHistory,
                    subtle = true
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (historyList.isEmpty()) {
            SearchEmptyHint(
                title = "还没有历史搜索",
                subtitle = "搜索过的影片会保留在这里，方便下次快速继续。"
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (keyword in historyList) {
                    SearchHistoryPill(
                        text = keyword,
                        onSearch = { onSearch(keyword) },
                        onRemove = { onRemoveHistory(keyword) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhoneHotSearchSection(
    hotKeywords: List<String>,
    onSearch: (String) -> Unit
) {
    SearchSectionCard {
        Text(
            text = "热门搜索",
            color = AppTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (hotKeywords.isEmpty()) {
            SearchEmptyHint(
                title = "暂无热门搜索",
                subtitle = "稍后再来看看，或者直接输入你想找的内容。"
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                hotKeywords.take(10).forEachIndexed { index, keyword ->
                    HotKeywordChip(
                        rank = index + 1,
                        keyword = keyword,
                        label = hotLabelFor(index + 1),
                        onClick = { onSearch(keyword) }
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
                for (group in groups) {
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

@Composable
internal fun SearchMiniButton(
    text: String? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    subtle: Boolean = false
) {
    TvSurface(
        onClick = onClick,
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (subtle) Color.White.copy(alpha = 0.02f) else Color.Transparent
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                icon()
            } else if (text != null) {
                Text(
                    text = text,
                    color = if (subtle) AppTextSecondary else AppTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
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
                "热门搜索" -> if (phoneLayout) "看看大家都在搜什么" else "挑一个热词，快速进入搜索结果"
                "已启用站点" -> if (phoneLayout) "查看当前可用搜索来源" else "当前已接入并支持搜索的站点"
                else -> if (phoneLayout) "轻触即可发起搜索" else "发现更多相关入口"
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (tag in group.tags) {
                SearchActionChip(
                    text = tag,
                    emphasize = group.title == "热门搜索",
                    onClick = { onClick(tag) }
                )
            }
        }
    }
}

@Composable
private fun SearchSectionCard(
    modifier: Modifier = Modifier,
    accentBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.06f),
            Color.White.copy(alpha = 0.03f)
        )
    ),
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(AppThemeTokens.cardShape())
            .background(accentBrush)
            .border(1.dp, AppThemeTokens.mutedBorder(), AppThemeTokens.cardShape())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        content()
    }
}

@Composable
private fun SearchHistoryPill(
    text: String,
    onSearch: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(AppThemeTokens.buttonShape())
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, AppThemeTokens.mutedBorder(), AppThemeTokens.buttonShape())
            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = AppTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .alpha(0.98f)
                .clickable(onClick = onSearch)
        )
        TvSurface(
            onClick = onRemove,
            shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp))
        ) {
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "X",
                    color = AppTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HotKeywordChip(
    rank: Int,
    keyword: String,
    label: String,
    onClick: () -> Unit
) {
    TvSurface(
        onClick = onClick,
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            AppSurfaceVariant.copy(alpha = 0.96f),
                            AppSurface.copy(alpha = 0.92f)
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = rank.toString(),
                    color = AppTextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = keyword,
                color = AppTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                color = AppTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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

private fun hotLabelFor(rank: Int): String {
    return when (rank) {
        1, 4, 8 -> "HOT"
        2 -> "TOP"
        3 -> "NEW"
        5, 6 -> "WAVE"
        7 -> "LIKE"
        else -> "UP"
    }
}

private fun String.toSingleLineSearchInput(): String =
    replace('\r', ' ').replace('\n', ' ')

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
