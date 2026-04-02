package com.loong.longtv.ui.search

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.widgets.TvSurface

/**
 * @author zl
 * @Date 2025/10/17
 **/
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Keyboard(
    modifier: Modifier = Modifier,
    onKeyClick: (String) -> Unit
) {
    val deviceClass = currentDeviceClass()
    val keys = listOf(
        "A", "B", "C", "D", "E", "F",
        "G", "H", "I", "J", "K", "L",
        "M", "N", "O", "P", "Q", "R",
        "S", "T", "U", "V", "W", "X",
        "Y", "Z",
        "1", "2", "3", "4", "5", "6",
        "7", "8", "9", "0"
    )

    val spanCount = 6 // 等价于 GridLayoutManager(6)
    val rows = keys.chunked(spanCount)

    // ✅ BoxWithConstraints: 让键盘在固定区域内自适配不裁剪
    BoxWithConstraints(
        modifier = modifier
    ) {
        val horizontalSpacing = 8.dp
        val verticalSpacing = 8.dp

        // 按键基础宽度
        val width = (maxWidth - horizontalSpacing * (spanCount - 1)) / spanCount

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEachIndexed { colIndex, label ->
                        KeyboardKey(
                            label = label,
                            width = width,
                            deviceClass = deviceClass,
                            onClick = { onKeyClick(label) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun KeyboardKey(
    label: String,
    width: Dp,
    deviceClass: DeviceClass,
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
                fontSize = when (deviceClass) {
                    DeviceClass.PHONE -> 15.sp
                    DeviceClass.TABLET -> 14.sp
                    DeviceClass.TV -> 12.sp
                },
                fontWeight = FontWeight.Medium,
                color = AppTextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}
