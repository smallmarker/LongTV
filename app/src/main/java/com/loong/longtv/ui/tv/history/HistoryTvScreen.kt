package com.loong.longtv.ui.tv.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.loong.longtv.data.room.entity.WatchHistoryEntity
import com.loong.longtv.nav.Screen
import com.loong.longtv.ui.shared.VideoPosterAspectRatio
import com.loong.longtv.ui.shared.history.HistoryViewModel
import com.loong.longtv.ui.shared.widgets.AppButton
import com.loong.longtv.ui.shared.widgets.AppScreen
import com.loong.longtv.ui.shared.widgets.SmartAsyncImage
import com.loong.longtv.ui.shared.widgets.TvSurface
import com.loong.longtv.ui.theme.AppBackgroundElevated
import com.loong.longtv.ui.theme.AppCardOverlay
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppSurface
import com.loong.longtv.ui.theme.AppSurfaceVariant
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.AppThemeTokens
import com.loong.longtv.ui.theme.BrandAccent
import com.loong.longtv.ui.theme.BrandPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryTvScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val histories by viewModel.histories.collectAsStateWithLifecycle()
    var editMode by remember { mutableStateOf(false) }
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }
    var requestBatchFocus by remember { mutableStateOf(false) }
    val sections = remember(histories) { histories.toTvHistorySections() }
    val batchPrimaryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(histories, editMode) {
        val validKeys = histories.mapTo(linkedSetOf()) { it.historyKey }
        selectedKeys = selectedKeys.filterTo(linkedSetOf()) { it in validKeys }
        if (!editMode) {
            selectedKeys = emptySet()
        }
        if (histories.isEmpty()) {
            editMode = false
        }
    }

    LaunchedEffect(editMode, histories.isNotEmpty()) {
        if (editMode && histories.isNotEmpty() && requestBatchFocus) {
            batchPrimaryFocusRequester.requestFocus()
            requestBatchFocus = false
        }
    }

    AppScreen {
        TvHistoryTopBar(
            editMode = editMode,
            canEdit = histories.isNotEmpty(),
            onBack = { navController.popBackStack() },
            onToggleEdit = {
                requestBatchFocus = !editMode
                editMode = !editMode
                if (!editMode) {
                    selectedKeys = emptySet()
                }
            }
        )

        if (editMode && histories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            TvHistoryBatchBar(
                primaryFocusRequester = batchPrimaryFocusRequester,
                allSelected = selectedKeys.size == histories.size,
                selectedCount = selectedKeys.size,
                onToggleSelectAll = {
                    selectedKeys = if (selectedKeys.size == histories.size) emptySet() else histories.mapTo(linkedSetOf()) { it.historyKey }
                },
                onDelete = {
                    viewModel.deleteHistories(selectedKeys.toList())
                    selectedKeys = emptySet()
                    editMode = false
                }
            )
        }

        if (histories.isEmpty()) {
            TvHistoryEmptyState()
        } else {
            Spacer(modifier = Modifier.height(28.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(sections, key = { it.key }) { section ->
                    TvHistorySection(
                        section = section,
                        editMode = editMode,
                        selectedKeys = selectedKeys,
                        onToggleSelected = { historyKey ->
                            selectedKeys = if (historyKey in selectedKeys) {
                                selectedKeys - historyKey
                            } else {
                                selectedKeys + historyKey
                            }
                        },
                        onOpen = { item ->
                            navController.navigate(Screen.VideoDetail.createRoute(item.siteKey, item.videoId))
                        },
                        onEnterEditMode = { historyKey ->
                            requestBatchFocus = false
                            editMode = true
                            selectedKeys = setOf(historyKey)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvHistoryTopBar(
    editMode: Boolean,
    canEdit: Boolean,
    onBack: () -> Unit,
    onToggleEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TvSurface(
            onClick = onBack,
            modifier = Modifier
                .width(116.dp)
            ,
            colors = ClickableSurfaceDefaults.colors(
                containerColor = AppCardOverlay.copy(alpha = 0.86f),
                focusedContainerColor = BrandPrimary.copy(alpha = 0.16f),
                pressedContainerColor = BrandPrimary.copy(alpha = 0.10f),
                disabledContainerColor = AppSurface.copy(alpha = 0.45f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = AppThemeTokens.buttonShape()
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = AppTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "返回",
                        color = AppTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "观看历史",
            color = AppTextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.width(116.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TvTopActionChip(
                text = if (editMode) "完成" else "编辑",
                enabled = canEdit,
                prominent = editMode,
                onClick = onToggleEdit
            )
        }
    }
}

@Composable
private fun TvHistoryBatchBar(
    primaryFocusRequester: FocusRequester,
    allSelected: Boolean,
    selectedCount: Int,
    onToggleSelectAll: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), AppThemeTokens.cardShape())
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = AppThemeTokens.cardShape()
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TvMiniActionChip(
            text = if (allSelected) "取消全选" else "全选",
            onClick = onToggleSelectAll,
            modifier = Modifier.focusRequester(primaryFocusRequester)
        )
        TvMiniActionChip(
            text = if (selectedCount > 0) "删除($selectedCount)" else "删除",
            onClick = onDelete,
            enabled = selectedCount > 0,
            warning = selectedCount > 0
        )
    }
}

@Composable
private fun TvHistorySection(
    section: TvHistorySectionData,
    editMode: Boolean,
    selectedKeys: Set<String>,
    onToggleSelected: (String) -> Unit,
    onOpen: (WatchHistoryEntity) -> Unit,
    onEnterEditMode: (String) -> Unit
) {
    var currentPage by remember(section.key, section.items) { mutableStateOf(0) }
    val pages = remember(section.items) { section.items.chunked(TV_PAGE_SIZE) }
    val visibleItems = pages.getOrElse(currentPage) { emptyList() }

    LaunchedEffect(pages.size) {
        currentPage = currentPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.05f), AppThemeTokens.cardShape())
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = AppThemeTokens.cardShape()
                    )
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "${section.emoji} ${section.title}",
                    color = AppTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            visibleItems.chunked(TV_COLUMNS).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    rowItems.forEach { item ->
                        TvHistoryPosterCard(
                            item = item,
                            editMode = editMode,
                            selected = item.historyKey in selectedKeys,
                            onToggleSelected = { onToggleSelected(item.historyKey) },
                            onOpen = { onOpen(item) },
                            onLongPress = { onEnterEditMode(item.historyKey) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(TV_COLUMNS - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        TvHistoryPagination(
            currentPage = currentPage,
            pageCount = pages.size,
            onPrevious = { currentPage = (currentPage - 1).coerceAtLeast(0) },
            onNext = { currentPage = (currentPage + 1).coerceAtMost(pages.size - 1) }
        )
    }
}

@Composable
private fun TvHistoryPosterCard(
    item: WatchHistoryEntity,
    editMode: Boolean,
    selected: Boolean,
    onToggleSelected: () -> Unit,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = tvHistoryProgress(item)
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember(item.historyKey) { mutableStateOf(false) }

    TvSurface(
        onClick = if (editMode) onToggleSelected else onOpen,
        onLongClick = if (editMode) null else onLongPress,
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer {
                scaleX = if (focused) 1.02f else 1f
                scaleY = if (focused) 1.02f else 1f
            },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = BrandPrimary.copy(alpha = 0.18f),
            pressedContainerColor = BrandPrimary.copy(alpha = 0.12f),
            disabledContainerColor = AppSurface.copy(alpha = 0.45f)
        ),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            when {
                                editMode && selected -> BrandPrimary.copy(alpha = 0.20f)
                                editMode -> AppSurface.copy(alpha = 0.82f)
                                selected || focused -> BrandPrimary.copy(alpha = 0.22f)
                                else -> AppSurfaceVariant.copy(alpha = 0.96f)
                            },
                            when {
                                editMode && selected -> AppSurface.copy(alpha = 0.96f)
                                editMode -> AppSurface.copy(alpha = 0.88f)
                                focused -> AppSurfaceVariant.copy(alpha = 0.98f)
                                else -> AppSurface.copy(alpha = 0.94f)
                            }
                        )
                    ),
                    shape = AppThemeTokens.cardShape()
                )
                .border(
                    width = 1.dp,
                    color = when {
                        selected -> BrandPrimary.copy(alpha = 0.56f)
                        focused -> BrandPrimary.copy(alpha = 0.42f)
                        editMode -> Color.White.copy(alpha = 0.04f)
                        else -> Color.White.copy(alpha = 0.07f)
                    },
                    shape = AppThemeTokens.cardShape()
                )
                .padding(horizontal = 0.dp, vertical = 0.dp)
        ) {
            SmartAsyncImage(
                model = item.cover,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(VideoPosterAspectRatio)
                    .clip(AppThemeTokens.cardShape()),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                fallbackText = item.title.ifBlank { "历史记录" }
            )

            if (editMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    TvSelectionBadge(selected = selected, onClick = onToggleSelected)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.72f),
                                Color.Black.copy(alpha = 0.86f)
                            )
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                Text(
                    text = item.title.ifBlank { "未命名内容" },
                    color = if (focused && !editMode) BrandPrimary else AppTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.episodeName.ifBlank { "继续观看" },
                    color = AppTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )

                if (!editMode) {
                    Spacer(modifier = Modifier.height(7.dp))
                    TvHistoryProgressBar(progress = progress)
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}% 继续▶",
                            color = BrandPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatTvCardDate(item.updateTime),
                            color = if (focused) AppTextSecondary else AppTextMuted,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selected) "已选择" else "待选择",
                        color = if (selected) BrandPrimary else AppTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TvSelectionBadge(
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(
                color = if (selected) BrandPrimary else Color.Transparent,
                shape = CircleShape
            )
            .border(
                width = 1.5.dp,
                color = if (selected) BrandPrimary else AppTextMuted.copy(alpha = 0.7f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (selected) "✓" else "",
            color = AppBackgroundElevated,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TvHistoryProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .background(
                    Brush.horizontalGradient(listOf(BrandPrimary, BrandAccent)),
                    RoundedCornerShape(999.dp)
                )
        )
    }
}

@Composable
private fun TvHistoryPagination(
    currentPage: Int,
    pageCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    if (pageCount <= 1) return

    Column(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .background(Color.White.copy(alpha = 0.06f), AppThemeTokens.cardShape())
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = AppThemeTokens.cardShape()
            )
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "第 ${currentPage + 1} / $pageCount 页",
            color = AppTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AppButton(
                text = if (currentPage > 0) "◀ 上一页" else "上一页",
                onClick = onPrevious,
                enabled = currentPage > 0
            )
            AppButton(
                text = if (currentPage < pageCount - 1) "下一页 ▶" else "下一页",
                onClick = onNext,
                enabled = currentPage < pageCount - 1
            )
        }
    }
}

@Composable
private fun TvHistoryEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .background(Color.White.copy(alpha = 0.06f), AppThemeTokens.cardShape())
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = AppThemeTokens.cardShape()
                )
                .padding(horizontal = 28.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "暂无观看历史",
                color = AppTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "开始播放后，这里会以时间分组的方式整理你的续播记录。",
                color = AppTextMuted,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun TvTopActionChip(
    text: String,
    enabled: Boolean,
    prominent: Boolean,
    onClick: () -> Unit
) {
    TvSurface(
        onClick = onClick,
        modifier = Modifier,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (!enabled) AppSurface.copy(alpha = 0.45f)
            else if (prominent) BrandPrimary.copy(alpha = 0.18f)
            else AppCardOverlay,
            focusedContainerColor = BrandPrimary.copy(alpha = 0.16f),
            pressedContainerColor = BrandPrimary.copy(alpha = 0.10f),
            disabledContainerColor = AppSurface.copy(alpha = 0.45f)
        )
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = if (prominent) BrandPrimary.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.08f),
                    shape = AppThemeTokens.buttonShape()
                )
                .padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (!enabled) AppTextMuted else if (prominent) BrandPrimary else AppTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TvMiniActionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    warning: Boolean = false
) {
    TvSurface(
        onClick = onClick,
        modifier = modifier,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (!enabled) AppSurface.copy(alpha = 0.45f)
            else if (warning) AppError.copy(alpha = 0.14f)
            else AppCardOverlay,
            focusedContainerColor = if (warning) AppError.copy(alpha = 0.20f) else BrandPrimary.copy(alpha = 0.16f),
            pressedContainerColor = if (warning) AppError.copy(alpha = 0.14f) else BrandPrimary.copy(alpha = 0.10f),
            disabledContainerColor = AppSurface.copy(alpha = 0.45f)
        )
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = when {
                        !enabled -> Color.White.copy(alpha = 0.04f)
                        warning -> AppError.copy(alpha = 0.28f)
                        else -> Color.White.copy(alpha = 0.08f)
                    },
                    shape = AppThemeTokens.buttonShape()
                )
                .padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = when {
                    !enabled -> AppTextMuted
                    warning -> AppError
                    else -> AppTextPrimary
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private data class TvHistorySectionData(
    val key: String,
    val title: String,
    val emoji: String,
    val items: List<WatchHistoryEntity>
)

private fun List<WatchHistoryEntity>.toTvHistorySections(nowMillis: Long = System.currentTimeMillis()): List<TvHistorySectionData> {
    val todayStart = startOfDayMillis(nowMillis)
    val yesterdayStart = todayStart - ONE_DAY_MS
    val grouped = linkedMapOf<String, MutableList<WatchHistoryEntity>>()

    for (item in this) {
        val key = when {
            item.updateTime >= todayStart -> "today"
            item.updateTime >= yesterdayStart -> "yesterday"
            else -> "earlier"
        }
        grouped.getOrPut(key) { mutableListOf() }.add(item)
    }

    return grouped.map { (key, items) ->
        when (key) {
            "today" -> TvHistorySectionData(key = key, title = "今日", emoji = "🔥", items = items)
            "yesterday" -> TvHistorySectionData(key = key, title = "昨日", emoji = "⏱", items = items)
            else -> TvHistorySectionData(key = key, title = "更早", emoji = "📅", items = items)
        }
    }
}

private fun tvHistoryProgress(item: WatchHistoryEntity): Float {
    return if (item.durationMs > 0) {
        (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
}

private fun formatTvCardDate(updateTime: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val todayStart = startOfDayMillis(nowMillis)
    val yesterdayStart = todayStart - ONE_DAY_MS
    return when {
        updateTime >= todayStart -> "今天${SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(updateTime))}"
        updateTime >= yesterdayStart -> "昨天${SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(updateTime))}"
        else -> SimpleDateFormat("M月d日", Locale.CHINA).format(Date(updateTime))
    }
}

private fun startOfDayMillis(timeMillis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
private const val TV_COLUMNS = 5
private const val TV_PAGE_SIZE = 10
