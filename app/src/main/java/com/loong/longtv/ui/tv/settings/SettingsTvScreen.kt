package com.loong.longtv.ui.tv.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.loong.longtv.ui.shared.settings.AccentPill
import com.loong.longtv.ui.shared.settings.SettingsUiState
import com.loong.longtv.ui.shared.settings.SettingsViewModel
import com.loong.longtv.ui.shared.settings.SiteToggleItem
import com.loong.longtv.ui.shared.settings.spiderFamily
import com.loong.longtv.ui.shared.widgets.AppButton
import com.loong.longtv.ui.shared.widgets.AppScreen
import com.loong.longtv.ui.shared.widgets.TvSurface
import com.loong.longtv.ui.theme.AppCardOverlay
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppSurface
import com.loong.longtv.ui.theme.AppSurfaceVariant
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.BrandAccent
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.theme.BrandSecondary

private enum class TvSettingsSection(
    val title: String,
    val subtitle: String
) {
    SOURCE("数据源管理", "管理默认站点、启用状态和搜索覆盖"),
    PLAYBACK("播放设置", "围绕当前工作区查看播放相关配置"),
    PARSER("解析能力", "确认内置解析器与当前可用能力"),
    ABOUT("项目说明", "当前 TV 端设置页聚焦真实已落地功能")
}

@Composable
fun SettingsTvScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedSection = remember { mutableStateOf(TvSettingsSection.SOURCE) }
    val sourcePanelFocusRequester = remember { FocusRequester() }

    AppScreen {
        SettingsTvTopBar(
            onBack = { navController.popBackStack() }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = 18.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TvSettingsRail(
                selected = selectedSection.value,
                onSelect = { selectedSection.value = it },
                panelFocusRequester = if (selectedSection.value == TvSettingsSection.SOURCE) {
                    sourcePanelFocusRequester
                } else {
                    null
                }
            )
            TvSettingsPanel(
                modifier = Modifier.weight(1f),
                state = state,
                section = selectedSection.value,
                onDefault = viewModel::setDefaultSite,
                onToggle = viewModel::toggleSite,
                sourcePanelFocusRequester = sourcePanelFocusRequester
            )
        }
    }
}

@Composable
private fun SettingsTvTopBar(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TvSurface(
            onClick = onBack,
            modifier = Modifier.width(116.dp),
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
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "返回",
                    color = AppTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "设置",
            color = AppTextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(116.dp))
    }
}

