package com.klyx.editor

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@ExperimentalCodeEditorApi
internal fun Modifier.codeEditorScroll(state: CodeEditorState) = this then Modifier.pointerInput(state) {
    detectDragGestures(
        onDrag = { change, dragAmount ->
            change.consume()
            state.scroll(-dragAmount)
        }
    )
}
