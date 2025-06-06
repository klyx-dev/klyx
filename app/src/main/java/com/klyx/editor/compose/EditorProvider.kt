package com.klyx.editor.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun EditorProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalEditorViewModel provides viewModel()
    ) {
        content()
    }
}
