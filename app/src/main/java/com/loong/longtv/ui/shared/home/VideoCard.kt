package com.loong.longtv.ui.shared.home

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
import com.loong.longtv.engine.source.model.VodInfo
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.shared.widgets.SmartAsyncImage
import com.loong.longtv.ui.theme.AppThemeTokens
import com.loong.longtv.ui.theme.BrandPrimary

@Composable
fun VideoCard(video: VodInfo) {
    val isTv = currentDeviceClass() == DeviceClass.TV
    val title = video.displayTitle()
    val score = video.displayScore()
    val badgeText = video.displayBadge()
    val shape = AppThemeTokens.cardShape()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (isTv) 16f / 9f else 2f / 3f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartAsyncImage(
            model = video.pic.orEmpty().trim(),
            contentDescription = title,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape),
            contentScale = ContentScale.Crop,
            fallbackText = "暂无海报"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = if (isTv) 14.dp else 12.dp, vertical = if (isTv) 12.dp else 10.dp)
                .align(androidx.compose.ui.Alignment.BottomStart)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = if (isTv) 16.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (badgeText.isNotBlank()) {
            Text(
                text = badgeText,
                color = Color(0xFF0C1522),
                fontSize = if (isTv) 11.sp else 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(BrandPrimary)
                    .padding(horizontal = if (isTv) 10.dp else 9.dp, vertical = if (isTv) 5.dp else 4.dp)
            )
        }
        if (score != null) {
            Text(
                text = score,
                color = Color.White,
                fontSize = if (isTv) 11.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = if (isTv) 10.dp else 9.dp, vertical = if (isTv) 5.dp else 4.dp)
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

private fun VodInfo.displayBadge(): String {
    return remarks.normalizedDisplayText()
        .takeUnless { it.extractScoreText() != null }
        .orEmpty()
}

private fun String?.normalizedDisplayText(): String {
    val text = this.orEmpty().trim()
    if (text.isBlank()) return ""
    if (text.contains("暂无海报")) return ""
    if (text == "热播") return ""
    return text
}

private fun VodInfo.displayScore(): String? {
    val raw = listOf(score, remarks, typeName)
        .map { it.trim() }
        .firstNotNullOfOrNull { text -> text.extractScoreText() }
        ?: return null
    return raw.toFloatOrNull()
        ?.takeIf { it > 0f }
        ?.let { "%.1f".format(it) }
        ?: raw
}

private fun String.extractScoreText(): String? {
    if (isBlank()) return null
    val direct = toFloatOrNull()?.takeIf { it > 0f && it <= 10f }
    if (direct != null) return this
    val explicitScore = Regex("""(?:豆瓣|评分)\s*([0-9](?:\.[0-9])?|10(?:\.0)?)""").find(this)
        ?: Regex("""([0-9](?:\.[0-9])?|10(?:\.0)?)\s*分""").find(this)
    return explicitScore
        ?.groupValues
        ?.getOrNull(1)
        ?.takeIf { it.toFloatOrNull()?.let { value -> value > 0f && value <= 10f } == true }
}
