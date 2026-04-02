package com.loong.longtv.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.loong.longtv.Screen
import com.loong.longtv.data.bean.TagGroup
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.widgets.TvSurface
import com.loong.longtv.ui.widgets.AppHero
import com.loong.longtv.ui.widgets.AppPageHeader
import com.loong.longtv.ui.widgets.AppScreen
import com.loong.longtv.ui.widgets.SectionHeader

/**
 * @author zl
 * @Date 2025/10/20
 **/
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController, viewModel: SearchViewModel = hiltViewModel()) {
    val deviceClass = currentDeviceClass()
    val compact = deviceClass != DeviceClass.TV
    val phoneLayout = deviceClass == DeviceClass.PHONE
    AppScreen {
        if (phoneLayout) {
            AppPageHeader(
                title = "搜索",
                subtitle = "手机端以直接输入和最近搜索为主，减少 TV 风格键盘干扰。"
            )
        } else {
            AppHero(
                title = "搜索与发现",
                subtitle = "输入片名、浏览搜索历史，或者从已启用站点里快速进入结果页。"
            )
        }
        if (compact) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SearchKeyboard(
                    modifier = Modifier.fillMaxWidth(),
                    onSearch = { keyword ->
                        if (keyword.isNotBlank()) {
                            navController.navigate(Screen.SearchResult.createRoute(keyword))
                        }
                    },
                    onInputChange = {}
                )

                SearchHistoryScreen(
                    modifier = Modifier.fillMaxWidth(),
                    viewModel = viewModel,
                    onHistoryClick = { keyword ->
                        navController.navigate(Screen.SearchResult.createRoute(keyword))
                    }
                )

                TagListScreen(
                    modifier = Modifier.fillMaxWidth(),
                    viewModel = viewModel,
                    compact = true,
                    phoneLayout = phoneLayout
                ) {
                    navController.navigate(Screen.SearchResult.createRoute(it))
                }
            }
        } else {
            Row(
                modifier = Modifier.padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.width(260.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    SearchKeyboard(
                        modifier = Modifier.fillMaxWidth(),
                        onSearch = { keyword ->
                            if (keyword.isNotBlank()) {
                                navController.navigate(Screen.SearchResult.createRoute(keyword))
                            }
                        },
                        onInputChange = {}
                    )
                    SearchHistoryScreen(
                        modifier = Modifier.fillMaxWidth(),
                        viewModel = viewModel,
                        onHistoryClick = { keyword ->
                            navController.navigate(Screen.SearchResult.createRoute(keyword))
                        }
                    )
                }

                TagListScreen(
                    modifier = Modifier.weight(1f),
                    viewModel = viewModel,
                    compact = false,
                    phoneLayout = false
                ) {
                    navController.navigate(Screen.SearchResult.createRoute(it))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchHistoryScreen(
    modifier: Modifier,
    viewModel: SearchViewModel,
    onHistoryClick: (String) -> Unit
) {

    val historyList = viewModel.history.collectAsState().value

    Column(modifier = modifier) {
        SectionHeader(
            title = "搜索历史",
            subtitle = "继续上次的关键词"
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            historyList.forEach { history ->
                TvSurface(
                    onClick = {
                        onHistoryClick(history.keyword)
                    }
                ) {
                    Box(
                        modifier = Modifier.padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = history.keyword,
                            color = AppTextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TagListScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel,
    compact: Boolean = false,
    phoneLayout: Boolean = false,
    onItemClick: (String) -> Unit
) {
    val groups = viewModel.searchState.collectAsState().value

    if (compact) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groups.forEach { group ->
                TagItem(group, phoneLayout = phoneLayout) { wd ->
                    onItemClick(wd)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(groups) { group ->
                TagItem(group, phoneLayout = phoneLayout) { wd ->
                    onItemClick(wd)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagItem(group: TagGroup, phoneLayout: Boolean = false, onClick: (String) -> Unit) {
    Column {
        SectionHeader(
            title = group.title,
            subtitle = if (phoneLayout) "轻触即可发起搜索" else "发现更多相关入口"
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            group.tags.forEach { tag ->
                TvSurface(
                    onClick = { onClick(tag) }
                ) {
                    Box(
                        modifier = Modifier.padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tag,
                            color = AppTextSecondary
                        )
                    }
                }
            }
        }
    }
}
