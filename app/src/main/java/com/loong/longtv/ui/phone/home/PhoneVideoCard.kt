package com.loong.longtv.ui.phone.home

import androidx.compose.runtime.Composable
import com.loong.longtv.engine.source.model.VodInfo
import com.loong.longtv.ui.shared.home.VideoCard

@Composable
fun PhoneVideoCard(video: VodInfo) {
    VideoCard(video = video)
}
