package com.klyx.editor

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput

@ExperimentalCodeEditorApi
internal fun Modifier.codeEditorScroll(state: CodeEditorState) = composed {
    val offset by animateOffsetAsState(Offset.Zero)

    pointerInput(state) {
        detectDragGestures(
            onDrag = { change, dragAmount ->
                change.consume()
                state.scroll(dragAmount)
            }
        )
    }
}
