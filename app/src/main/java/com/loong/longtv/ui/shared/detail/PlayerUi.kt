package com.loong.longtv.ui.shared.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
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

sealed interface PlayerCommand {
    data object TogglePlayPause : PlayerCommand
    data class SeekBy(val offsetMs: Long) : PlayerCommand
    data class SeekTo(val positionMs: Long) : PlayerCommand
    data class SetSpeed(val speed: Float) : PlayerCommand
}

@Stable
class PlayerControlState {
    private var pendingCommand by mutableStateOf<PlayerCommand?>(null)
    var commandVersion by mutableIntStateOf(0)
        private set

    fun togglePlayPause() = dispatch(PlayerCommand.TogglePlayPause)

    fun seekBy(offsetMs: Long) = dispatch(PlayerCommand.SeekBy(offsetMs))

    fun seekTo(positionMs: Long) = dispatch(PlayerCommand.SeekTo(positionMs))

    fun setSpeed(speed: Float) = dispatch(PlayerCommand.SetSpeed(speed))

    internal fun consume(): PlayerCommand? {
        val command = pendingCommand
        pendingCommand = null
        return command
    }

    private fun dispatch(command: PlayerCommand) {
        pendingCommand = command
        commandVersion += 1
    }
}

@Composable
fun rememberPlayerControlState(): PlayerControlState = remember { PlayerControlState() }
