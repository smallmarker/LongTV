package com.loong.longtv.nav

import androidx.compose.runtime.Composable
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass

@Composable
fun AppNavGraph() {
    when (currentDeviceClass()) {
        DeviceClass.PHONE -> PhoneNavGraph()
        DeviceClass.TV -> TvNavGraph()
    }
}
