package com.klyx.editor.compose.selection.contextmenu.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.util.fastAll
import com.klyx.editor.compose.selection.contextmenu.gestures.awaitFirstRightClickDown

/** Similar to PointerInputScope.detectTapAndPress, but for right clicks. */
internal suspend fun PointerInputScope.onRightClickDown(onDown: (Offset) -> Unit) {
    awaitEachGesture {
        val down = awaitFirstRightClickDown()
        down.consume()
        onDown(down.position)
        waitForUpOrCancellation()?.consume()
    }
}

/**
 * Similar to AwaitPointerEventScope.awaitFirstDown, but with an additional check to ensure it is a
 * right click.
 */
private suspend fun AwaitPointerEventScope.awaitFirstRightClickDown(): PointerInputChange {
    while (true) {
        val event = awaitPointerEvent()
        if (event.buttons.isSecondaryPressed && event.changes.fastAll { it.changedToDown() }) {
            return event.changes[0]
        }
    }
}
