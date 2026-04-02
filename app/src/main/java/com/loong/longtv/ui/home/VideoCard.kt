package com.loong.longtv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.loong.longtv.data.bean.Video
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.theme.AppCardOverlay
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.widgets.SmartAsyncImage

@Composable
fun VideoCard(video: Video) {
    when (currentDeviceClass()) {
        DeviceClass.TV -> TvVideoCard(video)
        DeviceClass.TABLET -> PhoneVideoCard(video, compact = false)
        DeviceClass.PHONE -> PhoneVideoCard(video, compact = true)
    }
}

@Composable
private fun PhoneVideoCard(
    video: Video,
    compact: Boolean
) {
    val title = video.vod_name.orEmpty().ifBlank { "未知标题" }
    val meta = listOf(video.vod_year, video.vod_remarks)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { video.type_name.ifBlank { "热门推荐" } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box {
            SmartAsyncImage(
                model = video.vod_pic.trim(),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop,
                fallbackText = title
            )
            Text(
                text = video.vod_remarks.orEmpty().ifBlank { "热播" },
                color = Color(0xFF0C1522),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(BrandPrimary)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppCardOverlay)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = if (compact) 14.sp else 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = meta,
                color = AppTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = video.vod_area.orEmpty().ifBlank { video.type_name.ifBlank { "片库" } },
                color = AppTextMuted,
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
private fun TvVideoCard(video: Video) {
    val title = video.vod_name.orEmpty().ifBlank { "未知标题" }
    val meta = listOf(video.vod_year, video.vod_remarks, video.type_name)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { "热门推荐" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box {
            SmartAsyncImage(
                model = video.vod_pic.trim(),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop,
                fallbackText = title
            )
            Text(
                text = video.vod_remarks.orEmpty().ifBlank { "热播" },
                color = Color(0xFF0C1522),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(BrandPrimary)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppCardOverlay)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = meta,
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = video.vod_area.orEmpty().ifBlank { "片库" },
                color = AppTextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
