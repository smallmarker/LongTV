package com.loong.longtv.ui.shared.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.loong.longtv.engine.source.model.SiteConfig
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.phone.widgets.PhoneChipTabs
import com.loong.longtv.ui.phone.widgets.PhoneSiteSelector
import com.loong.longtv.ui.tv.widgets.TvChipTabs
import com.loong.longtv.ui.tv.widgets.TvSiteSelector

@Composable
fun AdaptiveChipTabs(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false
) {
    when (currentDeviceClass()) {
        DeviceClass.PHONE -> PhoneChipTabs(
            items = items,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            modifier = modifier,
            singleLine = singleLine
        )
        DeviceClass.TV -> TvChipTabs(
            items = items,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            modifier = modifier
        )
    }
}

@Composable
fun AdaptiveSiteSelector(
    sites: List<SiteConfig>,
    currentSiteKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (currentDeviceClass()) {
        DeviceClass.PHONE -> PhoneSiteSelector(
            sites = sites,
            currentSiteKey = currentSiteKey,
            onSelect = onSelect,
            modifier = modifier
        )
        DeviceClass.TV -> TvSiteSelector(
            sites = sites,
            currentSiteKey = currentSiteKey,
            onSelect = onSelect,
            modifier = modifier
        )
    }
}
