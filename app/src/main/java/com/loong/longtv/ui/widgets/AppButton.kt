package com.loong.longtv.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.theme.AppSurfaceVariant
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.theme.BrandPrimaryDark

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    when (currentDeviceClass()) {
        DeviceClass.PHONE, DeviceClass.TABLET -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (enabled) BrandPrimary else AppSurfaceVariant)
                    .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = if (enabled) Color(0xFF0E1626) else AppTextPrimary.copy(alpha = 0.45f),
                    fontSize = 14.sp
                )
            }
        }

        DeviceClass.TV -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = ButtonDefaults.shape(RoundedCornerShape(14.dp)),
                colors = ButtonDefaults.colors(
                    containerColor = BrandPrimary,
                    contentColor = Color(0xFF0E1626),
                    focusedContainerColor = BrandPrimaryDark,
                    focusedContentColor = Color(0xFF0E1626),
                    disabledContainerColor = AppSurfaceVariant,
                    disabledContentColor = AppTextPrimary.copy(alpha = 0.45f)
                )
            ) {
                Text(text)
            }
        }
    }
}
