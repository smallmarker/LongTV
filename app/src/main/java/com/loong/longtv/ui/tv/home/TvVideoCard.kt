package com.loong.longtv.ui.tv.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.loong.longtv.data.bean.Video
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.shared.widgets.SmartAsyncImage

@Composable
fun TvVideoCard(video: Video) {
    val title = video.vod_name.orEmpty().trim().ifBlank { "未知标题" }
    val score = video.displayScore()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartAsyncImage(
            model = video.vod_pic.trim(),
            contentDescription = title,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop,
            fallbackText = "暂无海报"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .align(androidx.compose.ui.Alignment.BottomStart)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
        val badgeText = video.vod_remarks.orEmpty().ifBlank { "" }
        if (badgeText.isNotBlank()) {
            Text(
                text = badgeText,
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
        if (score != null) {
            Text(
                text = score,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

private fun Video.displayScore(): String? {
    val raw = vod_score.trim()
    if (raw.isBlank()) return null
    return raw.toFloatOrNull()
        ?.takeIf { it > 0f }
        ?.let { "%.1f".format(it) }
        ?: raw
}
