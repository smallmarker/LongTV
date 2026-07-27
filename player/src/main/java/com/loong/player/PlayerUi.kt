package com.loong.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

fun formatPlayerTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun playbackProgress(positionMs: Long, durationMs: Long): Float {
    if (durationMs <= 0) return 0f
    return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

enum class PlayerResizeMode {
    Fit,
    Fill,
    Zoom,
    FixedWidth,
    FixedHeight
}

sealed interface PlayerCommand {
    data object Play : PlayerCommand
    data object Pause : PlayerCommand
    data object TogglePlayPause : PlayerCommand
    data class SeekBy(val offsetMs: Long) : PlayerCommand
    data class SeekTo(val positionMs: Long) : PlayerCommand
    data class SeekToAndPlay(val positionMs: Long) : PlayerCommand
    data class SetSpeed(val speed: Float) : PlayerCommand
    data class SetVolume(val volume: Float) : PlayerCommand
    data class SetMuted(val muted: Boolean) : PlayerCommand
    data class SetResizeMode(val resizeMode: PlayerResizeMode) : PlayerCommand
}

@Stable
class PlayerControlState(
    initialResizeMode: PlayerResizeMode = PlayerResizeMode.Fit,
    initialVolume: Float = 1f,
    initialMuted: Boolean = false
) {
    private var pendingCommand by mutableStateOf<PlayerCommand?>(null)

    var commandVersion by mutableIntStateOf(0)
        private set

    var resizeMode by mutableStateOf(initialResizeMode)
        private set

    var volume by mutableFloatStateOf(initialVolume.coerceIn(0f, 1f))
        private set

    var muted by mutableStateOf(initialMuted)
        private set

    fun play() = dispatch(PlayerCommand.Play)

    fun pause() = dispatch(PlayerCommand.Pause)

    fun togglePlayPause() = dispatch(PlayerCommand.TogglePlayPause)

    fun seekBack(offsetMs: Long = 10_000L) = seekBy(-offsetMs)

    fun seekForward(offsetMs: Long = 10_000L) = seekBy(offsetMs)

    fun seekBy(offsetMs: Long) = dispatch(PlayerCommand.SeekBy(offsetMs))

    fun seekTo(positionMs: Long) = dispatch(PlayerCommand.SeekTo(positionMs))

    fun seekToAndPlay(positionMs: Long) = dispatch(PlayerCommand.SeekToAndPlay(positionMs))

    fun setSpeed(speed: Float) = dispatch(PlayerCommand.SetSpeed(speed.coerceIn(0.25f, 4f)))

    fun changeVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        dispatch(PlayerCommand.SetVolume(volume))
    }

    fun changeMuted(value: Boolean) {
        muted = value
        dispatch(PlayerCommand.SetMuted(value))
    }

    fun toggleMuted() = changeMuted(!muted)

    fun changeResizeMode(value: PlayerResizeMode) {
        resizeMode = value
        dispatch(PlayerCommand.SetResizeMode(value))
    }

    fun cycleResizeMode() {
        val values = PlayerResizeMode.entries
        val nextIndex = (values.indexOf(resizeMode) + 1).floorMod(values.size)
        changeResizeMode(values[nextIndex])
    }

    internal fun consume(): PlayerCommand? {
        val command = pendingCommand
        pendingCommand = null
        return command
    }

    private fun dispatch(command: PlayerCommand) {
        pendingCommand = command
        commandVersion += 1
    }

    private fun Int.floorMod(other: Int): Int = ((this % other) + other) % other
}

@Composable
fun rememberPlayerControlState(
    initialResizeMode: PlayerResizeMode = PlayerResizeMode.Fit,
    initialVolume: Float = 1f,
    initialMuted: Boolean = false
): PlayerControlState = remember {
    PlayerControlState(
        initialResizeMode = initialResizeMode,
        initialVolume = initialVolume,
        initialMuted = initialMuted
    )
}
