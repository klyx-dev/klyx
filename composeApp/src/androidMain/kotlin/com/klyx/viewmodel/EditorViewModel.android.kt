package com.klyx.viewmodel

import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.tab.Tab
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCodeEditorApi::class)
fun EditorViewModel.getActiveEditor() = activeTab.map {
    when (val tab = it) {
        is Tab.FileTab -> tab.editorState.editor
        else -> null
    }
}
