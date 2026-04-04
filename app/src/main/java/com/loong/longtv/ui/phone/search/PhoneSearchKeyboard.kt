package com.loong.longtv.ui.phone.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loong.longtv.ui.shared.search.SearchKeyboardAction
import com.loong.longtv.ui.theme.AppSurface
import com.loong.longtv.ui.theme.AppSurfaceVariant
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.BrandAccent
import com.loong.longtv.ui.theme.BrandPrimary

@Composable
fun PhoneSearchKeyboard(
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit = {},
    onInputChange: (String) -> Unit = {}
) {
    var inputText by rememberSaveable { mutableStateOf("") }

    fun updateInput(value: String) {
        inputText = value
        onInputChange(inputText)
    }

    fun submitSearch() {
        val keyword = inputText.trim()
        if (keyword.isNotEmpty()) onSearch(keyword)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicTextField(
            value = inputText,
            onValueChange = { updateInput(it) },
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            cursorBrush = Brush.verticalGradient(colors = listOf(BrandPrimary, BrandAccent)),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { submitSearch() }
            )
        )
        SearchKeyboardAction(
            label = "清空",
            modifier = Modifier.width(76.dp).height(46.dp),
            fontSize = 15.sp,
            containerAlpha = if (inputText.isBlank()) 0.45f else 1f,
            onClick = {
                if (inputText.isNotBlank()) updateInput("")
            }
        )
    }
}
