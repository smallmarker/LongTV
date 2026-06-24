package com.loong.longtv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit

@Immutable
object AppThemeTokens {
    val ButtonRadius: Dp = 8.dp
    val CardRadius: Dp = 8.dp
    val FieldRadius: Dp = 8.dp

    val TvPageGap: Dp = 30.dp
    val PhonePageGap: Dp = 22.dp
    val TvSectionGap: Dp = 18.dp
    val PhoneSectionGap: Dp = 12.dp

    val TvSearchHeight: Dp = 36.dp
    val PhoneSearchHeight: Dp = 46.dp

    val TvPosterRailWidth: Dp = 220.dp
    val PhonePosterRailWidth: Dp = 150.dp

    val SurfaceAlpha: Float = 0.08f
    val BorderAlpha: Float = 0.18f
    val MutedBorderAlpha: Float = 0.08f

    val SearchPromptTv: TextUnit = 16.sp
    val SearchPromptPhone: TextUnit = 14.sp
    val SectionTitleTv: TextUnit = 22.sp
    val SectionTitlePhone: TextUnit = 17.sp
    val ActionTextTv: TextUnit = 15.sp
    val ActionTextPhone: TextUnit = 13.sp

    fun buttonShape() = RoundedCornerShape(ButtonRadius)
    fun cardShape() = RoundedCornerShape(CardRadius)
    fun fieldShape() = RoundedCornerShape(FieldRadius)

    fun fieldBackground() = Color.White.copy(alpha = SurfaceAlpha)
    fun fieldBorder() = Color.White.copy(alpha = BorderAlpha)
    fun mutedBorder() = Color.White.copy(alpha = MutedBorderAlpha)
}
