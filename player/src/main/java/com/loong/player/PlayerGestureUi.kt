package com.loong.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class PhoneGestureType {
    BRIGHTNESS,
    VOLUME,
    SEEK
}

enum class OverlayIcon {
    BRIGHTNESS,
    VOLUME,
    PLAY,
    PAUSE
}

data class AdjustmentOverlayState(
    val type: PhoneGestureType,
    val percent: Int
)

data class SeekOverlayState(
    val targetPositionMs: Long,
    val durationMs: Long,
    val deltaMs: Long,
    val reachedEdgeMessage: String? = null
)

sealed interface PhoneFeedbackOverlay {
    data class Adjustment(val state: AdjustmentOverlayState) : PhoneFeedbackOverlay
    data class Seek(val state: SeekOverlayState) : PhoneFeedbackOverlay
    data class PlayPause(val isPlaying: Boolean) : PhoneFeedbackOverlay
}

@Composable
fun PhoneFullscreenGestureLayer(
    modifier: Modifier,
    resetKey: Any? = Unit,
    durationMs: Long,
    currentPositionMs: Long,
    brightnessPercent: Int,
    volumePercent: Int,
    reservedBottom: Dp = 80.dp,
    gestureConfig: PlayerGestureConfig = PlayerGestureConfig(),
    onSingleTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onGestureStart: () -> Unit,
    onGestureEnd: (PhoneGestureType) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onSeekPreview: (Long, Long, String?) -> Unit,
    onSeekCommit: (SeekOverlayState) -> Unit = {},
    onBoundaryReached: () -> Unit
) {
    val latestDurationMs by rememberUpdatedState(durationMs)
    val latestCurrentPositionMs by rememberUpdatedState(currentPositionMs)
    val latestBrightnessPercent by rememberUpdatedState(brightnessPercent)
    val latestVolumePercent by rememberUpdatedState(volumePercent)
    val latestOnSingleTap by rememberUpdatedState(onSingleTap)
    val latestOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val latestOnGestureStart by rememberUpdatedState(onGestureStart)
    val latestOnGestureEnd by rememberUpdatedState(onGestureEnd)
    val latestOnBrightnessChange by rememberUpdatedState(onBrightnessChange)
    val latestOnVolumeChange by rememberUpdatedState(onVolumeChange)
    val latestOnSeekPreview by rememberUpdatedState(onSeekPreview)
    val latestOnSeekCommit by rememberUpdatedState(onSeekCommit)
    val latestOnBoundaryReached by rememberUpdatedState(onBoundaryReached)

    Box(
        modifier = modifier
            .pointerInput(
                resetKey,
                reservedBottom,
                gestureConfig
            ) {
                val resolvedConfig = gestureConfig.copy(reservedBottomPx = reservedBottom.toPx())
                var pendingTapJob: Job? = null
                var lastTapTimeMs = Long.MIN_VALUE
                var lastTapX = 0f
                var lastTapY = 0f

                coroutineScope {
                    fun scheduleSingleTap(x: Float, y: Float, timeMs: Long) {
                        val isDoubleTap = timeMs - lastTapTimeMs <= resolvedConfig.doubleTapTimeoutMs &&
                            hypot((x - lastTapX).toDouble(), (y - lastTapY).toDouble()) <= resolvedConfig.doubleTapSlopPx
                        if (isDoubleTap) {
                            pendingTapJob?.cancel()
                            pendingTapJob = null
                            lastTapTimeMs = Long.MIN_VALUE
                            latestOnDoubleTap()
                        } else {
                            pendingTapJob?.cancel()
                            lastTapTimeMs = timeMs
                            lastTapX = x
                            lastTapY = y
                            pendingTapJob = launch {
                                delay(resolvedConfig.doubleTapTimeoutMs)
                                latestOnSingleTap()
                            }
                        }
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val widthPx = size.width.toFloat()
                        val heightPx = size.height.toFloat()
                        if (
                            widthPx <= 0f ||
                            heightPx <= 0f ||
                            down.position.x <= resolvedConfig.edgeIgnorePx ||
                            down.position.y >= heightPx - resolvedConfig.reservedBottomPx
                        ) {
                            return@awaitEachGesture
                        }

                        val pointerId = down.id
                        val downX = down.position.x
                        val downY = down.position.y
                        val startPositionMs = latestCurrentPositionMs
                        val startDurationMs = latestDurationMs
                        val startBrightnessPercent = latestBrightnessPercent
                        val startVolumePercent = latestVolumePercent
                        val verticalZone = when {
                            downX < widthPx / 3f -> PhoneGestureType.BRIGHTNESS
                            downX > widthPx * 2f / 3f -> PhoneGestureType.VOLUME
                            else -> null
                        }
                        var activeType: PhoneGestureType? = null
                        var lastSeekState: SeekOverlayState? = null
                        var boundaryTriggered = false

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes
                                    .filter { it.id != pointerId && it.pressed }
                                    .forEach { it.consume() }
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: run {
                                    if (event.changes.any { it.pressed }) {
                                        continue
                                    }
                                    break
                                }
                                if (!change.pressed) {
                                    val type = activeType
                                    if (type == null) {
                                        scheduleSingleTap(change.position.x, change.position.y, change.uptimeMillis)
                                    } else {
                                        if (type == PhoneGestureType.SEEK && lastSeekState != null) {
                                            latestOnSeekCommit(lastSeekState!!)
                                        }
                                        latestOnGestureEnd(type)
                                    }
                                    event.changes
                                        .filter { it.id != pointerId && it.pressed }
                                        .forEach { it.consume() }
                                    while (event.changes.any { it.id != pointerId && it.pressed }) {
                                        val waitEvent = awaitPointerEvent()
                                        waitEvent.changes.forEach { it.consume() }
                                        if (waitEvent.changes.none { it.pressed }) {
                                            break
                                        }
                                    }
                                    break
                                }
                                val totalX = change.position.x - downX
                                val totalY = change.position.y - downY
                                var belowTriggerDistance = false
                                val type = activeType ?: run {
                                    val absX = abs(totalX)
                                    val absY = abs(totalY)
                                    if (hypot(absX.toDouble(), absY.toDouble()) < resolvedConfig.triggerDistancePx) {
                                        belowTriggerDistance = true
                                        null
                                    } else {
                                        val angle = Math.toDegrees(atan2(absY.toDouble(), absX.toDouble())).toFloat()
                                        when {
                                            angle <= resolvedConfig.horizontalAngleMaxDegrees -> PhoneGestureType.SEEK
                                            angle >= resolvedConfig.verticalAngleMinDegrees && verticalZone != null -> verticalZone
                                            else -> null
                                        }?.also {
                                            activeType = it
                                            latestOnGestureStart()
                                        }
                                    }
                                }

                                if (belowTriggerDistance) {
                                    change.consume()
                                    continue
                                }

                                when (type) {
                                    PhoneGestureType.BRIGHTNESS -> {
                                        val percent = (startBrightnessPercent + (-totalY / resolvedConfig.verticalSensitivityPx))
                                            .roundToInt()
                                            .coerceIn(0, 100)
                                        latestOnBrightnessChange(percent)
                                        val atBoundary = percent == 0 || percent == 100
                                        if (atBoundary && !boundaryTriggered) {
                                            latestOnBoundaryReached()
                                            boundaryTriggered = true
                                        } else if (!atBoundary) {
                                            boundaryTriggered = false
                                        }
                                    }
                                    PhoneGestureType.VOLUME -> {
                                        val percent = (startVolumePercent + (-totalY / resolvedConfig.verticalSensitivityPx))
                                            .roundToInt()
                                            .coerceIn(0, 100)
                                        latestOnVolumeChange(percent)
                                        val atBoundary = percent == 0 || percent == 100
                                        if (atBoundary && !boundaryTriggered) {
                                            latestOnBoundaryReached()
                                            boundaryTriggered = true
                                        } else if (!atBoundary) {
                                            boundaryTriggered = false
                                        }
                                    }
                                    PhoneGestureType.SEEK -> {
                                        if (startDurationMs > 0L) {
                                            val previewDeltaMs = ((totalX / widthPx) * startDurationMs * resolvedConfig.seekSensitivity)
                                                .roundToLong()
                                            val targetMs = (startPositionMs + previewDeltaMs).coerceIn(0L, startDurationMs)
                                            val reachedEdgeMessage = when {
                                                targetMs == 0L && previewDeltaMs < 0L -> "已至开头"
                                                targetMs == startDurationMs && previewDeltaMs > 0L -> "已至结尾"
                                                else -> null
                                            }
                                            val state = SeekOverlayState(
                                                targetPositionMs = targetMs,
                                                durationMs = startDurationMs,
                                                deltaMs = targetMs - startPositionMs,
                                                reachedEdgeMessage = reachedEdgeMessage
                                            )
                                            lastSeekState = state
                                            latestOnSeekPreview(
                                                state.targetPositionMs,
                                                state.deltaMs,
                                                state.reachedEdgeMessage
                                            )
                                            val atBoundary = reachedEdgeMessage != null
                                            if (atBoundary && !boundaryTriggered) {
                                                latestOnBoundaryReached()
                                                boundaryTriggered = true
                                            } else if (!atBoundary) {
                                                boundaryTriggered = false
                                            }
                                        }
                                    }
                                    null -> Unit
                                }
                                change.consume()
                            }
                        } finally {
                            activeType = null
                        }
                    }
                }
            }
    )
}

