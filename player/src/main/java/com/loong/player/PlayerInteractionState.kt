package com.loong.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@Stable
class PlayerInteractionState(
    initialPositionMs: Long = 0L,
    controlsInitiallyVisible: Boolean = false
) {
    var controlsVisible by mutableStateOf(controlsInitiallyVisible)
        private set

    var gestureInProgress by mutableStateOf(false)
        private set

    var isScrubbing by mutableStateOf(false)
        private set

    var scrubPositionMs by mutableLongStateOf(initialPositionMs)
        private set

    var feedbackOverlay by mutableStateOf<PhoneFeedbackOverlay?>(null)
        private set

    var interactionVersion by mutableIntStateOf(0)
        private set

    var feedbackVersion by mutableIntStateOf(0)
        private set

    fun registerInteraction() {
        controlsVisible = true
        interactionVersion += 1
    }

    fun showControls() {
        controlsVisible = true
        interactionVersion += 1
    }

    fun hideControls() {
        controlsVisible = false
    }

    fun toggleControls() {
        if (controlsVisible) hideControls() else showControls()
    }

    fun beginGesture() {
        gestureInProgress = true
    }

    fun updateSeekPreview(state: SeekOverlayState) {
        isScrubbing = true
        scrubPositionMs = state.targetPositionMs
        showFeedback(
            overlay = PhoneFeedbackOverlay.Seek(state),
            keepControlsVisible = false
        )
    }

    fun endGesture(type: PhoneGestureType): SeekOverlayState? {
        gestureInProgress = false
        feedbackVersion += 1
        return if (type == PhoneGestureType.SEEK) {
            val state = (feedbackOverlay as? PhoneFeedbackOverlay.Seek)?.state
            isScrubbing = false
            state
        } else {
            null
        }
    }

    fun cancelScrub() {
        isScrubbing = false
        registerInteraction()
    }

    fun cancelGesture() {
        gestureInProgress = false
        isScrubbing = false
        feedbackVersion += 1
    }

    fun updateScrubPosition(positionMs: Long) {
        isScrubbing = true
        scrubPositionMs = positionMs
        registerInteraction()
    }

    fun syncPlaybackPosition(positionMs: Long) {
        if (!isScrubbing) {
            scrubPositionMs = positionMs
        }
    }

    fun commitScrub(
        positionMs: Long,
        keepControlsVisible: Boolean = true
    ) {
        scrubPositionMs = positionMs
        isScrubbing = false
        if (keepControlsVisible) {
            registerInteraction()
        }
    }

    fun showAdjustment(type: PhoneGestureType, percent: Int) {
        showFeedback(
            overlay = PhoneFeedbackOverlay.Adjustment(
                AdjustmentOverlayState(type = type, percent = percent)
            ),
            keepControlsVisible = false
        )
    }

    fun showPlayPause(isPlaying: Boolean) {
        showFeedback(
            overlay = PhoneFeedbackOverlay.PlayPause(isPlaying),
            keepControlsVisible = false
        )
    }

    fun showFeedback(
        overlay: PhoneFeedbackOverlay,
        keepControlsVisible: Boolean = true
    ) {
        feedbackOverlay = overlay
        feedbackVersion += 1
        if (keepControlsVisible) {
            registerInteraction()
        }
    }

    fun clearFeedback() {
        feedbackOverlay = null
    }

    fun clearForNewMedia(positionMs: Long = 0L) {
        controlsVisible = false
        gestureInProgress = false
        isScrubbing = false
        scrubPositionMs = positionMs
        feedbackOverlay = null
        interactionVersion += 1
        feedbackVersion += 1
    }
}

@Composable
fun rememberPlayerInteractionState(
    key: Any? = Unit,
    initialPositionMs: Long = 0L,
    controlsInitiallyVisible: Boolean = false,
    controlsAutoHideMs: Long = PlayerInteractionDefaults.ControlsAutoHideMs,
    feedbackAutoHideMs: Long = PlayerInteractionDefaults.FeedbackAutoHideMs,
    seekFeedbackAutoHideMs: Long = PlayerInteractionDefaults.SeekFeedbackAutoHideMs,
    playPauseFeedbackAutoHideMs: Long = PlayerInteractionDefaults.PlayPauseFeedbackAutoHideMs,
    canAutoHideControls: Boolean = true
): PlayerInteractionState {
    val state = remember(key) {
        PlayerInteractionState(
            initialPositionMs = initialPositionMs,
            controlsInitiallyVisible = controlsInitiallyVisible
        )
    }

    LaunchedEffect(
        state.controlsVisible,
        state.interactionVersion,
        state.gestureInProgress,
        canAutoHideControls,
        controlsAutoHideMs
    ) {
        if (state.controlsVisible && !state.gestureInProgress && canAutoHideControls) {
            delay(controlsAutoHideMs)
            state.hideControls()
        }
    }

    LaunchedEffect(
        state.feedbackOverlay,
        state.feedbackVersion,
        state.gestureInProgress,
        feedbackAutoHideMs,
        seekFeedbackAutoHideMs,
        playPauseFeedbackAutoHideMs
    ) {
        val current = state.feedbackOverlay ?: return@LaunchedEffect
        val version = state.feedbackVersion
        val timeout = when (current) {
            is PhoneFeedbackOverlay.Adjustment -> if (state.gestureInProgress) null else feedbackAutoHideMs
            is PhoneFeedbackOverlay.Seek -> if (state.gestureInProgress) null else seekFeedbackAutoHideMs
            is PhoneFeedbackOverlay.PlayPause -> playPauseFeedbackAutoHideMs
        }
        if (timeout == null) return@LaunchedEffect
        delay(timeout)
        if (state.feedbackVersion == version) {
            state.clearFeedback()
        }
    }

    return state
}
