package com.loong.longtv.ui.shared.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged

@Composable
internal fun Modifier.restoreTvFocus(restore: Boolean, onRestored: () -> Unit): Modifier {
    if (!restore) return this
    val requester = remember { FocusRequester() }
    LaunchedEffect(requester) {
        withFrameNanos { }
        requester.requestFocus()
    }
    return focusRequester(requester).onFocusChanged {
        if (it.isFocused) onRestored()
    }
}
