package com.loong.longtv.ui.shared.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.phone.widgets.PhoneAppButton
import com.loong.longtv.ui.tv.widgets.TvAppButton

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    when (currentDeviceClass()) {
        DeviceClass.PHONE -> PhoneAppButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled)
        DeviceClass.TV -> TvAppButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled)
    }
}
