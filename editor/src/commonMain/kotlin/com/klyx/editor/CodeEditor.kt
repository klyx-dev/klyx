package com.klyx.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
@ExperimentalCodeEditorApi
expect fun CodeEditor(
    state: CodeEditorState,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontFamily.Monospace,
    fontSize: TextUnit = 18.sp,
    editable: Boolean = true,
    pinLineNumber: Boolean = true,
    language: String? = null
)
