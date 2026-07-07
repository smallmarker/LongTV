package com.loong.longtv.ui.shared.detail

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.ContextWrapper
import android.os.PowerManager
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.util.WeakHashMap

@Composable
internal fun KeepScreenOnEffect(enabled: Boolean) {
    val context = LocalContext.current

    DisposableEffect(context, enabled) {
        val activity = context.findActivity()
        val token = if (enabled && activity != null) {
            KeepScreenOnRegistry.acquire(activity, context.applicationContext)
        } else {
            null
        }
        onDispose {
            token?.release()
        }
    }
}

private object KeepScreenOnRegistry {
    private data class WindowState(
        var count: Int,
        val hadKeepScreenOnFlag: Boolean,
        val decorKeepScreenOn: Boolean,
        val wakeLock: PowerManager.WakeLock?
    )

    private val states = WeakHashMap<Window, WindowState>()

    @Synchronized
    fun acquire(activity: Activity, appContext: Context): KeepScreenOnToken {
        val window = activity.window
        val state = states.getOrPut(window) {
            WindowState(
                count = 0,
                hadKeepScreenOnFlag = window.hasKeepScreenOnFlag(),
                decorKeepScreenOn = window.decorView.keepScreenOn,
                wakeLock = appContext.createPlaybackWakeLock()
            )
        }
        state.count += 1
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.keepScreenOn = true
        state.wakeLock?.let { wakeLock ->
            if (!wakeLock.isHeld) {
                wakeLock.acquire()
            }
        }
        return KeepScreenOnToken(window)
    }

    @Synchronized
    fun release(window: Window) {
        val state = states[window] ?: return
        state.count -= 1
        if (state.count > 0) return

        if (!state.hadKeepScreenOnFlag) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        window.decorView.keepScreenOn = state.decorKeepScreenOn
        state.wakeLock?.let { wakeLock ->
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
        states.remove(window)
    }
}

private class KeepScreenOnToken(
    private val window: Window
) {
    fun release() {
        KeepScreenOnRegistry.release(window)
    }
}

private fun Window.hasKeepScreenOnFlag(): Boolean {
    return attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
}

@SuppressLint("WakelockTimeout")
@Suppress("DEPRECATION")
private fun Context.createPlaybackWakeLock(): PowerManager.WakeLock? {
    val powerManager = getSystemService(POWER_SERVICE) as? PowerManager ?: return null
    return powerManager.newWakeLock(
        PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
        "LongTV:VideoPlayback"
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
