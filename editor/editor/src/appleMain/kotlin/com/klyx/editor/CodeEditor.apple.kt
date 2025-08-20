package com.klyx.editor

import androidx.compose.runtime.Composable

@Composable
@ExperimentalCodeEditorApi
actual fun CodeEditor(
    state: CodeEditorState,
    modifier: androidx.compose.ui.Modifier,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    fontSize: androidx.compose.ui.unit.TextUnit,
    editable: Boolean,
    pinLineNumber: Boolean,
    language: String?
) {
}
