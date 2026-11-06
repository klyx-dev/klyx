package com.klyx.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class CodeEditorController {
}

@Composable
fun rememberCodeEditorController(): CodeEditorController {
    return remember { CodeEditorController() }
}
