package com.loong.player

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.roundToLong

data class PlayerGestureConfig(
    val triggerDistancePx: Float = 10f,
    val edgeIgnorePx: Float = 15f,
    val reservedBottomPx: Float = 80f,
    val verticalSensitivityPx: Float = 8f,
    val seekSensitivity: Float = 0.8f,
    val horizontalAngleMaxDegrees: Float = 30f,
    val verticalAngleMinDegrees: Float = 60f,
    val doubleTapTimeoutMs: Long = 260L,
    val doubleTapSlopPx: Float = 80f
)

data class PlayerGestureContext(
    val widthPx: Float,
    val heightPx: Float,
    val currentPositionMs: Long,
    val durationMs: Long,
    val brightnessPercent: Int,
    val volumePercent: Int
)

sealed interface PlayerGestureEvent {
    data object Tap : PlayerGestureEvent
    data object DoubleTap : PlayerGestureEvent
    data class GestureStart(val type: PhoneGestureType) : PlayerGestureEvent
    data class GestureEnd(val type: PhoneGestureType) : PlayerGestureEvent
    data class BrightnessChange(val percent: Int) : PlayerGestureEvent
    data class VolumeChange(val percent: Int) : PlayerGestureEvent
    data class SeekPreview(val state: SeekOverlayState) : PlayerGestureEvent
    data class SeekCommit(val state: SeekOverlayState) : PlayerGestureEvent
    data object BoundaryReached : PlayerGestureEvent
}

