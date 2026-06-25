package com.loong.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerGestureDetectorTest {
    private val config = PlayerGestureConfig(
        triggerDistancePx = 10f,
        edgeIgnorePx = 15f,
        reservedBottomPx = 80f,
        verticalSensitivityPx = 8f,
        seekSensitivity = 0.8f,
        doubleTapTimeoutMs = 260L,
        doubleTapSlopPx = 80f
    )

    private val context = PlayerGestureContext(
        widthPx = 900f,
        heightPx = 500f,
        currentPositionMs = 60_000L,
        durationMs = 120_000L,
        brightnessPercent = 50,
        volumePercent = 40
    )

    @Test
    fun leftVerticalDragChangesBrightnessOnly() {
        val detector = PlayerGestureDetector(config)

        detector.onDown(100f, 200f, 0L, context)
        val events = detector.onMove(100f, 120f)

        assertTrue(events.any { it is PlayerGestureEvent.GestureStart && it.type == PhoneGestureType.BRIGHTNESS })
        assertTrue(events.any { it is PlayerGestureEvent.BrightnessChange && it.percent == 60 })
        assertFalse(events.any { it is PlayerGestureEvent.VolumeChange })
        assertFalse(events.any { it is PlayerGestureEvent.SeekPreview })
    }

    @Test
    fun leftVerticalDragKeepsUpdatingDuringSamePress() {
        val detector = PlayerGestureDetector(config)

        detector.onDown(100f, 200f, 0L, context)
        val firstMove = detector.onMove(100f, 160f)
        val secondMove = detector.onMove(100f, 80f)

        assertTrue(firstMove.any { it is PlayerGestureEvent.BrightnessChange && it.percent == 55 })
        assertTrue(secondMove.any { it is PlayerGestureEvent.BrightnessChange && it.percent == 65 })
        assertFalse(secondMove.any { it is PlayerGestureEvent.GestureStart })
    }

    @Test
    fun rightVerticalDragChangesVolumeOnly() {
        val detector = PlayerGestureDetector(config)

        detector.onDown(800f, 200f, 0L, context)
        val events = detector.onMove(800f, 120f)

        assertTrue(events.any { it is PlayerGestureEvent.GestureStart && it.type == PhoneGestureType.VOLUME })
        assertTrue(events.any { it is PlayerGestureEvent.VolumeChange && it.percent == 50 })
        assertFalse(events.any { it is PlayerGestureEvent.BrightnessChange })
        assertFalse(events.any { it is PlayerGestureEvent.SeekPreview })
    }

    @Test
    fun horizontalDragPreviewsAndCommitsSeek() {
        val detector = PlayerGestureDetector(config)

        detector.onDown(450f, 200f, 0L, context)
        val previewEvents = detector.onMove(600f, 203f)
        val commitEvents = detector.onUp(600f, 203f, 120L)

        val preview = previewEvents.filterIsInstance<PlayerGestureEvent.SeekPreview>().single()
        val commit = commitEvents.filterIsInstance<PlayerGestureEvent.SeekCommit>().single()
        assertEquals(76_000L, preview.state.targetPositionMs)
        assertEquals(preview.state, commit.state)
        assertTrue(commitEvents.any { it is PlayerGestureEvent.GestureEnd && it.type == PhoneGestureType.SEEK })
    }

    @Test
    fun horizontalDragKeepsUpdatingSeekPreviewDuringSamePress() {
        val detector = PlayerGestureDetector(config)

        detector.onDown(450f, 200f, 0L, context)
        val firstMove = detector.onMove(540f, 202f)
        val secondMove = detector.onMove(720f, 204f)

        val firstPreview = firstMove.filterIsInstance<PlayerGestureEvent.SeekPreview>().single()
        val secondPreview = secondMove.filterIsInstance<PlayerGestureEvent.SeekPreview>().single()
        assertEquals(69_600L, firstPreview.state.targetPositionMs)
        assertEquals(88_800L, secondPreview.state.targetPositionMs)
        assertFalse(secondMove.any { it is PlayerGestureEvent.GestureStart })
    }

    @Test
    fun smallMoveStillTaps() {
        val detector = PlayerGestureDetector(config)

        detector.onDown(450f, 200f, 0L, context)
        val moveEvents = detector.onMove(454f, 203f)
        val upEvents = detector.onUp(454f, 203f, 80L)

        assertTrue(moveEvents.isEmpty())
        assertEquals(listOf(PlayerGestureEvent.Tap), upEvents)
    }

    @Test
    fun doubleTapDoesNotReturnSecondSingleTap() {
        val detector = PlayerGestureDetector(config)

        detector.onDown(450f, 200f, 0L, context)
        val firstUp = detector.onUp(450f, 200f, 50L)
        detector.onDown(455f, 205f, 120L, context)
        val secondUp = detector.onUp(455f, 205f, 170L)

        assertEquals(listOf(PlayerGestureEvent.Tap), firstUp)
        assertEquals(listOf(PlayerGestureEvent.DoubleTap), secondUp)
    }

    @Test
    fun seekClampsAtStartAndReportsBoundary() {
        val detector = PlayerGestureDetector(config)
        val nearStart = context.copy(currentPositionMs = 2_000L)

        detector.onDown(450f, 200f, 0L, nearStart)
        val events = detector.onMove(0f, 200f)
        val preview = events.filterIsInstance<PlayerGestureEvent.SeekPreview>().single()

        assertEquals(0L, preview.state.targetPositionMs)
        assertEquals("已至开头", preview.state.reachedEdgeMessage)
        assertTrue(events.any { it == PlayerGestureEvent.BoundaryReached })
    }

    @Test
    fun bottomReservedAreaIgnoresGesture() {
        val detector = PlayerGestureDetector(config)

        detector.onDown(450f, 450f, 0L, context)
        val events = detector.onMove(700f, 450f)
        val upEvents = detector.onUp(700f, 450f, 100L)

        assertTrue(events.isEmpty())
        assertTrue(upEvents.isEmpty())
    }
}
