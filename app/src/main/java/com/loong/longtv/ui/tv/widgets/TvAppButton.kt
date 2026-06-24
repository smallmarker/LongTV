package com.loong.longtv.ui.tv.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.loong.longtv.ui.theme.AppSurfaceVariant
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppThemeTokens
import com.loong.longtv.ui.theme.BrandPrimary
import com.loong.longtv.ui.theme.BrandPrimaryDark

@Composable
fun TvAppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ButtonDefaults.shape(AppThemeTokens.buttonShape()),
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
