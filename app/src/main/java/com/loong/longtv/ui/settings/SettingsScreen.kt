package com.loong.longtv.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.tv.material3.Text
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.widgets.AppButton
import com.loong.longtv.ui.widgets.AppHero
import com.loong.longtv.ui.widgets.AppPageHeader
import com.loong.longtv.ui.widgets.AppScreen
import com.loong.longtv.ui.widgets.SectionHeader
import com.loong.longtv.ui.widgets.TvSurface

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val deviceClass = currentDeviceClass()
    val compact = deviceClass != DeviceClass.TV
    val phoneLayout = deviceClass == DeviceClass.PHONE
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppScreen {
        if (phoneLayout) {
            AppPageHeader(
                title = "设置",
                subtitle = "手机端聚焦站点启停、默认站点和解析器状态。"
            )
        } else {
            AppHero(
                title = "播放与站点设置",
                subtitle = "管理默认站点、启用的片源和内置解析能力，让 TV 与手机都按你的偏好工作。"
            )
        }

        if (state.parserNames.isNotEmpty()) {
            SectionHeader(
                title = "内置解析器",
                subtitle = "用于补全部分站点返回的播放地址",
                modifier = Modifier.padding(top = 20.dp)
            )
            FlowRow(
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.parserNames.forEach { parserName ->
                    TvSurface(onClick = {}) {
                        Text(
                            text = parserName,
                            color = AppTextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        SectionHeader(
            title = "站点管理",
            subtitle = "启用常用站点，并指定一个默认首页来源"
        )
        LazyColumn(
            modifier = Modifier
                .padding(top = 14.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.sites) { item ->
                SiteSettingCard(
                    item = item,
                    compact = compact,
                    phoneLayout = phoneLayout,
                    defaultSiteKey = state.defaultSiteKey,
                    onDefault = { viewModel.setDefaultSite(item.site.key) },
                    onToggle = { viewModel.toggleSite(item.site.key, !item.enabled) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SiteSettingCard(
    item: SiteToggleItem,
    compact: Boolean,
    phoneLayout: Boolean,
    defaultSiteKey: String,
    onDefault: () -> Unit,
    onToggle: () -> Unit
) {
    TvSurface(onClick = {}) {
        when {
            phoneLayout -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SiteSettingInfo(
                        item = item,
                        defaultSiteKey = defaultSiteKey,
                        phoneLayout = true
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppButton(text = if (item.enabled) "停用" else "启用", onClick = onToggle)
                        AppButton(
                            text = "设为默认",
                            onClick = onDefault,
                            enabled = item.enabled && defaultSiteKey != item.site.key
                        )
                    }
                }
            }

            compact -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SiteSettingInfo(
                        item = item,
                        defaultSiteKey = defaultSiteKey,
                        phoneLayout = false
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppButton(text = if (item.enabled) "停用" else "启用", onClick = onToggle)
                        AppButton(
                            text = "设为默认",
                            onClick = onDefault,
                            enabled = item.enabled && defaultSiteKey != item.site.key
                        )
                    }
                }
            }

            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SiteSettingInfo(
                        item = item,
                        defaultSiteKey = defaultSiteKey,
                        phoneLayout = false,
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppButton(text = if (item.enabled) "停用" else "启用", onClick = onToggle)
                        AppButton(
                            text = "设为默认",
                            onClick = onDefault,
                            enabled = item.enabled && defaultSiteKey != item.site.key
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SiteSettingInfo(
    item: SiteToggleItem,
    defaultSiteKey: String,
    phoneLayout: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = item.site.name,
            color = AppTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = if (phoneLayout) 2 else 1,
            overflow = TextOverflow.Ellipsis
        )
        FlowRow(
            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingChip(text = item.site.spiderFamily().label)
            if (!phoneLayout) {
                SettingChip(text = "key: ${item.site.key}")
                SettingChip(text = "api: ${item.site.api}")
            }
        }
        Text(
            text = buildString {
                append(if (item.enabled) "已启用" else "已停用")
                if (defaultSiteKey == item.site.key) append(" / 默认站点")
            },
            color = AppTextSecondary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = buildString {
                append("type=${item.site.type}")
                if (item.site.isSearchable()) append(" / 可搜索")
                if (item.site.quickSearch != 0) append(" / 快搜")
            },
            color = AppTextMuted,
            fontSize = 12.sp,
            maxLines = if (phoneLayout) 2 else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun SettingChip(text: String) {
    TvSurface(onClick = {}) {
        Text(
            text = text,
            color = AppTextPrimary,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}
