package com.klyx.editor.compose.selection.contextmenu

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.klyx.editor.compose.selection.contextmenu.ContextMenuState.Status
import com.klyx.editor.compose.selection.contextmenu.gestures.onRightClickDown

/** Unique key to avoid [Unit] clashes in [pointerInput]. */
private object ContextMenuKey

/**
 * Track right click events and update the [state] to [ContextMenuState.Status.Open] with the click
 * offset.
 *
 * @param state the state that will have its status set to open on a right click
 */
internal fun Modifier.contextMenuGestures(state: ContextMenuState): Modifier = contextMenuGestures {
    state.status = Status.Open(offset = it)
}

/**
 * Track right click events and invoke [onOpenGesture] callback
 *
 * @param onOpenGesture the callback that will be invoked on a right click
 */
internal fun Modifier.contextMenuGestures(onOpenGesture: (Offset) -> Unit): Modifier =
    pointerInput(ContextMenuKey) { onRightClickDown(onOpenGesture) }
