package com.loong.longtv.ui.phone.home

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.loong.longtv.engine.source.model.VodInfo
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.shared.widgets.SmartAsyncImage

@Composable
fun PhoneVideoCard(video: VodInfo) {
    val title = video.displayTitle()
    val score = video.displayScore()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartAsyncImage(
            model = video.pic.orEmpty().trim(),
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
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .align(androidx.compose.ui.Alignment.BottomStart)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        val badgeText = video.remarks.orEmpty().ifBlank { "" }
        if (badgeText.isNotBlank()) {
            Text(
                text = badgeText,
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
        if (score != null) {
            Text(
                text = score,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            )
        }
    }
}

private fun VodInfo.displayTitle(): String {
    return listOf(name, typeName)
        .map { it.normalizedDisplayText() }
        .firstOrNull { it.isNotBlank() }
        ?: typeName.ifBlank { "未知标题" }
}

private fun String?.normalizedDisplayText(): String {
    val text = this.orEmpty().trim()
    if (text.isBlank()) return ""
    if (text.contains("暂无海报")) return ""
    if (text == "热播") return ""
    return text
}

private fun VodInfo.displayScore(): String? {
    val raw = score.trim()
    if (raw.isBlank()) return null
    return raw.toFloatOrNull()
        ?.takeIf { it > 0f }
        ?.let { "%.1f".format(it) }
        ?: raw
}
