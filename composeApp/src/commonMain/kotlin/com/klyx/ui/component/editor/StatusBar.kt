package com.klyx.ui.component.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.klyx.editor.CodeEditorState
import com.klyx.editor.ExperimentalCodeEditorApi

@OptIn(ExperimentalCodeEditorApi::class)
@Composable
expect fun StatusBar(
    editorState: CodeEditorState,
    modifier: Modifier = Modifier
)
