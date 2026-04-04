package com.loong.longtv.ui.phone.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.loong.longtv.data.room.entity.WatchHistoryEntity
import com.loong.longtv.nav.Screen
import com.loong.longtv.ui.shared.history.HistoryViewModel
import com.loong.longtv.ui.shared.history.formatMs
import com.loong.longtv.ui.shared.widgets.AppScreen
import com.loong.longtv.ui.shared.widgets.SmartAsyncImage
import com.loong.longtv.ui.theme.AppBackgroundElevated
import com.loong.longtv.ui.theme.AppCardOverlay
import com.loong.longtv.ui.theme.AppError
import com.loong.longtv.ui.theme.AppSurface
import com.loong.longtv.ui.theme.AppSurfaceVariant
import com.loong.longtv.ui.theme.AppTextMuted
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.AppTextSecondary
import com.loong.longtv.ui.theme.BrandAccent
import com.loong.longtv.ui.theme.BrandPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryPhoneScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val histories by viewModel.histories.collectAsStateWithLifecycle()
    var editMode by remember { mutableStateOf(false) }
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }
    val sections = remember(histories) { histories.toPhoneHistorySections() }

    LaunchedEffect(histories, editMode) {
        val validKeys = histories.mapTo(linkedSetOf()) { it.historyKey }
        selectedKeys = selectedKeys.filterTo(linkedSetOf()) { it in validKeys }
        if (histories.isEmpty()) {
            editMode = false
        }
        if (!editMode) {
            selectedKeys = emptySet()
        }
    }

    AppScreen {
        HistoryPhoneTopBar(
            editMode = editMode,
            canEdit = histories.isNotEmpty(),
            onBack = { navController.popBackStack() },
            onToggleEdit = {
                editMode = !editMode
                if (!editMode) {
                    selectedKeys = emptySet()
                }
            }
        )

        if (editMode && histories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HistoryPhoneBatchBar(
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
            HistoryPhoneEmptyState()
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(sections, key = { it.key }) { section ->
                    HistoryPhoneSection(
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
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    HistoryPhoneFooter(totalCount = histories.size)
                }
            }
        }
    }
}

@Composable
private fun HistoryPhoneTopBar(
    editMode: Boolean,
    canEdit: Boolean,
    onBack: () -> Unit,
    onToggleEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onBack)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = AppTextPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "返回", color = AppTextSecondary, fontSize = 13.sp)
        }
        Text(
            text = "观看历史",
            color = AppTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        HistoryTopActionChip(
            text = if (editMode) "完成" else "编辑",
            enabled = canEdit,
            prominent = editMode,
            onClick = onToggleEdit
        )
    }
}

@Composable
private fun HistoryPhoneBatchBar(
    allSelected: Boolean,
    selectedCount: Int,
    onToggleSelectAll: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(AppCardOverlay.copy(alpha = 0.96f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (allSelected) "取消全选" else "全选",
            color = AppTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onToggleSelectAll)
        )
        Text(
            text = if (selectedCount > 0) "删除($selectedCount)" else "删除",
            color = if (selectedCount > 0) AppError else AppTextMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(enabled = selectedCount > 0, onClick = onDelete)
        )
    }
}

@Composable
private fun HistoryPhoneSection(
    section: PhoneHistorySection,
    editMode: Boolean,
    selectedKeys: Set<String>,
    onToggleSelected: (String) -> Unit,
    onOpen: (WatchHistoryEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = section.emoji,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = section.title,
                    color = AppTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = section.dateLabel,
                color = AppTextMuted,
                fontSize = 12.sp
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            section.items.forEach { item ->
                HistoryPhoneCard(
                    item = item,
                    selected = item.historyKey in selectedKeys,
                    editMode = editMode,
                    onToggleSelected = { onToggleSelected(item.historyKey) },
                    onOpen = { onOpen(item) }
                )
            }
        }
    }
}

