package com.loong.longtv.ui.shared.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.loong.longtv.ui.shared.widgets.AppButton
import com.loong.longtv.ui.shared.widgets.restoreTvFocus
import com.loong.longtv.ui.theme.AppCardOverlay
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FullscreenPlaybackFailure(
    message: String,
    isTv: Boolean,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
    onSwitchLine: (() -> Unit)?,
    onSearch: (() -> Unit)?,
    onBack: () -> Unit
) {
    Column(
        modifier = modifier
            .widthIn(max = 560.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppCardOverlay)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("播放失败", color = AppTextPrimary, fontSize = if (isTv) 22.sp else 17.sp, fontWeight = FontWeight.SemiBold)
        Text(message, color = AppTextSecondary, fontSize = if (isTv) 16.sp else 13.sp, textAlign = TextAlign.Center, maxLines = 3)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppButton(text = "重试", onClick = onRetry, modifier = Modifier.restoreTvFocus(isTv) {})
            if (onSwitchLine != null) AppButton(text = "换线路", onClick = onSwitchLine)
            if (onSearch != null) AppButton(text = "搜同名", onClick = onSearch)
            AppButton(text = "返回详情", onClick = onBack)
        }
    }
}
