package com.klyx.editor.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.klyx.editor.compose.text.buffer.EmptyTextBuffer
import com.klyx.editor.compose.text.buffer.buildTextBuffer

@Stable
class CodeEditorState(
    initialText: String,
    val scrollState: EditorScrollState
) {
    val text = if (initialText.isEmpty()) EmptyTextBuffer else buildTextBuffer { append(initialText) }

    internal var version by mutableIntStateOf(0)
        private set

    fun update() {
        version++
    }
}

@Composable
fun rememberCodeEditorState(
    initialText: String = "",
    scrollState: EditorScrollState = rememberEditorScrollState()
) = remember(scrollState) {
    CodeEditorState(initialText, scrollState)
}
