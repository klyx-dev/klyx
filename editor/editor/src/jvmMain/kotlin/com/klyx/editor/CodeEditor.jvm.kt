package com.klyx.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit

@Composable
@ExperimentalCodeEditorApi
actual fun CodeEditor(
    state: CodeEditorState,
    modifier: Modifier,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    editable: Boolean,
    pinLineNumber: Boolean,
    language: String?
) {
    // TODO
}
