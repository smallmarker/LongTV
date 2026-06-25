package com.loong.longtv.ui.phone.widgets

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.loong.longtv.ui.theme.AppCardOverlay
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.BrandPrimary

@Composable
fun PhoneAppPageScaffold(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    AppPageScaffoldBase(
        modifier = modifier,
        horizontalPadding = 16.dp,
        verticalPadding = 16.dp,
        content = content
    )
}

@Composable
fun PhoneAppPageHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    AppPageHeaderBase(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        titleSize = 22.sp,
        horizontalPadding = 18.dp,
        verticalPadding = 16.dp,
        subtitleFontSize = 13.sp,
        subtitleLineHeight = 18.sp,
        subtitleMaxLines = 3,
        trailing = trailing
    )
}

@Composable
private fun AppPageScaffoldBase(
    modifier: Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    verticalPadding: androidx.compose.ui.unit.Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            content = content
        )
    }
}

@Composable
private fun AppPageHeaderBase(
    title: String,
    subtitle: String,
    modifier: Modifier,
    titleSize: androidx.compose.ui.unit.TextUnit,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    verticalPadding: androidx.compose.ui.unit.Dp,
    subtitleFontSize: androidx.compose.ui.unit.TextUnit,
    subtitleLineHeight: androidx.compose.ui.unit.TextUnit,
    subtitleMaxLines: Int,
    trailing: (@Composable () -> Unit)?
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = AppCardOverlay,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
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
                maxLines = subtitleMaxLines,
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
