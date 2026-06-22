package com.loong.longtv.ui.shared.detail

typealias PlayerCommand = com.loong.player.PlayerCommand
typealias PlayerControlState = com.loong.player.PlayerControlState
typealias PlayerResizeMode = com.loong.player.PlayerResizeMode

fun formatPlayerTime(ms: Long): String = com.loong.player.formatPlayerTime(ms)

fun playbackProgress(positionMs: Long, durationMs: Long): Float {
    return com.loong.player.playbackProgress(positionMs, durationMs)
}

@androidx.compose.runtime.Composable
fun rememberPlayerControlState(
    initialResizeMode: PlayerResizeMode = PlayerResizeMode.Fit,
    initialVolume: Float = 1f,
    initialMuted: Boolean = false
): PlayerControlState = com.loong.player.rememberPlayerControlState(
    initialResizeMode = initialResizeMode,
    initialVolume = initialVolume,
    initialMuted = initialMuted
)
