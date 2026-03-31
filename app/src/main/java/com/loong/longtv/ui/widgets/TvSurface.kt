package com.loong.longtv.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceShape
import androidx.tv.material3.Surface
import com.loong.longtv.ui.common.isCompactScreen
import com.loong.longtv.ui.theme.KeyboardDefault
import com.loong.longtv.ui.theme.KeyboardDisabled
import com.loong.longtv.ui.theme.KeyboardFocused
import com.loong.longtv.ui.theme.KeyboardPressed

/**
 * @author zl
 * @Date 2025/10/17
 **/
@Composable
fun TvSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: ClickableSurfaceShape = ClickableSurfaceDefaults.shape(RoundedCornerShape(2.dp)),
    colors: ClickableSurfaceColors = ClickableSurfaceDefaults.colors(
        containerColor = KeyboardDefault,
        focusedContainerColor = KeyboardFocused,
        pressedContainerColor = KeyboardPressed,
        disabledContainerColor = KeyboardDisabled
    ),
    content: @Composable (BoxScope.() -> Unit)
) {
    val compact = isCompactScreen()
    if (compact) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(KeyboardDefault)
                .clickable(onClick = onClick),
            content = content
        )
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            content = content
        )
    }
}
