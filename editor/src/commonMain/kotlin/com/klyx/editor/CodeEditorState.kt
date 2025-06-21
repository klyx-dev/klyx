package com.klyx.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@ExperimentalCodeEditorApi
class CodeEditorState(
    val initialText: String = "",
) {
}

@ExperimentalCodeEditorApi
@Composable
fun rememberCodeEditorState(
    initialText: String = "",
) = remember(initialText) { CodeEditorState(initialText) }
