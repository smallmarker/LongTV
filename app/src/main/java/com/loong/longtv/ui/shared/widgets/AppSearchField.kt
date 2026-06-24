package com.loong.longtv.ui.shared.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.theme.AppThemeTokens
import com.loong.longtv.ui.theme.BrandPrimary

@Composable
fun AppSearchFieldSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = AppThemeTokens.fieldShape(),
    content: @Composable BoxScope.() -> Unit
) {
    when (currentDeviceClass()) {
        DeviceClass.TV -> TvSurface(
            onClick = onClick,
            modifier = modifier,
            shape = ClickableSurfaceDefaults.shape(shape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                pressedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, BrandPrimary.copy(alpha = 0.95f)),
                    inset = 0.dp,
                    shape = shape
                ),
                pressedBorder = Border(
                    border = BorderStroke(2.dp, BrandPrimary),
                    inset = 0.dp,
                    shape = shape
                )
            ),
            content = content
        )
        DeviceClass.PHONE -> Box(
            modifier = modifier.clickable(onClick = onClick),
            content = content
        )
    }
}

fun Modifier.appFieldChrome(
    shape: RoundedCornerShape = AppThemeTokens.fieldShape(),
    backgroundColor: Color = AppThemeTokens.fieldBackground(),
    border: Color = AppThemeTokens.fieldBorder()
): Modifier {
    return background(color = backgroundColor, shape = shape)
        .border(1.dp, border, shape)
}
