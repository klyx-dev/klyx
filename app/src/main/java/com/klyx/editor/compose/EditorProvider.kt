package com.klyx.editor.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.klyx.core.file.FileId
import com.klyx.editor.KlyxCodeEditor
import kotlinx.coroutines.Dispatchers

@Composable
fun EditorProvider(content: @Composable () -> Unit) {
    val editorMap = remember { mutableStateMapOf<FileId, KlyxCodeEditor>() }

    CompositionLocalProvider(
        LocalEditorStore provides editorMap,
        LocalEditorViewModel provides viewModel()
    ) {
        val viewModel = LocalEditorViewModel.current
        val editors = LocalEditorStore.current
        val state by viewModel.state.collectAsState(context = Dispatchers.IO)

        CompositionLocalProvider(LocalKlyxEditor provides editors[state.activeFileId]) {
            content()
        }
    }
}
