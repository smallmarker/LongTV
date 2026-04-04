package com.loong.longtv.ui.phone.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.loong.longtv.ui.theme.AppSurfaceVariant

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhoneSurface(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable (BoxScope.() -> Unit)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(AppSurfaceVariant.copy(alpha = 0.92f))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
                ,
                onLongClick = onLongClick
            ),
        content = content
    )
}
