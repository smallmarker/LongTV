package com.loong.longtv.ui.common

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

enum class DeviceClass {
    PHONE,
    TV
}

fun Context.currentDeviceClass(): DeviceClass {
    val uiModeManager = getSystemService(UiModeManager::class.java)
    val isTvMode = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    val hasLeanbackFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    return if (isTvMode || hasLeanbackFeature) {
        DeviceClass.TV
    } else {
        DeviceClass.PHONE
    }
}

@Composable
fun currentDeviceClass(): DeviceClass {
    LocalConfiguration.current
    val context = LocalContext.current
    return context.currentDeviceClass()
}

@Composable
fun isPhoneScreen(): Boolean = currentDeviceClass() == DeviceClass.PHONE

@Composable
fun isTvScreen(): Boolean = currentDeviceClass() == DeviceClass.TV
