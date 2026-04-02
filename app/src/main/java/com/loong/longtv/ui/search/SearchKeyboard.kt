package com.loong.longtv.ui.search

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Text
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.theme.AppSurfaceVariant
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.widgets.TvSurface

/**
 * @author zl
 * @Date 2025/10/17
 **/
@Composable
fun SearchKeyboard(
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit = {},
    onInputChange: (String) -> Unit = {}
) {
    val deviceClass = currentDeviceClass()
    val phoneLayout = deviceClass == DeviceClass.PHONE
    val fieldHeight = when (deviceClass) {
        DeviceClass.PHONE -> 48.dp
        DeviceClass.TABLET -> 44.dp
        DeviceClass.TV -> 24.dp
    }
    val actionHeight = when (deviceClass) {
        DeviceClass.PHONE -> 46.dp
        DeviceClass.TABLET -> 42.dp
        DeviceClass.TV -> 24.dp
    }
    val buttonWidth = when (deviceClass) {
        DeviceClass.PHONE -> 76.dp
        DeviceClass.TABLET -> 72.dp
        DeviceClass.TV -> 50.dp
    }
    val actionFontSize = when (deviceClass) {
        DeviceClass.PHONE -> 15.sp
        DeviceClass.TABLET -> 14.sp
        DeviceClass.TV -> 14.sp
    }
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicTextField(
                value = inputText,
                onValueChange = {
                    Log.d("SearchKeyboard", "onValueChange: $it")
                    inputText = it
                    // 实时回调输入内容变化
                    onInputChange(inputText)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(fieldHeight)
                    .background(AppSurfaceVariant, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { onSearch(inputText) }
                ),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "请输入搜索内容",
                                color = AppTextMuted,
                                fontSize = when (deviceClass) {
                                    DeviceClass.PHONE -> 15.sp
                                    DeviceClass.TABLET -> 14.sp
                                    DeviceClass.TV -> 12.sp
                                },
                            )
                        }
                        innerTextField()
                    }
                }
            )
            TvSurface(
                onClick = {
                    // 清空输入
                    inputText = ""
                    // 实时回调输入内容变化
                    onInputChange(inputText)
                },
                modifier = Modifier
                    .width(buttonWidth)
                    .height(actionHeight),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(2.dp)),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "清空",
                        color = AppTextPrimary,
                        fontSize = actionFontSize,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvSurface(
                onClick = {
                    onSearch(inputText)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(actionHeight),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(2.dp)),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "搜索",
                        color = AppTextPrimary,
                        fontSize = actionFontSize,
                    )
                }
            }
            TvSurface(
                onClick = {
                    if (inputText.isNotEmpty()) {
                        // 删除最后一个字符
                        inputText = inputText.dropLast(1)
                        // 实时回调输入内容变化
                        onInputChange(inputText)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(actionHeight),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(2.dp)),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "删除",
                        color = AppTextPrimary,
                        fontSize = actionFontSize,
                    )
                }
            }
        }

        if (!phoneLayout) {
            Keyboard(
                modifier = Modifier.fillMaxSize()
            ) { key ->
                inputText += key
                onInputChange(inputText)
            }
        }
    }
}