class PlayerGestureDetector(
    private val config: PlayerGestureConfig = PlayerGestureConfig()
) {
    private data class Down(
        val x: Float,
        val y: Float,
        val timeMs: Long,
        val context: PlayerGestureContext,
        val zone: PhoneGestureType?
    )

    private var down: Down? = null
    private var activeType: PhoneGestureType? = null
    private var ignored = false
    private var boundaryTriggered = false
    private var lastSeekState: SeekOverlayState? = null
    private var lastTapTimeMs = Long.MIN_VALUE
    private var lastTapX = 0f
    private var lastTapY = 0f

    fun onDown(
        x: Float,
        y: Float,
        timeMs: Long,
        context: PlayerGestureContext
    ) {
        resetGesture()
        ignored = shouldIgnoreDown(x, y, context)
        if (ignored) return
        down = Down(
            x = x,
            y = y,
            timeMs = timeMs,
            context = context,
            zone = when {
                x < context.widthPx / 3f -> PhoneGestureType.BRIGHTNESS
                x > context.widthPx * 2f / 3f -> PhoneGestureType.VOLUME
                else -> null
            }
        )
    }

    fun onMove(x: Float, y: Float): List<PlayerGestureEvent> {
        if (ignored) return emptyList()
        val start = down ?: return emptyList()
        val totalX = x - start.x
        val totalY = y - start.y
        val events = mutableListOf<PlayerGestureEvent>()
        val type = activeType ?: lockGestureType(totalX, totalY, start.zone) ?: return emptyList()
        if (activeType == null) {
            activeType = type
            events += PlayerGestureEvent.GestureStart(type)
        }

        when (type) {
            PhoneGestureType.BRIGHTNESS -> {
                val percent = (start.context.brightnessPercent + (-totalY / config.verticalSensitivityPx))
                    .roundToInt()
                    .coerceIn(0, 100)
                events += PlayerGestureEvent.BrightnessChange(percent)
                maybeAddBoundaryEvent(percent == 0 || percent == 100, events)
            }

            PhoneGestureType.VOLUME -> {
                val percent = (start.context.volumePercent + (-totalY / config.verticalSensitivityPx))
                    .roundToInt()
                    .coerceIn(0, 100)
                events += PlayerGestureEvent.VolumeChange(percent)
                maybeAddBoundaryEvent(percent == 0 || percent == 100, events)
            }

            PhoneGestureType.SEEK -> {
                val state = seekState(totalX, start.context) ?: return events
                lastSeekState = state
                events += PlayerGestureEvent.SeekPreview(state)
                maybeAddBoundaryEvent(state.reachedEdgeMessage != null, events)
            }
        }
        return events
    }

    fun onUp(x: Float, y: Float, timeMs: Long): List<PlayerGestureEvent> {
        if (ignored) {
            resetGesture()
            return emptyList()
        }
        val start = down ?: return emptyList()
        val type = activeType
        val events = mutableListOf<PlayerGestureEvent>()
        if (type == null) {
            val isDoubleTap = timeMs - lastTapTimeMs <= config.doubleTapTimeoutMs &&
                hypot((x - lastTapX).toDouble(), (y - lastTapY).toDouble()) <= config.doubleTapSlopPx
            if (isDoubleTap) {
                lastTapTimeMs = Long.MIN_VALUE
                events += PlayerGestureEvent.DoubleTap
            } else {
                lastTapTimeMs = timeMs
                lastTapX = x
                lastTapY = y
                events += PlayerGestureEvent.Tap
            }
            resetGesture()
            return events
        }

        if (type == PhoneGestureType.SEEK) {
            val totalX = x - start.x
            val state = seekState(totalX, start.context) ?: lastSeekState
            if (state != null) {
                events += PlayerGestureEvent.SeekCommit(state)
            }
        }
        events += PlayerGestureEvent.GestureEnd(type)
        resetGesture()
        return events
    }

    fun cancel(): List<PlayerGestureEvent> {
        val type = activeType
        resetGesture()
        return if (type == null) emptyList() else listOf(PlayerGestureEvent.GestureEnd(type))
    }

    private fun shouldIgnoreDown(x: Float, y: Float, context: PlayerGestureContext): Boolean {
        return context.widthPx <= 0f ||
            context.heightPx <= 0f ||
            x <= config.edgeIgnorePx ||
            y >= context.heightPx - config.reservedBottomPx
    }

    private fun lockGestureType(
        totalX: Float,
        totalY: Float,
        zone: PhoneGestureType?
    ): PhoneGestureType? {
        val absX = abs(totalX)
        val absY = abs(totalY)
        if (hypot(absX.toDouble(), absY.toDouble()) < config.triggerDistancePx) return null
        val angle = Math.toDegrees(atan2(absY.toDouble(), absX.toDouble())).toFloat()
        return when {
            angle <= config.horizontalAngleMaxDegrees -> PhoneGestureType.SEEK
            angle >= config.verticalAngleMinDegrees && zone != null -> zone
            absX >= absY -> PhoneGestureType.SEEK
            zone != null -> zone
            else -> null
        }
    }

    private fun seekState(totalX: Float, context: PlayerGestureContext): SeekOverlayState? {
        if (context.durationMs <= 0L || context.widthPx <= 0f) return null
        val previewDeltaMs = ((totalX / context.widthPx) * context.durationMs * config.seekSensitivity)
            .roundToLong()
        val targetMs = (context.currentPositionMs + previewDeltaMs).coerceIn(0L, context.durationMs)
        val reachedEdgeMessage = when {
            targetMs == 0L && previewDeltaMs < 0L -> "已至开头"
            targetMs == context.durationMs && previewDeltaMs > 0L -> "已至结尾"
            else -> null
        }
        return SeekOverlayState(
            targetPositionMs = targetMs,
            durationMs = context.durationMs,
            deltaMs = targetMs - context.currentPositionMs,
            reachedEdgeMessage = reachedEdgeMessage
        )
    }

    private fun maybeAddBoundaryEvent(
        atBoundary: Boolean,
        events: MutableList<PlayerGestureEvent>
    ) {
        if (atBoundary && !boundaryTriggered) {
            events += PlayerGestureEvent.BoundaryReached
            boundaryTriggered = true
        } else if (!atBoundary) {
            boundaryTriggered = false
        }
    }

    private fun resetGesture() {
        down = null
        activeType = null
        ignored = false
        boundaryTriggered = false
        lastSeekState = null
    }
}
