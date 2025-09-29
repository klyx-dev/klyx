package com.klyx.editor.compose

import androidx.compose.ui.graphics.Color

data class EditorColorScheme(
    val background: Color,
    val foreground: Color,
)

val DefaultEditorColorScheme = EditorColorScheme(
    background = Color.Black,
    foreground = Color.White,
)
