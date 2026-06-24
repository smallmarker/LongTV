package com.loong.longtv.ui.phone.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.draw.clip
import androidx.navigation.NavController
import androidx.tv.material3.ClickableSurfaceDefaults
import com.loong.longtv.ui.shared.search.SearchViewModel
import com.loong.longtv.ui.shared.search.SearchMiniButton
import com.loong.longtv.ui.shared.search.SearchResultContent
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.shared.widgets.AppScreen
import com.loong.longtv.ui.shared.widgets.TvSurface
import com.loong.longtv.ui.theme.AppSurface

@Composable
fun SearchResultPhoneScreen(
    navigator: NavController,
    keyword: String,
    viewModel: SearchViewModel = hiltViewModel()
) {
    LaunchedEffect(keyword) {
        viewModel.searchContent(keyword)
    }
    val videoMap by viewModel.searchResult.collectAsStateWithLifecycle()
    val loading by viewModel.searchLoading.collectAsStateWithLifecycle()
    val error by viewModel.searchError.collectAsStateWithLifecycle()
    val categories = videoMap.keys.toList()
    var selectedCategory by rememberSaveable(keyword) { mutableStateOf<String?>(null) }

    LaunchedEffect(videoMap) {
        if (selectedCategory !in categories) {
            selectedCategory = categories.firstOrNull()
        }
    }

    AppScreen {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.clickable { navigator.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = AppTextPrimary
                    )
                }
                Text(
                    text = keyword,
                    color = AppTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${videoMap[selectedCategory].orEmpty().size} 条结果",
                    color = AppTextSecondary,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppSurface.copy(alpha = 0.8f))
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        SearchResultContent(
            navigator = navigator,
            keyword = keyword,
            categories = categories,
            selectedCategory = selectedCategory,
            resultCount = videoMap[selectedCategory].orEmpty().size,
            videos = videoMap[selectedCategory].orEmpty(),
            loading = loading,
            error = error,
            compact = true,
            phoneLayout = true,
            onRetry = { viewModel.searchContent(keyword, force = true) },
            onSelectCategory = { selectedCategory = it }
        )
    }
}