@Composable
fun PlayerFeedbackPopup(
    overlay: PhoneFeedbackOverlay?,
    tvLayout: Boolean = false
) {
    if (overlay == null) return

    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = false
        )
    ) {
        when (overlay) {
            is PhoneFeedbackOverlay.Adjustment -> AdjustmentFeedbackOverlay(
                modifier = Modifier,
                type = overlay.state.type,
                percent = overlay.state.percent,
                tvLayout = tvLayout
            )
            is PhoneFeedbackOverlay.Seek -> SeekFeedbackOverlay(
                modifier = Modifier,
                state = overlay.state,
                tvLayout = tvLayout
            )
            is PhoneFeedbackOverlay.PlayPause -> PlayPauseFeedbackOverlay(
                modifier = Modifier,
                isPlaying = overlay.isPlaying,
                tvLayout = tvLayout
            )
        }
    }
}

@Composable
fun AdjustmentFeedbackOverlay(
    modifier: Modifier,
    type: PhoneGestureType,
    percent: Int,
    tvLayout: Boolean = false,
    accentColor: Color = Color(0xFF32D0FF),
    contentColor: Color = Color(0xFFF6F8FB)
) {
    val cornerRadius = if (tvLayout) 12.dp else 20.dp
    val horizontalPadding = if (tvLayout) 28.dp else 16.dp
    val verticalPadding = if (tvLayout) 22.dp else 14.dp
    val spacing = if (tvLayout) 12.dp else 8.dp
    val glyphSize = if (tvLayout) 54.dp else 40.dp
    val barWidth = if (tvLayout) 240.dp else 104.dp
    val barHeight = if (tvLayout) 8.dp else 4.dp
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xCC000000))
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        OverlayGlyph(
            icon = if (type == PhoneGestureType.BRIGHTNESS) OverlayIcon.BRIGHTNESS else OverlayIcon.VOLUME,
            modifier = Modifier.size(glyphSize)
        )
        BasicText(
            text = if (type == PhoneGestureType.BRIGHTNESS) "亮度 $percent%" else "音量 $percent%",
            style = TextStyle(color = contentColor, fontSize = if (tvLayout) 19.sp else 13.sp, fontWeight = FontWeight.SemiBold)
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(barHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.18f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent / 100f)
                    .height(barHeight)
                    .background(accentColor)
            )
        }
    }
}

