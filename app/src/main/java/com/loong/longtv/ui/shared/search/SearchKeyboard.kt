package com.loong.longtv.ui.shared.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Text
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.phone.search.PhoneSearchKeyboard
import com.loong.longtv.ui.tv.search.TvSearchKeyboard
import com.loong.longtv.ui.theme.AppSurface
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.BrandAccent
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.shared.widgets.TvSurface

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
    when (currentDeviceClass()) {
        DeviceClass.PHONE -> PhoneSearchKeyboard(
            modifier = modifier,
            onSearch = onSearch,
            onInputChange = onInputChange
        )
        DeviceClass.TV -> TvSearchKeyboard(
            modifier = modifier,
            onSearch = onSearch,
            onInputChange = onInputChange
        )
    }
}

@Composable
fun SearchKeyboardAction(
    label: String,
    modifier: Modifier,
    fontSize: TextUnit,
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
