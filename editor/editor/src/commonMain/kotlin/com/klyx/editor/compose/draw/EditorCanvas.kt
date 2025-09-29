package com.klyx.editor.compose.draw

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import com.klyx.editor.compose.CodeEditorState
import com.klyx.editor.compose.EditorMeasurePolicy
import com.klyx.editor.compose.ProvideEditorCompositionLocals
import com.klyx.editor.compose.input.editorInput
import com.klyx.editor.compose.renderer.renderEditor

@Composable
internal fun EditorCanvas(
    state: CodeEditorState,
    modifier: Modifier = Modifier,
    editable: Boolean = true,
    onDraw: DrawScope.() -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    ProvideEditorCompositionLocals {
        Layout(
            modifier = modifier
                .focusRequester(focusRequester)
                .editorInput(
                    state = state,
                    editable = editable
                )
                .focusable(interactionSource = remember { MutableInteractionSource() })
                .pointerInput(state) {
                    detectTapGestures {
                        focusRequester.requestFocus()
                    }
                }
                .renderEditor(state, onDraw),
            measurePolicy = EditorMeasurePolicy
        )
    }
}
