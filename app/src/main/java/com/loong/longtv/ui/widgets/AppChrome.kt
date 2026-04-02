package com.loong.longtv.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.theme.AppBackground
import com.loong.longtv.ui.theme.AppBackgroundElevated
import com.loong.longtv.ui.theme.AppCardOverlay
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.BrandAccent
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.theme.BrandSecondary

@Composable
fun AppPageScaffold(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val deviceClass = currentDeviceClass()
    val horizontalPadding = when (deviceClass) {
        DeviceClass.PHONE -> 16.dp
        DeviceClass.TABLET -> 20.dp
        DeviceClass.TV -> 24.dp
    }
    val verticalPadding = when (deviceClass) {
        DeviceClass.PHONE -> 16.dp
        DeviceClass.TABLET -> 18.dp
        DeviceClass.TV -> 20.dp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppBackground,
                        AppBackgroundElevated,
                        AppBackground
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BrandSecondary.copy(alpha = 0.24f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            BrandAccent.copy(alpha = 0.16f),
                            Color.Transparent,
                            BrandPrimary.copy(alpha = 0.12f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            content = content
        )
    }
}

@Composable
fun AppScreen(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    AppPageScaffold(modifier = modifier, content = content)
}

@Composable
fun AppPageHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val deviceClass = currentDeviceClass()
    val titleSize = when (deviceClass) {
        DeviceClass.PHONE -> 22.sp
        DeviceClass.TABLET -> 26.sp
        DeviceClass.TV -> 28.sp
    }
    val horizontalPadding = when (deviceClass) {
        DeviceClass.PHONE -> 18.dp
        DeviceClass.TABLET -> 22.dp
        DeviceClass.TV -> 22.dp
    }
    val verticalPadding = when (deviceClass) {
        DeviceClass.PHONE -> 16.dp
        DeviceClass.TABLET -> 18.dp
        DeviceClass.TV -> 18.dp
    }
    val subtitleFontSize = when (deviceClass) {
        DeviceClass.PHONE -> 13.sp
        DeviceClass.TABLET -> 14.sp
        DeviceClass.TV -> 14.sp
    }
    val subtitleLineHeight = when (deviceClass) {
        DeviceClass.PHONE -> 18.sp
        DeviceClass.TABLET -> 20.sp
        DeviceClass.TV -> 20.sp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = AppCardOverlay,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(BrandPrimary, RoundedCornerShape(999.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LongTV",
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = title,
                color = AppTextPrimary,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = subtitle,
                color = AppTextSecondary,
                fontSize = subtitleFontSize,
                lineHeight = subtitleLineHeight,
                maxLines = if (deviceClass == DeviceClass.PHONE) 3 else 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(20.dp))
            trailing()
        }
    }
}

@Composable
fun AppHero(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    AppPageHeader(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        trailing = trailing
    )
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = AppTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = AppTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        if (action != null) action()
    }
}

@Composable
fun LoadingState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = AppTextPrimary,
            fontSize = 16.sp
        )
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = AppTextMuted,
            fontSize = 16.sp
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(12.dp))
            action()
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = AppError,
            fontSize = 16.sp
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(12.dp))
            action()
        }
    }
}
