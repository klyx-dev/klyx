package com.klyx.editor.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.klyx.core.file.FileId
import com.klyx.editor.KlyxCodeEditor
import com.klyx.viewmodel.EditorViewModel

@Composable
fun EditorProvider(content: @Composable () -> Unit) {
    val editorMap = remember { mutableStateMapOf<FileId, KlyxCodeEditor>() }
    val viewModel: EditorViewModel = viewModel()

    CompositionLocalProvider(
        LocalEditorStore provides editorMap,
        LocalEditorViewModel provides viewModel
    ) {
        content()
    }
}
