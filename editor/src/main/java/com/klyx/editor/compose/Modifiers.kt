package com.klyx.editor.compose

import android.graphics.Paint
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.SoftwareKeyboardController
import com.klyx.editor.CodeEditorState
import com.klyx.editor.compose.input.textInput
import com.klyx.editor.input.EditorInputConnection

internal fun Modifier.editorScroll(
    editorState: CodeEditorState,
    textPaint: Paint
) = composed {
    scrollable(
        state = rememberScrollableState { delta ->
            val maxScrollY = (editorState.totalLines * editorState.lineHeightWithSpacing - editorState.viewportSize.height + 200f).coerceAtLeast(0f)
            editorState.scrollOffset += Offset(0f, delta)
            editorState.scrollOffset = editorState.scrollOffset.copy(
                y = editorState.scrollOffset.y.coerceAtLeast(-maxScrollY).coerceAtMost(0f)
            )
            delta
        },
        orientation = Orientation.Vertical
    ).scrollable(
        state = rememberScrollableState { delta ->
            val maxScrollX = editorState.visibleLines.maxOfOrNull { (_, line) ->
                textPaint.measureText(line)
            }?.let { it + editorState.gutterWidth + editorState.gutterPadding * 2 } ?: 0f

            val maxScroll = (maxScrollX - editorState.viewportSize.width + 200f).coerceAtLeast(0f)

            editorState.scrollOffset += Offset(delta, 0f)
            editorState.scrollOffset = editorState.scrollOffset.copy(
                x = editorState.scrollOffset.x.coerceAtLeast(-maxScroll).coerceAtMost(0f)
            )
            delta
        },
        orientation = Orientation.Horizontal
    )
}

internal fun Modifier.editorTextInput(
    keyboardController: SoftwareKeyboardController?,
    editorState: CodeEditorState
) = this then Modifier.textInput(keyboardController) { EditorInputConnection(it, editorState) }