@Composable
fun SeekFeedbackOverlay(
    modifier: Modifier,
    state: SeekOverlayState,
    tvLayout: Boolean = false,
    accentColor: Color = Color(0xFF32D0FF),
    contentColor: Color = Color(0xFFF6F8FB),
    secondaryColor: Color = Color(0xBDE7ECF3)
) {
    val isForward = state.deltaMs >= 0
    val cornerRadius = if (tvLayout) 12.dp else 20.dp
    val horizontalPadding = if (tvLayout) 24.dp else 16.dp
    val verticalPadding = if (tvLayout) 22.dp else 14.dp
    val spacing = if (tvLayout) 10.dp else 8.dp
    val barWidth = if (tvLayout) 180.dp else 104.dp
    val barHeight = if (tvLayout) 8.dp else 4.dp
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xCC000000))
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        BasicText(
            text = if (isForward) "快进  ▶▶" else "◀◀  快退",
            style = TextStyle(color = contentColor, fontSize = if (tvLayout) 21.sp else 13.sp, fontWeight = FontWeight.Bold)
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(barHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.18f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(playbackProgress(state.targetPositionMs, state.durationMs))
                    .height(barHeight)
                    .background(accentColor)
            )
        }
        BasicText(
            text = "${formatPlayerTime(state.targetPositionMs)} / ${formatPlayerTime(state.durationMs)}",
            style = TextStyle(color = contentColor, fontSize = if (tvLayout) 18.sp else 12.sp, fontWeight = FontWeight.SemiBold)
        )
        BasicText(
            text = state.reachedEdgeMessage ?: signedDurationLabel(state.deltaMs),
            style = TextStyle(
                color = if (state.reachedEdgeMessage == null) secondaryColor else accentColor,
                fontSize = if (tvLayout) 16.sp else 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
fun PlayPauseFeedbackOverlay(
    modifier: Modifier,
    isPlaying: Boolean,
    tvLayout: Boolean = false
) {
    Box(
        modifier = modifier
            .size(if (tvLayout) 108.dp else 80.dp)
            .clip(CircleShape)
            .background(Color(0xB3000000)),
        contentAlignment = Alignment.Center
    ) {
        OverlayGlyph(
            icon = if (isPlaying) OverlayIcon.PLAY else OverlayIcon.PAUSE,
            modifier = Modifier.size(if (tvLayout) 72.dp else 56.dp)
        )
    }
}

@Composable
fun OverlayGlyph(
    icon: OverlayIcon,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.08f
        when (icon) {
            OverlayIcon.BRIGHTNESS -> {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension * 0.18f
                drawCircle(
                    color = Color.White,
                    radius = radius,
                    center = center,
                    style = Stroke(width = stroke)
                )
                repeat(8) { index ->
                    val angle = Math.toRadians((index * 45.0) - 90.0)
                    val start = Offset(
                        x = center.x + kotlin.math.cos(angle).toFloat() * (radius + stroke),
                        y = center.y + kotlin.math.sin(angle).toFloat() * (radius + stroke)
                    )
                    val end = Offset(
                        x = center.x + kotlin.math.cos(angle).toFloat() * (radius + stroke * 2.2f),
                        y = center.y + kotlin.math.sin(angle).toFloat() * (radius + stroke * 2.2f)
                    )
                    drawLine(Color.White, start, end, stroke * 0.7f, StrokeCap.Round)
                }
            }
            OverlayIcon.VOLUME -> {
                val speaker = Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.42f)
                    lineTo(size.width * 0.38f, size.height * 0.42f)
                    lineTo(size.width * 0.54f, size.height * 0.26f)
                    lineTo(size.width * 0.54f, size.height * 0.74f)
                    lineTo(size.width * 0.38f, size.height * 0.58f)
                    lineTo(size.width * 0.2f, size.height * 0.58f)
                    close()
                }
                drawPath(speaker, Color.White)
                drawArc(
                    color = Color.White,
                    startAngle = -40f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.42f, size.height * 0.24f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.32f, size.height * 0.52f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.72f),
                    startAngle = -35f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.5f, size.height * 0.14f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.26f, size.height * 0.72f),
                    style = Stroke(width = stroke * 0.72f, cap = StrokeCap.Round)
                )
            }
            OverlayIcon.PLAY -> {
                val path = Path().apply {
                    moveTo(size.width * 0.34f, size.height * 0.22f)
                    lineTo(size.width * 0.74f, size.height * 0.5f)
                    lineTo(size.width * 0.34f, size.height * 0.78f)
                    close()
                }
                drawPath(path, Color.White.copy(alpha = 0.92f))
            }
            OverlayIcon.PAUSE -> {
                val barWidth = size.width * 0.14f
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.92f),
                    topLeft = Offset(size.width * 0.28f, size.height * 0.22f),
                    size = androidx.compose.ui.geometry.Size(barWidth, size.height * 0.56f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.92f),
                    topLeft = Offset(size.width * 0.58f, size.height * 0.22f),
                    size = androidx.compose.ui.geometry.Size(barWidth, size.height * 0.56f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}

private fun signedDurationLabel(deltaMs: Long): String {
    val absSeconds = (abs(deltaMs) / 1_000L).coerceAtLeast(0L)
    val prefix = if (deltaMs >= 0) "+" else "-"
    return "$prefix${formatPlayerTime(absSeconds * 1_000L)}"
}
