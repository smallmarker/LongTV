package com.loong.longtv.ui.shared.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.loong.longtv.utils.extensions.provideSmartImageLoader

/**
 * @author zl
 * @Date 2025/10/15
 **/
@Composable
fun SmartAsyncImage(
    model: Any,
    modifier: Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale,
    fallbackText: String = "无封面"
) {
    val context = LocalContext.current
    val imageLoader = remember { provideSmartImageLoader(context) }
    val stringModel = (model as? String)?.trim()
    val shouldFallback = stringModel != null && stringModel.isBlank()

    if (shouldFallback) {
        Box(
            modifier = modifier.background(Color(0xFF1B1B1B)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackText,
                color = Color.LightGray,
                fontSize = 12.sp
            )
        }
        return
    }

    AsyncImage(
        model = stringModel ?: model,
        contentDescription = contentDescription,
        imageLoader = imageLoader,
        modifier = modifier,
        contentScale = contentScale
    )
}
