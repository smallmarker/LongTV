package com.loong.longtv.ui.favorite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.loong.longtv.data.room.entity.FavoriteEntity
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.widgets.AppButton
import com.loong.longtv.ui.widgets.AppHero
import com.loong.longtv.ui.widgets.AppPageHeader
import com.loong.longtv.ui.widgets.AppScreen
import com.loong.longtv.ui.widgets.SectionHeader
import com.loong.longtv.ui.widgets.SmartAsyncImage
import com.loong.longtv.ui.widgets.TvSurface

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoriteScreen(
    navController: NavController,
    viewModel: FavoriteViewModel = hiltViewModel()
) {
    val deviceClass = currentDeviceClass()
    val compact = deviceClass != DeviceClass.TV
    val phoneLayout = deviceClass == DeviceClass.PHONE
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    AppScreen {
        if (phoneLayout) {
            AppPageHeader(
                title = "我的收藏",
                subtitle = "手机端以紧凑片单卡片管理收藏内容。"
            )
        } else {
            AppHero(
                title = "我的收藏",
                subtitle = "把常看的影片、剧集和片源收藏起来，形成自己的内容库。"
            )
        }
        SectionHeader(
            title = "收藏片单",
            subtitle = if (favorites.isEmpty()) "还没有收藏内容" else "共 ${favorites.size} 个条目",
            modifier = Modifier.padding(top = 20.dp)
        )

        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "暂无收藏", color = AppTextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(top = 14.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favorites) { item ->
                    FavoriteCard(
                        item = item,
                        compact = compact,
                        phoneLayout = phoneLayout,
                        onClick = {
                            navController.navigate(Screen.VideoDetail.createRoute(item.siteKey, item.videoId))
                        },
                        onRemove = { viewModel.remove(item.favoriteKey) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteCard(
    item: FavoriteEntity,
    compact: Boolean,
    phoneLayout: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    TvSurface(onClick = onClick) {
        if (phoneLayout) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmartAsyncImage(
                        model = item.cover,
                        contentDescription = item.title,
                        modifier = Modifier
                            .width(116.dp)
                            .aspectRatio(2f / 3f),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        fallbackText = item.title.ifBlank { "收藏内容" }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.title, color = AppTextPrimary, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = item.siteKey,
                            color = AppTextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                AppButton(text = "移除", onClick = onRemove)
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
                    fallbackText = item.title.ifBlank { "收藏内容" }
                )
                Column {
                    Text(text = item.title, color = AppTextPrimary, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(text = item.siteKey, color = AppTextMuted, fontSize = 13.sp)
                }
                AppButton(text = "移除", onClick = onRemove)
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
                    modifier = Modifier.width(160.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    fallbackText = item.title.ifBlank { "收藏内容" }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.title, color = AppTextPrimary, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = item.siteKey, color = AppTextMuted, fontSize = 13.sp)
                }
                AppButton(text = "移除", onClick = onRemove)
            }
        }
    }
}
