package com.loong.longtv.ui.tv.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.shared.widgets.TvSurface

@Composable
fun TvKeyboardKey(
    label: String,
    width: Dp,
    onClick: () -> Unit
) {
    TvSurface(
        onClick = onClick,
        modifier = Modifier
            .width(width)
            .height(width)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AppTextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}
