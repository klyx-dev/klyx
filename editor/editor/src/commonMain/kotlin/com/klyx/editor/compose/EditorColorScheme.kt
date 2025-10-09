package com.klyx.editor.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class EditorColorScheme(
    val background: Color,
    val foreground: Color,
    val lineNumberBackground: Color = background,
    val lineNumber: Color = foreground,
    val lineDivider: Color = foreground.copy(alpha = 0.2f),
    val currentLineBackground: Color = foreground.copy(alpha = 0.1f),
    val cursor: Color = foreground,
    val selectionBackground: Color = foreground.copy(alpha = 0.2f),
    val selectionForeground: Color = background,
    val scrollbar: Color = foreground.copy(alpha = 0.4f),
    val scrollbarThumb: Color = foreground
)

val DefaultEditorColorScheme = EditorColorScheme(
    background = Color.Black,
    foreground = Color.White,
)
