package com.loong.longtv.ui.shared.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.loong.longtv.engine.source.model.SiteConfig
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.AppThemeTokens
import com.loong.longtv.ui.theme.BrandAccent
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.theme.BrandSecondary

@Composable
internal fun SettingsContent(
    state: SettingsUiState,
    compact: Boolean,
    phoneLayout: Boolean,
    onSelectPopularSite: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(top = if (phoneLayout) 18.dp else 20.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(if (phoneLayout) 12.dp else 14.dp)
    ) {
        item {
            SettingsSectionTitle(
                title = "热门影视来源",
                subtitle = "搜索和播放使用本地 Spider；这里仅决定首页热门影视与热门列表的数据来源。"
            )
        }

        if (state.error != null) {
            item {
                Text(
                    text = "错误：${state.error}",
                    color = AppError,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        if (state.sites.isEmpty()) {
            item { SettingsEmptyState(phoneLayout = phoneLayout) }
        } else {
            items(state.sites, key = { it.key }) { site ->
                PopularSiteCard(
                    site = site,
                    selected = site.key == (state.popularSiteKey ?: state.sites.firstOrNull()?.key),
                    compact = compact,
                    onClick = { onSelectPopularSite(site.key) }
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            color = AppTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            color = AppTextMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PopularSiteCard(
    site: SiteConfig,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppThemeTokens.cardShape())
            .background(if (selected) BrandPrimary.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                color = if (selected) BrandPrimary.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.08f),
                shape = AppThemeTokens.cardShape()
            )
            .clickable(onClick = onClick)
            .padding(if (compact) 13.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = site.name.ifBlank { site.key },
                    color = AppTextPrimary,
                    fontSize = if (compact) 15.sp else 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = site.key,
                    color = AppTextMuted,
                    fontSize = if (compact) 12.sp else 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = if (selected) "当前" else "选择",
                color = if (selected) BrandPrimary else AppTextSecondary,
                fontSize = if (compact) 12.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (site.isSpiderSite()) {
                PillText(text = "Spider", color = BrandPrimary)
            }
            if (site.searchable == 1) {
                PillText(text = "可搜索", color = BrandAccent)
            }
            PillText(text = site.spiderClassName().ifBlank { "本地站点" }, color = BrandSecondary)
        }
    }
}

@Composable
private fun SettingsEmptyState(phoneLayout: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppThemeTokens.cardShape())
            .background(Color.White.copy(alpha = 0.05f))
            .padding(vertical = if (phoneLayout) 34.dp else 44.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "当前没有可用于热门影视的本地 Spider 站点",
            color = AppTextMuted,
            fontSize = if (phoneLayout) 13.sp else 16.sp
        )
    }
}

@Composable
private fun PillText(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(AppThemeTokens.buttonShape())
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
