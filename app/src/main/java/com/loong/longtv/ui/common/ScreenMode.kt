package com.loong.longtv.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

enum class DeviceClass {
    PHONE,
    TABLET,
    TV
}

@Composable
fun currentDeviceClass(): DeviceClass {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp
    return when {
        width >= 900 -> DeviceClass.TV
        width >= 600 -> DeviceClass.TABLET
        else -> DeviceClass.PHONE
    }
}

@Composable
fun isPhoneScreen(): Boolean = currentDeviceClass() == DeviceClass.PHONE

@Composable
fun isTabletScreen(): Boolean = currentDeviceClass() == DeviceClass.TABLET

@Composable
fun isTvScreen(): Boolean = currentDeviceClass() == DeviceClass.TV