@Composable
private fun TvSettingsRail(
    selected: TvSettingsSection,
    onSelect: (TvSettingsSection) -> Unit,
    panelFocusRequester: FocusRequester?
) {
    TvCardShell(
        modifier = Modifier.width(280.dp),
        accent = listOf(
            BrandPrimary.copy(alpha = 0.14f),
            Color.Transparent
        )
    ) {
        Text(
            text = "设置分类",
            color = AppTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "用遥控器左右切换大类，右侧查看详情。",
            color = AppTextMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
        Column(
            modifier = Modifier.padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TvSettingsSection.entries.forEach { section ->
                TvSurface(
                    onClick = { onSelect(section) },
                    modifier = Modifier
                        .then(
                            if (panelFocusRequester != null) {
                                Modifier.focusProperties { right = panelFocusRequester }
                            } else {
                                Modifier
                            }
                        )
                        .onFocusChanged {
                            if (it.isFocused) onSelect(section)
                        },
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (section == selected) BrandPrimary.copy(alpha = 0.18f)
                        else AppSurfaceVariant.copy(alpha = 0.78f),
                        focusedContainerColor = BrandPrimary.copy(alpha = 0.24f),
                        pressedContainerColor = BrandPrimary.copy(alpha = 0.14f),
                        disabledContainerColor = AppSurface.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (section == selected) BrandPrimary.copy(alpha = 0.30f)
                                else Color.White.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = section.title,
                            color = AppTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = section.subtitle,
                            color = AppTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSettingsPanel(
    modifier: Modifier,
    state: SettingsUiState,
    section: TvSettingsSection,
    onDefault: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    sourcePanelFocusRequester: FocusRequester
) {
    TvCardShell(
        modifier = modifier,
        accent = listOf(
            BrandSecondary.copy(alpha = 0.16f),
            BrandAccent.copy(alpha = 0.08f),
            Color.Transparent
        )
    ) {
        Text(
            text = section.title,
            color = AppTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = section.subtitle,
            color = AppTextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f))
                .padding(top = 0.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
            when (section) {
                TvSettingsSection.SOURCE -> TvSourcePanel(
                    state = state,
                    onDefault = onDefault,
                    onToggle = onToggle,
                    panelFocusRequester = sourcePanelFocusRequester
                )
                TvSettingsSection.PLAYBACK -> TvPlaybackPanel(state = state)
                TvSettingsSection.PARSER -> TvParserPanel(state = state)
                TvSettingsSection.ABOUT -> TvAboutPanel(state = state)
            }
        }
    }
}

@Composable
private fun TvSourcePanel(
    state: SettingsUiState,
    onDefault: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    panelFocusRequester: FocusRequester
) {
    val currentItem = state.sites.firstOrNull { it.site.key == state.defaultSiteKey } ?: state.sites.firstOrNull()
    val firstSiteActionFocusRequester = remember { FocusRequester() }

    LazyColumn(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TvSectionBlock(
                title = "当前数据源",
                action = if (state.sites.size > 1) "默认站点会影响首页入口" else null,
                modifier = Modifier
                    .focusRequester(panelFocusRequester)
                    .focusProperties {
                        down = firstSiteActionFocusRequester
                    },
                focusableBlock = true
            ) {
                if (currentItem == null) {
                    Text(
                        text = "当前没有可用站点。",
                        color = AppTextMuted,
                        fontSize = 14.sp
                    )
                } else {
                    TvCurrentSourceCard(item = currentItem)
                }
            }
        }

        item {
            TvSectionBlock(
                title = "站点列表",
                action = "上下键可浏览全部站点"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.sites.forEach { item ->
                        TvSiteRow(
                            item = item,
                            isDefault = item.site.key == state.defaultSiteKey,
                            onDefault = { onDefault(item.site.key) },
                            onToggle = { onToggle(item.site.key, !item.enabled) },
                            primaryFocusRequester = if (item.site.key == state.defaultSiteKey || state.sites.firstOrNull()?.site?.key == item.site.key) {
                                firstSiteActionFocusRequester
                            } else {
                                null
                            },
                            upFocusRequester = if (item.site.key == state.defaultSiteKey || state.sites.firstOrNull()?.site?.key == item.site.key) {
                                panelFocusRequester
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvPlaybackPanel(
    state: SettingsUiState
) {
    val defaultItem = state.sites.firstOrNull { it.site.key == state.defaultSiteKey } ?: state.sites.firstOrNull()
    val enabledCount = state.sites.count { it.enabled }
    val searchableCount = state.sites.count { it.site.isSearchable() }

    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TvMetricCard(
                label = "站点总数",
                value = state.sites.size.toString(),
                prominent = false,
                modifier = Modifier.weight(1f)
            )
            TvMetricCard(
                label = "已启用",
                value = enabledCount.toString(),
                prominent = true,
                modifier = Modifier.weight(1f)
            )
            TvMetricCard(
                label = "可搜索",
                value = searchableCount.toString(),
                prominent = false,
                modifier = Modifier.weight(1f)
            )
        }

        TvSectionBlock(
            title = "当前工作区",
            modifier = Modifier.weight(1f),
            contentPadding = 12.dp,
            itemSpacing = 10.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(
                    leftLabel = "默认站点",
                    leftValue = defaultItem?.site?.name ?: "未设置",
                    rightLabel = "站点类型",
                    rightValue = defaultItem?.site?.spiderFamily()?.label ?: "未知"
                )
                DetailRow(
                    leftLabel = "API 标识",
                    leftValue = defaultItem?.site?.api ?: "未设置",
                    rightLabel = "搜索能力",
                    rightValue = if (defaultItem?.site?.isSearchable() == true) "支持搜索" else "不支持搜索"
                )
                DetailRow(
                    leftLabel = "快搜",
                    leftValue = if (defaultItem?.site?.quickSearch != 0) "已启用" else "未启用",
                    rightLabel = "播放器",
                    rightValue = "类型 ${defaultItem?.site?.playerType ?: 0}"
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TvParserPanel(
    state: SettingsUiState
) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvMetricCard(
                label = "解析器数量",
                value = state.parserNames.size.toString(),
                prominent = state.parserNames.isNotEmpty(),
                modifier = Modifier.weight(1f)
            )
            TvMetricCard(
                label = "默认站点",
                value = (state.sites.firstOrNull { it.site.key == state.defaultSiteKey }?.site?.name ?: "未设置"),
                prominent = false,
                modifier = Modifier.weight(1f)
            )
        }

        TvSectionBlock(
            title = "内置解析器",
            modifier = Modifier.weight(1f)
        ) {
            if (state.parserNames.isEmpty()) {
                AccentPill(
                    text = "未检测到解析器",
                    accent = AppError,
                    subtle = true
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.parserNames.forEach { parserName ->
                        AccentPill(
                            text = parserName,
                            accent = BrandSecondary,
                            subtle = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvAboutPanel(
    state: SettingsUiState
) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TvSectionBlock(
            title = "当前页面说明",
            modifier = Modifier.weight(1f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "这个 TV 端设置页只展示当前项目已经落地的真实功能，不额外铺设缓存、日志或解码等还未接入的假入口。",
                    color = AppTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Text(
                    text = "目前重点围绕数据源管理、播放工作区和解析能力展开，确保大屏上焦点路径稳定、信息层级清晰。",
                    color = AppTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                AccentPill(
                    text = "当前站点数：${state.sites.size}",
                    accent = BrandPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TvCurrentSourceCard(
    item: SiteToggleItem
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppSurfaceVariant.copy(alpha = 0.78f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = item.site.name,
            color = AppTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.site.api,
            color = AppTextSecondary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccentPill(text = item.site.spiderFamily().label, accent = BrandSecondary, subtle = true)
            AccentPill(text = if (item.enabled) "当前使用" else "已停用", accent = if (item.enabled) BrandPrimary else AppTextMuted, subtle = true)
        }
    }
}

@Composable
private fun TvSiteRow(
    item: SiteToggleItem,
    isDefault: Boolean,
    onDefault: () -> Unit,
    onToggle: () -> Unit,
    primaryFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppSurfaceVariant.copy(alpha = 0.72f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (isDefault) BrandPrimary else AppTextMuted.copy(alpha = 0.6f))
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.site.name,
                color = AppTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(item.site.spiderFamily().label)
                    append(" · ")
                    append(if (item.site.isSearchable()) "支持搜索" else "不支持搜索")
                },
                color = AppTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppButton(
                text = if (item.enabled) "停用" else "启用",
                onClick = onToggle,
                modifier = Modifier
                    .then(if (primaryFocusRequester != null) Modifier.focusRequester(primaryFocusRequester) else Modifier)
                    .then(if (upFocusRequester != null) Modifier.focusProperties { up = upFocusRequester } else Modifier)
            )
            AppButton(
                text = if (isDefault) "当前使用" else "设为当前",
                onClick = onDefault,
                enabled = item.enabled && !isDefault
            )
        }
    }
}

@Composable
private fun TvSectionBlock(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    focusableBlock: Boolean = false,
    contentPadding: androidx.compose.ui.unit.Dp = 16.dp,
    itemSpacing: androidx.compose.ui.unit.Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusableBlock) Modifier.focusable() else Modifier)
            .clip(RoundedCornerShape(18.dp))
            .background(AppSurface.copy(alpha = 0.38f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = AppTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!action.isNullOrBlank()) {
                    Text(
                        text = action,
                        color = AppTextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            content()
        }
    )
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = AppTextMuted,
            fontSize = 12.sp,
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            color = AppTextPrimary,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DetailRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DetailLine(
            label = leftLabel,
            value = leftValue,
            modifier = Modifier.weight(1f)
        )
        DetailLine(
            label = rightLabel,
            value = rightValue,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TvCardShell(
    modifier: Modifier = Modifier,
    accent: List<Color>,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(AppSurface.copy(alpha = 0.78f))
            .background(Brush.horizontalGradient(accent))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
private fun TvMetricCard(
    label: String,
    value: String,
    prominent: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(AppSurfaceVariant.copy(alpha = 0.88f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                color = AppTextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = AppTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
