package com.klyx.editor.compose.internal

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.util.fastAny

/** Detects pointer down and up events. This detector does not require events to be unconsumed. */
internal suspend fun PointerInputScope.detectPressDownGesture(
    onDown: TapOnPosition,
    onUp: (() -> Unit)? = null,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onDown.onEvent(down.position)

        if (onUp != null) {
            // Wait for that pointer to come up.
            do {
                val event = awaitPointerEvent()
            } while (event.changes.fastAny { it.id == down.id && it.pressed })
            onUp.invoke()
        }
    }
}

internal fun interface TapOnPosition {
    fun onEvent(offset: Offset)
}
