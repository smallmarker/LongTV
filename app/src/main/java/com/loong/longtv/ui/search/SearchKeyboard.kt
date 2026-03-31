package com.loong.longtv.ui.search

import android.util.Log
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
import com.loong.longtv.ui.common.isCompactScreen
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
    val compact = isCompactScreen()
    val fieldHeight = if (compact) 44.dp else 24.dp
    val actionHeight = if (compact) 44.dp else 24.dp
    val buttonWidth = if (compact) 72.dp else 50.dp
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
                    .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp),
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
                                color = Color.White,
                                fontSize = if (compact) 14.sp else 12.sp,
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
                        color = Color.White,
                        fontSize = if (compact) 15.sp else 14.sp,
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
                        color = Color.White,
                        fontSize = if (compact) 15.sp else 14.sp,
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
                        color = Color.White,
                        fontSize = if (compact) 15.sp else 14.sp,
                    )
                }
            }
        }

        Keyboard(
            modifier = Modifier.fillMaxSize()
        ) { key ->
            inputText += key
            // 实时回调输入内容变化
            onInputChange(inputText)
        }
    }
}
