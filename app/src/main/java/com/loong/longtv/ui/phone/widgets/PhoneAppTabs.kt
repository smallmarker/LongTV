package com.loong.longtv.ui.phone.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.loong.longtv.data.bean.Site
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.shared.widgets.TvSurface

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhoneChipTabs(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEachIndexed { index, item ->
            TabChip(
                text = item,
                selected = index == selectedIndex,
                compact = true,
                onClick = { onSelect(index) }
            )
        }
    }
}

@Composable
fun PhoneSiteSelector(
    sites: List<Site>,
    currentSiteKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val selectedIndex = sites.indexOfFirst { it.key == currentSiteKey }.coerceAtLeast(0)

    LaunchedEffect(selectedIndex, sites.size) {
        if (sites.isNotEmpty()) {
            listState.animateScrollToItem(selectedIndex.coerceIn(0, sites.lastIndex))
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(sites) { _, site ->
            SiteChip(
                text = site.name,
                selected = site.key == currentSiteKey,
                compact = true,
                onClick = { onSelect(site.key) }
            )
        }
    }
}

@Composable
private fun TabChip(
    text: String,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    TvSurface(onClick = onClick) {
        Text(
            text = text.ifBlank { "未命名分类" },
            modifier = Modifier
                .background(if (selected) Color(0xFF25466D) else Color.Transparent)
                .padding(
                    horizontal = if (compact) 12.dp else 14.dp,
                    vertical = if (compact) 8.dp else 10.dp
                ),
            color = AppTextPrimary,
            fontSize = if (compact) 13.sp else 15.sp
        )
    }
}

@Composable
private fun SiteChip(
    text: String,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    TvSurface(onClick = onClick) {
        Text(
            text = text,
            modifier = Modifier
                .background(if (selected) Color(0xFF25466D) else Color.Transparent)
                .padding(
                    horizontal = if (compact) 13.dp else 14.dp,
                    vertical = if (compact) 9.dp else 10.dp
                ),
            color = AppTextPrimary,
            fontSize = if (compact) 12.sp else 13.sp
        )
    }
}
