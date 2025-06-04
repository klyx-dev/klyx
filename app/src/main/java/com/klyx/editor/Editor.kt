package com.klyx.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Editor {
    var current by mutableStateOf<KlyxCodeEditor?>(null)
        private set

    fun setCurrentEditor(editor: KlyxCodeEditor?) {
        current = editor
    }
}
