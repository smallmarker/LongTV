package com.loong.longtv.ui.shared.widgets

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceShape
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.theme.AppSurface
import com.loong.longtv.ui.theme.KeyboardDisabled
import com.loong.longtv.ui.theme.KeyboardFocused
import com.loong.longtv.ui.theme.KeyboardPressed
import com.loong.longtv.ui.phone.widgets.PhoneSurface
import com.loong.longtv.ui.tv.widgets.TvInteractiveSurface

/**
 * @author zl
 * @Date 2025/10/17
 **/
@Composable
fun TvSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    shape: ClickableSurfaceShape = ClickableSurfaceDefaults.shape(RoundedCornerShape(18.dp)),
    colors: ClickableSurfaceColors = ClickableSurfaceDefaults.colors(
        containerColor = AppSurface.copy(alpha = 0.86f),
        focusedContainerColor = KeyboardFocused,
        pressedContainerColor = KeyboardPressed,
        disabledContainerColor = KeyboardDisabled
    ),
    content: @Composable (BoxScope.() -> Unit)
) {
    when (currentDeviceClass()) {
        DeviceClass.PHONE -> PhoneSurface(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
            interactionSource = interactionSource,
            content = content
        )
        DeviceClass.TV -> TvInteractiveSurface(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
            interactionSource = interactionSource,
            shape = shape,
            colors = colors,
            content = content
        )
    }
}
