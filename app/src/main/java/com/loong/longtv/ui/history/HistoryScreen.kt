package com.loong.longtv.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.tv.material3.Text
import com.loong.longtv.Screen
import com.loong.longtv.data.room.entity.WatchHistoryEntity
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
import com.loong.longtv.ui.widgets.SmartAsyncImage
import com.loong.longtv.ui.widgets.TvSurface

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val deviceClass = currentDeviceClass()
    val compact = deviceClass != DeviceClass.TV
    val phoneLayout = deviceClass == DeviceClass.PHONE
    val histories by viewModel.histories.collectAsStateWithLifecycle()

    AppScreen {
        if (phoneLayout) {
            AppPageHeader(
                title = "观看历史",
                subtitle = "手机端聚焦继续观看和最近进度。"
            )
        } else {
            AppHero(
                title = "观看历史",
                subtitle = "自动记录上次看到哪一集，方便从客厅电视或手机继续追剧。"
            )
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = "继续观看",
                subtitle = if (histories.isEmpty()) "还没有历史记录" else "共 ${histories.size} 条记录"
            )
            AppButton(text = "清空", onClick = viewModel::clearAll)
        }

        if (histories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "暂无观看历史", color = AppTextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(top = 14.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(histories) { item ->
                    HistoryCard(item = item, compact = compact, phoneLayout = phoneLayout) {
                        navController.navigate(Screen.VideoDetail.createRoute(item.siteKey, item.videoId))
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    item: WatchHistoryEntity,
    compact: Boolean,
    phoneLayout: Boolean,
    onClick: () -> Unit
) {
    TvSurface(onClick = onClick) {
        if (phoneLayout) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmartAsyncImage(
                    model = item.cover,
                    contentDescription = item.title,
                    modifier = Modifier
                        .width(116.dp)
                        .aspectRatio(2f / 3f),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    fallbackText = item.title.ifBlank { "历史记录" }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.title, color = AppTextPrimary, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = "${item.sourceName} / ${item.episodeName}",
                        color = AppTextSecondary,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "进度 ${formatMs(item.positionMs)}",
                        color = AppTextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        } else if (compact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmartAsyncImage(
                    model = item.cover,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    fallbackText = item.title.ifBlank { "历史记录" }
                )
                Column {
                    Text(text = item.title, color = AppTextPrimary, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = "${item.sourceName} / ${item.episodeName}",
                        color = AppTextSecondary,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "进度 ${formatMs(item.positionMs)}",
                        color = AppTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmartAsyncImage(
                    model = item.cover,
                    contentDescription = item.title,
                    modifier = Modifier
                        .padding(4.dp)
                        .width(160.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    fallbackText = item.title.ifBlank { "历史记录" }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.title, color = AppTextPrimary, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = "${item.sourceName} / ${item.episodeName}",
                        color = AppTextSecondary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "进度 ${formatMs(item.positionMs)}",
                        color = AppTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
