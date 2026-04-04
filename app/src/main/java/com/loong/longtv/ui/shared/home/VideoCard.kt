package com.loong.longtv.ui.shared.home

import androidx.compose.runtime.Composable
import com.loong.longtv.data.bean.Video
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.phone.home.PhoneVideoCard
import com.loong.longtv.ui.tv.home.TvVideoCard

@Composable
fun VideoCard(video: Video) {
    when (currentDeviceClass()) {
        DeviceClass.TV -> TvVideoCard(video)
        DeviceClass.PHONE -> PhoneVideoCard(video)
    }
}
