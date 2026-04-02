package com.loong.longtv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LongTVTheme(
    isInDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (isInDarkTheme) {
        darkColorScheme(
            primary = BrandPrimary,
            secondary = BrandSecondary,
            tertiary = BrandAccent,
            background = AppBackground,
            surface = AppSurface,
            surfaceVariant = AppSurfaceVariant,
            onPrimary = AppBackground,
            onSecondary = AppTextPrimary,
            onTertiary = AppTextPrimary,
            onBackground = AppTextPrimary,
            onSurface = AppTextPrimary,
            onSurfaceVariant = AppTextSecondary,
            error = AppError
        )
    } else {
        lightColorScheme(
            primary = BrandPrimaryDark,
            secondary = BrandSecondary,
            tertiary = BrandAccent,
            background = AppBackground,
            surface = AppSurface,
            surfaceVariant = AppSurfaceVariant,
            onPrimary = AppBackground,
            onSecondary = AppTextPrimary,
            onTertiary = AppTextPrimary,
            onBackground = AppTextPrimary,
            onSurface = AppTextPrimary,
            onSurfaceVariant = AppTextSecondary,
            error = AppError
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