@Composable
private fun HistoryPhoneCard(
    item: WatchHistoryEntity,
    selected: Boolean,
    editMode: Boolean,
    onToggleSelected: () -> Unit,
    onOpen: () -> Unit
) {
    val progress = historyProgress(item)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (selected) BrandPrimary.copy(alpha = 0.22f) else AppSurfaceVariant.copy(alpha = 0.96f),
                        if (selected) AppSurface.copy(alpha = 0.96f) else AppSurface.copy(alpha = 0.92f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = if (selected) BrandPrimary.copy(alpha = 0.54f) else Color.White.copy(alpha = 0.07f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = if (editMode) onToggleSelected else onOpen)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (editMode) {
            HistorySelectionDot(
                selected = selected,
                onClick = onToggleSelected
            )
        }

        SmartAsyncImage(
            model = item.cover,
            contentDescription = item.title,
            modifier = Modifier
                .width(84.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            fallbackText = item.title.ifBlank { "历史记录" }
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildHistoryTitle(item),
                        color = AppTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "观看至 ${formatMs(item.positionMs)} / ${historyDurationLabel(item)}",
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (!editMode) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(BrandPrimary.copy(alpha = 0.18f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "继续播放",
                            color = BrandPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HistoryProgressBar(progress = progress)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = historyUpdateLabel(item.updateTime),
                    color = AppTextMuted,
                    fontSize = 12.sp
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = BrandPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun HistorySelectionDot(
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) BrandPrimary else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (selected) BrandPrimary else AppTextMuted.copy(alpha = 0.7f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(AppBackgroundElevated, CircleShape)
            )
        }
    }
}

@Composable
private fun HistoryProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(BrandPrimary, BrandAccent)
                    )
                )
        )
    }
}

@Composable
private fun HistoryPhoneEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AppCardOverlay)
        )
        Text(
            text = "还没有观看历史",
            color = AppTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            text = "开始播放后，这里会按时间整理你的继续观看记录。",
            color = AppTextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(0.76f)
        )
    }
}

@Composable
private fun HistoryPhoneFooter(totalCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppCardOverlay.copy(alpha = 0.72f))
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "已展示最近 $totalCount 条历史记录",
            color = AppTextMuted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun HistoryTopActionChip(
    text: String,
    enabled: Boolean,
    prominent: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (!enabled) AppSurface.copy(alpha = 0.45f)
                else if (prominent) BrandPrimary.copy(alpha = 0.18f)
                else AppCardOverlay
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (!enabled) AppTextMuted else if (prominent) BrandPrimary else AppTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private data class PhoneHistorySection(
    val key: String,
    val title: String,
    val emoji: String,
    val dateLabel: String,
    val items: List<WatchHistoryEntity>
)

private fun List<WatchHistoryEntity>.toPhoneHistorySections(nowMillis: Long = System.currentTimeMillis()): List<PhoneHistorySection> {
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
            "today" -> PhoneHistorySection(
                key = key,
                title = "今日",
                emoji = "🔥",
                dateLabel = formatFullDate(nowMillis),
                items = items
            )
            "yesterday" -> PhoneHistorySection(
                key = key,
                title = "昨日",
                emoji = "🔥",
                dateLabel = formatFullDate(yesterdayStart),
                items = items
            )
            else -> PhoneHistorySection(
                key = key,
                title = "更早",
                emoji = "📅",
                dateLabel = "${items.size} 条记录",
                items = items
            )
        }
    }
}

private fun buildHistoryTitle(item: WatchHistoryEntity): String {
    val episode = item.episodeName.takeIf { it.isNotBlank() } ?: "继续观看"
    return "${item.title} · $episode"
}

private fun historyDurationLabel(item: WatchHistoryEntity): String {
    return if (item.durationMs > 0) formatMs(item.durationMs) else "未知时长"
}

private fun historyProgress(item: WatchHistoryEntity): Float {
    return if (item.durationMs > 0) {
        (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
}

private fun historyUpdateLabel(updateTime: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val todayStart = startOfDayMillis(nowMillis)
    val yesterdayStart = todayStart - ONE_DAY_MS
    val time = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(updateTime))
    return when {
        updateTime >= todayStart -> "今天 $time"
        updateTime >= yesterdayStart -> "昨天 $time"
        else -> SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(updateTime))
    }
}

private fun formatFullDate(timeMillis: Long): String {
    return SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(Date(timeMillis))
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
