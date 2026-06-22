package com.loong.longtv.ui.adaptive

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass

private data class AutoSizeSpec(
    val designWidthDp: Float,
    val designHeightDp: Float,
    val scaleByHeight: Boolean,
    val minScale: Float,
    val maxScale: Float
)

@Composable
fun AutoSizeLayout(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val deviceClass = currentDeviceClass()
    val currentDensity = LocalDensity.current
    val spec = remember(deviceClass, configuration.orientation) {
        autoSizeSpec(deviceClass, configuration.orientation)
    }
    val scale = remember(
        spec,
        configuration.screenWidthDp,
        configuration.screenHeightDp
    ) {
        calculateScale(
            configuration = configuration,
            spec = spec
        )
    }
    val adaptedDensity = remember(currentDensity, scale) {
        Density(
            density = currentDensity.density * scale,
            fontScale = currentDensity.fontScale
        )
    }

    CompositionLocalProvider(LocalDensity provides adaptedDensity) {
        content()
    }
}

private fun autoSizeSpec(
    deviceClass: DeviceClass,
    orientation: Int
): AutoSizeSpec {
    return when (deviceClass) {
        DeviceClass.PHONE -> AutoSizeSpec(
            designWidthDp = 360f,
            designHeightDp = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 360f else 760f,
            scaleByHeight = false,
            minScale = 0.86f,
            maxScale = 1.2f
        )

        DeviceClass.TV -> AutoSizeSpec(
            designWidthDp = 960f,
            designHeightDp = 540f,
            scaleByHeight = false,
            minScale = 0.9f,
            maxScale = 1.35f
        )
    }
}

private fun calculateScale(
    configuration: Configuration,
    spec: AutoSizeSpec
): Float {
    val screenDp = if (spec.scaleByHeight) {
        configuration.screenHeightDp.takeIf { it > 0 }?.toFloat()
    } else {
        configuration.screenWidthDp.takeIf { it > 0 }?.toFloat()
    } ?: return 1f
    val designDp = if (spec.scaleByHeight) spec.designHeightDp else spec.designWidthDp

    return (screenDp / designDp).coerceIn(spec.minScale, spec.maxScale)
}
