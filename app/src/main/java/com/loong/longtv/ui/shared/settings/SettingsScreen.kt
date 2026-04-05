package com.loong.longtv.ui.shared.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.loong.longtv.ui.shared.widgets.AppButton
import com.loong.longtv.ui.shared.widgets.SectionHeader
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppSuccess
import com.loong.longtv.ui.theme.AppSurface
import com.loong.longtv.ui.theme.AppSurfaceVariant
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.BrandAccent
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.theme.BrandSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsContent(
    state: SettingsUiState,
    compact: Boolean,
    phoneLayout: Boolean,
    onDefault: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit
) {
    val singleSiteMode = state.sites.size <= 1
    val enabledCount = state.sites.count { it.enabled }
    val searchableCount = state.sites.count { it.site.isSearchable() }
    val defaultSite = state.sites.firstOrNull { it.site.key == state.defaultSiteKey }?.site

    LazyColumn(
        modifier = Modifier.padding(top = 16.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(if (phoneLayout) 16.dp else 18.dp)
    ) {
        item {
            SettingsOverviewCard(
                title = if (singleSiteMode) "当前配置一目了然" else "把常用片源整理成你的默认工作区",
                subtitle = buildString {
                    append(
                        if (singleSiteMode) {
                            "当前项目以单站点跑通完整链路为主，设置页更聚焦运行状态、搜索能力和解析器可用性。"
                        } else {
                            "优先展示默认站点、启用状态和搜索覆盖情况，让首页、搜索和播放都围绕你的主要片源工作。"
                        }
                    )
                },
                modeLabel = if (singleSiteMode) "单站点模式" else "多站点模式",
                siteCount = state.sites.size,
                enabledCount = enabledCount,
                parserCount = state.parserNames.size,
                searchableCount = searchableCount,
                defaultSiteName = defaultSite?.name ?: "未设置",
                compact = compact,
                phoneLayout = phoneLayout
            )
        }

        item {
            ParserCapabilityCard(
                parserNames = state.parserNames,
                compact = compact,
                phoneLayout = phoneLayout
            )
        }

        item {
            SectionHeader(
                title = if (singleSiteMode) "站点状态" else "站点编排",
                subtitle = if (singleSiteMode) {
                    "当前只保留一个核心站点，重点确认它是否启用、可搜索且处于默认入口。"
                } else {
                    "每个站点都展示类型、搜索能力和默认状态，方便快速决定首页与搜索应优先命中哪里。"
                }
            )
        }

        items(state.sites) { item ->
            SiteSettingCard(
                item = item,
                compact = compact,
                phoneLayout = phoneLayout,
                singleSiteMode = singleSiteMode,
                defaultSiteKey = state.defaultSiteKey,
                onDefault = { onDefault(item.site.key) },
                onToggle = { onToggle(item.site.key, !item.enabled) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsOverviewCard(
    title: String,
    subtitle: String,
    modeLabel: String,
    siteCount: Int,
    enabledCount: Int,
    parserCount: Int,
    searchableCount: Int,
    defaultSiteName: String,
    compact: Boolean,
    phoneLayout: Boolean
) {
    AppPanelCard(
        accent = listOf(
            BrandSecondary.copy(alpha = 0.28f),
            BrandAccent.copy(alpha = 0.18f),
            Color.Transparent
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccentPill(text = modeLabel, accent = BrandPrimary)
                AccentPill(text = "默认站点：$defaultSiteName", accent = BrandSecondary)
            }

            Text(
                text = title,
                color = AppTextPrimary,
                fontSize = if (phoneLayout) 22.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = if (phoneLayout) 30.sp else 34.sp
            )
            Text(
                text = subtitle,
                color = AppTextSecondary,
                fontSize = if (phoneLayout) 13.sp else 14.sp,
                lineHeight = if (phoneLayout) 19.sp else 21.sp
            )

            if (phoneLayout) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OverviewMetricRow(
                        "站点总数",
                        siteCount.toString(),
                        "已启用",
                        enabledCount.toString(),
                        prominent = true
                    )
                    OverviewMetricRow(
                        "可搜索",
                        searchableCount.toString(),
                        "内置解析器",
                        parserCount.toString(),
                        prominent = false
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OverviewMetricCard(
                        label = "站点总数",
                        value = siteCount.toString(),
                        modifier = Modifier.weight(1f),
                        prominent = false,
                        compact = compact
                    )
                    OverviewMetricCard(
                        label = "已启用",
                        value = enabledCount.toString(),
                        modifier = Modifier.weight(1f),
                        prominent = true,
                        compact = compact
                    )
                    OverviewMetricCard(
                        label = "可搜索",
                        value = searchableCount.toString(),
                        modifier = Modifier.weight(1f),
                        prominent = false,
                        compact = compact
                    )
                    OverviewMetricCard(
                        label = "内置解析器",
                        value = parserCount.toString(),
                        modifier = Modifier.weight(1f),
                        prominent = false,
                        compact = compact
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParserCapabilityCard(
    parserNames: List<String>,
    compact: Boolean,
    phoneLayout: Boolean
) {
    AppPanelCard(
        accent = listOf(
            BrandPrimary.copy(alpha = 0.22f),
            Color.Transparent
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader(
                title = "解析能力",
                subtitle = if (parserNames.isEmpty()) {
                    "当前没有加载到内置解析器，部分站点可能只能依赖直链播放。"
                } else {
                    "解析器用于补足部分片源返回的播放地址，越完整，跨站点播放越稳定。"
                }
            )

            if (parserNames.isEmpty()) {
                AccentPill(
                    text = "未检测到解析器",
                    accent = AppError,
                    subtle = true
                )
            } else {
                val summary = if (phoneLayout) {
                    "已就绪 ${parserNames.size} 个解析器"
                } else {
                    "当前共 ${parserNames.size} 个内置解析器可直接参与播放链路"
                }
                Text(
                    text = summary,
                    color = AppTextPrimary,
                    fontSize = if (compact) 13.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    parserNames.forEach { parserName ->
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SiteSettingCard(
    item: SiteToggleItem,
    compact: Boolean,
    phoneLayout: Boolean,
    singleSiteMode: Boolean,
    defaultSiteKey: String,
    onDefault: () -> Unit,
    onToggle: () -> Unit
) {
    val isDefault = defaultSiteKey == item.site.key
    val stateLabel = when {
        item.enabled && isDefault -> "已启用 / 默认站点"
        item.enabled -> "已启用"
        else -> "已停用"
    }

    AppPanelCard(
        accent = listOf(
            if (item.enabled) BrandPrimary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
            Color.Transparent
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (phoneLayout) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SiteSettingInfo(
                        item = item,
                        defaultSiteKey = defaultSiteKey,
                        phoneLayout = true
                    )
                    SiteActions(
                        singleSiteMode = singleSiteMode,
                        itemEnabled = item.enabled,
                        isDefault = isDefault,
                        onDefault = onDefault,
                        onToggle = onToggle
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SiteSettingInfo(
                        item = item,
                        defaultSiteKey = defaultSiteKey,
                        phoneLayout = false,
                        modifier = Modifier.weight(1f)
                    )
                    if (!singleSiteMode) {
                        SiteActions(
                            singleSiteMode = singleSiteMode,
                            itemEnabled = item.enabled,
                            isDefault = isDefault,
                            onDefault = onDefault,
                            onToggle = onToggle,
                            modifier = Modifier
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccentPill(
                    text = stateLabel,
                    accent = if (item.enabled) AppSuccess else AppError,
                    subtle = true
                )
                AccentPill(
                    text = if (item.site.isSearchable()) "支持搜索" else "不支持搜索",
                    accent = if (item.site.isSearchable()) BrandSecondary else AppTextMuted,
                    subtle = true
                )
                AccentPill(
                    text = if (item.site.quickSearch != 0) "支持快搜" else "普通搜索",
                    accent = BrandAccent,
                    subtle = true
                )
                AccentPill(
                    text = "播放器 ${item.site.playerType ?: 0}",
                    accent = BrandPrimary,
                    subtle = true
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SiteActions(
    singleSiteMode: Boolean,
    itemEnabled: Boolean,
    isDefault: Boolean,
    onDefault: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (singleSiteMode) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppButton(
            text = if (itemEnabled) "停用站点" else "启用站点",
            onClick = onToggle
        )
        AppButton(
            text = if (isDefault) "当前默认" else "设为默认",
            onClick = onDefault,
            enabled = itemEnabled && !isDefault
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SiteSettingInfo(
    item: SiteToggleItem,
    defaultSiteKey: String,
    phoneLayout: Boolean,
    modifier: Modifier = Modifier
) {
    val isDefault = defaultSiteKey == item.site.key
    val siteMeta = buildString {
        append("type=${item.site.type}")
        append(" / api=${item.site.api}")
        append(" / key=${item.site.key}")
    }

    Column(modifier = modifier) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccentPill(text = item.site.spiderFamily().label, accent = BrandSecondary, subtle = true)
            if (isDefault) {
                AccentPill(text = "默认入口", accent = BrandPrimary)
            }
        }

        Text(
            text = item.site.name,
            color = AppTextPrimary,
            fontSize = if (phoneLayout) 20.sp else 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = if (phoneLayout) 2 else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = siteMeta,
            color = AppTextSecondary,
            fontSize = if (phoneLayout) 12.sp else 13.sp,
            maxLines = if (phoneLayout) 3 else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = buildString {
                append(if (item.enabled) "站点处于启用状态，可参与首页和搜索调度。" else "站点已停用，不会参与首页与搜索。")
                if (isDefault) append(" 当前首页默认优先命中这里。")
            },
            color = AppTextMuted,
            fontSize = if (phoneLayout) 12.sp else 13.sp,
            lineHeight = if (phoneLayout) 18.sp else 20.sp,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun AppPanelCard(
    accent: List<Color>,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(accent))
            .background(AppSurface.copy(alpha = 0.68f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            content = content
        )
    }
}

@Composable
private fun OverviewMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    prominent: Boolean,
    compact: Boolean
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (prominent) BrandPrimary.copy(alpha = 0.16f)
                else AppSurfaceVariant.copy(alpha = 0.84f)
            )
            .border(
                width = 1.dp,
                color = if (prominent) BrandPrimary.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 14.dp, vertical = if (compact) 12.dp else 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                color = AppTextMuted,
                fontSize = 12.sp
            )
            Text(
                text = value,
                color = if (prominent) BrandPrimary else AppTextPrimary,
                fontSize = if (compact) 24.sp else 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun OverviewMetricRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String,
    prominent: Boolean
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OverviewMetricCard(
            label = leftLabel,
            value = leftValue,
            modifier = Modifier.weight(1f),
            prominent = prominent,
            compact = true
        )
        OverviewMetricCard(
            label = rightLabel,
            value = rightValue,
            modifier = Modifier.weight(1f),
            prominent = false,
            compact = true
        )
    }
}

@Composable
internal fun AccentPill(
    text: String,
    accent: Color,
    subtle: Boolean = false
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (subtle) accent.copy(alpha = 0.12f)
                else accent.copy(alpha = 0.18f)
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = if (subtle) 0.18f else 0.32f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(accent)
        )
        Text(
            text = text,
            color = AppTextPrimary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
