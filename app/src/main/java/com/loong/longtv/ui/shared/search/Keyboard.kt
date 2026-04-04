package com.loong.longtv.ui.shared.search

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Dp.Companion.Infinity
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.phone.search.PhoneKeyboardKey
import com.loong.longtv.ui.tv.search.TvKeyboardKey

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

        val widthByWidth = (maxWidth - horizontalSpacing * (spanCount - 1)) / spanCount
        val heightByRows = if (maxHeight != Infinity) {
            (maxHeight - verticalSpacing * (rows.size - 1)) / rows.size
        } else {
            widthByWidth
        }
        val width = if (heightByRows < widthByWidth) heightByRows else widthByWidth

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { label ->
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
    when (deviceClass) {
        DeviceClass.PHONE -> PhoneKeyboardKey(label = label, width = width, onClick = onClick)
        DeviceClass.TV -> TvKeyboardKey(label = label, width = width, onClick = onClick)
    }
}
