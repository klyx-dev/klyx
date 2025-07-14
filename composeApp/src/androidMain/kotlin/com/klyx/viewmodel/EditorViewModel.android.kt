package com.klyx.viewmodel

import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.tab.Tab
import io.github.rosemoe.sora.widget.CodeEditor

@OptIn(ExperimentalCodeEditorApi::class)
fun EditorViewModel.getActiveEditor(): CodeEditor? {
    return when (val tab = getActiveTab()) {
        is Tab.FileTab -> tab.editorState.editor
        else -> null
    }
}
