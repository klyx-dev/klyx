package com.klyx.editor.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class EditorScrollState {
    var scrollY by mutableStateOf(0f)
        internal set

    fun scrollBy(delta: Float, maxScroll: Float) {
        scrollY = (scrollY - delta).coerceIn(0f, maxScroll)
    }
}

@Composable
fun rememberEditorScrollState() = remember { EditorScrollState() }
