package com.loong.longtv.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Text
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.theme.AppSurface
import com.loong.longtv.ui.theme.AppSurfaceVariant
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.BrandAccent
import com.loong.longtv.ui.theme.BrandPrimary
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
    val keyboardTitle = when (deviceClass) {
        DeviceClass.PHONE -> "搜索内容"
        DeviceClass.TABLET -> "输入关键词"
        DeviceClass.TV -> "搜索面板"
    }
    val helperText = if (phoneLayout) {
        "支持片名、演员、导演等关键词，回车即可搜索。"
    } else {
        "可使用系统输入法或下方字母键盘，输入后立即跳转到结果页。"
    }
    var inputText by rememberSaveable { mutableStateOf("") }

    fun updateInput(value: String) {
        inputText = value
        onInputChange(inputText)
    }

    fun submitSearch() {
        val keyword = inputText.trim()
        if (keyword.isNotEmpty()) {
            onSearch(keyword)
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = keyboardTitle,
                color = AppTextPrimary,
                fontSize = if (phoneLayout) 16.sp else 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = helperText,
                color = AppTextMuted,
                fontSize = if (phoneLayout) 12.sp else 11.sp,
                lineHeight = if (phoneLayout) 18.sp else 16.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicTextField(
                value = inputText,
                onValueChange = {
                    updateInput(it)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(fieldHeight)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                AppSurfaceVariant,
                                AppSurface.copy(alpha = 0.98f)
                            )
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp,
                        if (inputText.isBlank()) Color.White.copy(alpha = 0.08f) else BrandPrimary.copy(alpha = 0.32f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp),
                textStyle = TextStyle(
                    color = AppTextPrimary,
                    fontSize = when (deviceClass) {
                        DeviceClass.PHONE -> 16.sp
                        DeviceClass.TABLET -> 15.sp
                        DeviceClass.TV -> 13.sp
                    },
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = Brush.verticalGradient(
                    colors = listOf(BrandPrimary, BrandAccent)
                ),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { submitSearch() }
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (inputText.isBlank()) {
                            Text(
                                text = if (phoneLayout) "例如：流浪地球 / 周星驰 / 悬疑" else "输入片名、演员、导演或关键词",
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
            SearchKeyboardAction(
                label = "清空",
                modifier = Modifier
                    .width(buttonWidth)
                    .height(actionHeight),
                fontSize = actionFontSize,
                containerAlpha = if (inputText.isBlank()) 0.45f else 1f,
                onClick = {
                    if (inputText.isNotBlank()) {
                        updateInput("")
                    }
                }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchKeyboardAction(
                label = if (phoneLayout) "立即搜索" else "搜索",
                modifier = Modifier
                    .weight(1f)
                    .height(actionHeight),
                fontSize = actionFontSize,
                accent = true,
                containerAlpha = if (inputText.isBlank()) 0.6f else 1f,
                onClick = { submitSearch() }
            )
            SearchKeyboardAction(
                label = "删除",
                modifier = Modifier
                    .weight(1f)
                    .height(actionHeight),
                fontSize = actionFontSize,
                containerAlpha = if (inputText.isBlank()) 0.45f else 1f,
                onClick = {
                    if (inputText.isNotEmpty()) {
                        updateInput(inputText.dropLast(1))
                    }
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (inputText.isBlank()) "尚未输入关键词" else "已输入 ${inputText.length} 个字符",
                color = if (inputText.isBlank()) AppTextMuted else AppTextSecondary,
                fontSize = 12.sp
            )
            if (inputText.isNotBlank()) {
                Text(
                    text = "当前内容：$inputText",
                    color = AppTextMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }

        if (!phoneLayout) {
            Keyboard(
                modifier = Modifier.fillMaxWidth()
            ) { key ->
                updateInput(inputText + key)
            }
        }
    }
}

@Composable
private fun SearchKeyboardAction(
    label: String,
    modifier: Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit,
    accent: Boolean = false,
    containerAlpha: Float = 1f
) {
    TvSurface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (accent) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                BrandPrimary.copy(alpha = 0.30f * containerAlpha),
                                BrandAccent.copy(alpha = 0.18f * containerAlpha)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                AppSurface.copy(alpha = 0.25f * containerAlpha),
                                Color.Transparent
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = AppTextPrimary.copy(alpha = containerAlpha.coerceIn(0.45f, 1f)),
                fontSize = fontSize,
                fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}
