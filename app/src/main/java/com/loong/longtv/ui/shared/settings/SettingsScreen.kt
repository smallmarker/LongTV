package com.loong.longtv.ui.shared.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.loong.longtv.engine.config.ConfigSource
import com.loong.longtv.engine.source.model.SiteConfig
import com.loong.longtv.ui.shared.widgets.AppButton
import com.loong.longtv.ui.shared.widgets.SectionHeader
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
    onSwitchSource: (ConfigSource) -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(top = 16.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(if (phoneLayout) 16.dp else 18.dp)
    ) {
        item {
            SectionHeader(
                title = "配置源",
                subtitle = "切换配置源将重新加载站点和Spider插件"
            )
        }

        items(state.availableSources) { source ->
            ConfigSourceCard(
                source = source,
                isCurrent = source == state.currentSource,
                loading = state.loading,
                compact = compact,
                phoneLayout = phoneLayout,
                onSwitch = { onSwitchSource(source) }
            )
        }

        if (state.error != null) {
            item {
                Text(
                    text = "错误：${state.error}",
                    color = com.loong.longtv.ui.theme.AppError,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        if (state.sites.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "已加载站点 (${state.sites.size})",
                    subtitle = "当前配置源中的Spider站点列表"
                )
            }

            items(state.sites) { site ->
                SiteInfoCard(
                    site = site,
                    compact = compact,
                    phoneLayout = phoneLayout
                )
            }
        }
    }
}

@Composable
private fun ConfigSourceCard(
    source: ConfigSource,
    isCurrent: Boolean,
    loading: Boolean,
    compact: Boolean,
    phoneLayout: Boolean,
    onSwitch: () -> Unit
) {
    val (label, desc) = when (source) {
        is ConfigSource.Asset -> Pair("本地内置（${source.assetPath}）", "使用应用内置的饭太硬配置包")
        is ConfigSource.Remote -> Pair("在线源（${source.url}）", "从网络拉取最新配置，需要网络权限")
        is ConfigSource.Local -> Pair("本地文件（${source.filePath}）", "从设备存储加载自定义配置")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) BrandPrimary.copy(alpha = 0.18f) else AppSurfaceVariant)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    color = AppTextPrimary,
                    fontSize = if (compact) 13.sp else 14.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = desc,
                    color = AppTextMuted,
                    fontSize = 12.sp
                )
                if (isCurrent) {
                    Text(
                        text = "当前使用中",
                        color = BrandAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (!isCurrent) {
                AppButton(
                    text = if (loading) "加载中..." else "切换",
                    onClick = onSwitch,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SiteInfoCard(
    site: SiteConfig,
    compact: Boolean,
    phoneLayout: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AppSurfaceVariant)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = site.name.ifBlank { site.key },
                color = AppTextPrimary,
                fontSize = if (compact) 13.sp else 14.sp,
                fontWeight = FontWeight.Medium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PillText(text = "key: ${site.key}", color = BrandSecondary)
                if (site.isSpiderSite()) {
                    PillText(text = "Spider: ${site.spiderClassName()}", color = BrandPrimary)
                }
                if (site.searchable == 1) {
                    PillText(text = "可搜索", color = BrandAccent)
                }
            }
        }
    }
}

@Composable
private fun PillText(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
