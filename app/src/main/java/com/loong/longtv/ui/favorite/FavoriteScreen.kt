package com.loong.longtv.ui.favorite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.loong.longtv.ui.common.isCompactScreen
import com.loong.longtv.ui.widgets.AppButton
import com.loong.longtv.ui.widgets.SmartAsyncImage
import com.loong.longtv.ui.widgets.TvSurface

@Composable
fun FavoriteScreen(
    navController: NavController,
    viewModel: FavoriteViewModel = hiltViewModel()
) {
    val compact = isCompactScreen()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
    ) {
        Text(text = "收藏", color = Color.White, fontSize = 24.sp)

        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "暂无收藏", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(favorites) { item ->
                    FavoriteCard(
                        item = item,
                        compact = compact,
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
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    TvSurface(onClick = onClick) {
        if (compact) {
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
                    Text(text = item.title, color = Color.White, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(text = item.siteKey, color = Color.Gray, fontSize = 13.sp)
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
                    Text(text = item.title, color = Color.White, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = item.siteKey, color = Color.Gray, fontSize = 13.sp)
                }
                AppButton(text = "移除", onClick = onRemove)
            }
        }
    }
}
