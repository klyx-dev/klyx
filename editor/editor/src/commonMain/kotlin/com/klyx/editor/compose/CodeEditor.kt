package com.klyx.editor.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import com.klyx.editor.compose.draw.EditorLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CodeEditor(
    modifier: Modifier = Modifier,
    editable: Boolean = true,
    colorScheme: EditorColorScheme = EditorDefaults.colorScheme(),
    state: CodeEditorState = rememberCodeEditorState(editable = editable),
    showLineNumber: Boolean = true,
    pinLineNumber: Boolean = true,
    fontFamily: FontFamily = FontFamily.Monospace,
    fontSize: TextUnit = TextUnit.Unspecified
) {
    ProvideEditorCompositionLocals(
        colorScheme = colorScheme
    ) {
        CodeEditorImpl(
            modifier = modifier,
            state = state.apply { this.colorScheme = colorScheme },
            editable = editable,
            showLineNumber = showLineNumber,
            pinLineNumber = pinLineNumber,
            fontFamily = fontFamily,
            fontSize = fontSize
        )
    }
}

@Composable
private fun CodeEditorImpl(
    modifier: Modifier,
    state: CodeEditorState,
    editable: Boolean,
    showLineNumber: Boolean,
    pinLineNumber: Boolean,
    fontFamily: FontFamily,
    fontSize: TextUnit
) {
    val cursorAlpha = remember { Animatable(1f) }
    var cursorJob: Job? = null

    var isTyping by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { state.cursor }.collect {
            isTyping = true
            delay(400)
            isTyping = false
        }
    }

    LaunchedEffect(state.editable, state.cursor) {
        cursorJob?.cancel()

        if (state.editable) {
            cursorJob = launch(Dispatchers.Default) {
                while (true) {
                    if (!isTyping) {
                        cursorAlpha.animateTo(0f, tween(500)) { state.cursorAlpha = value }
                        cursorAlpha.animateTo(1f, tween(500)) { state.cursorAlpha = value }
                    } else {
                        state.cursorAlpha = 1f
                        delay(500)
                    }
                }
            }
        }
    }

    EditorLayout(
        state = state,
        modifier = modifier
            .clipToBounds()
            .pointerHoverIcon(PointerIcon.Text, overrideDescendants = true),
        editable = editable,
        showLineNumber = showLineNumber,
        pinLineNumber = pinLineNumber,
        fontFamily = fontFamily,
        fontSize = fontSize
    )
}
