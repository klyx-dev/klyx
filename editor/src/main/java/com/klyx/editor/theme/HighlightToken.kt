package com.klyx.editor.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class HighlightToken(
    val start: Int,
    val end: Int,
    val color: Color
)
