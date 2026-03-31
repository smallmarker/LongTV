package com.loong.longtv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.tv.material3.Text
import com.loong.longtv.ui.common.isCompactScreen
import com.loong.longtv.ui.widgets.AppButton
import com.loong.longtv.ui.widgets.TvSurface

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val compact = isCompactScreen()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
    ) {
        Text(text = "设置", color = Color.White, fontSize = 24.sp)
        Text(
            text = "默认站点、站点开关和本地解析器",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )
        if (state.parserNames.isNotEmpty()) {
            Text(text = "内置解析器", color = Color.White, fontSize = 16.sp)
            FlowRow(
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.parserNames.forEach { parserName ->
                    TvSurface(onClick = {}) {
                        Text(
                            text = parserName,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.sites) { item ->
                SiteSettingCard(
                    item = item,
                    compact = compact,
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
    defaultSiteKey: String,
    onDefault: () -> Unit,
    onToggle: () -> Unit
) {
    TvSurface(onClick = {}) {
        if (compact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SiteSettingInfo(item = item, defaultSiteKey = defaultSiteKey)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton(text = if (item.enabled) "停用" else "启用", onClick = onToggle)
                    AppButton(
                        text = "设为默认",
                        onClick = onDefault,
                        enabled = item.enabled && defaultSiteKey != item.site.key
                    )
                }
            }
        } else {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SiteSettingInfo(
    item: SiteToggleItem,
    defaultSiteKey: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = item.site.name, color = Color.White, fontSize = 18.sp)
        FlowRow(
            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingChip(text = item.site.spiderFamily().label)
            SettingChip(text = "key: ${item.site.key}")
            SettingChip(text = "api: ${item.site.api}")
        }
        Text(
            text = buildString {
                append(if (item.enabled) "已启用" else "已停用")
                if (defaultSiteKey == item.site.key) append(" / 默认站点")
            },
            color = Color.Gray,
            fontSize = 13.sp
        )
        Text(
            text = buildString {
                append("type=${item.site.type}")
                if (item.site.isSearchable()) append(" / 可搜索")
                if (item.site.quickSearch != 0) append(" / 快搜")
            },
            color = Color(0xFF9AA0A6),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun SettingChip(text: String) {
    TvSurface(onClick = {}) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}
