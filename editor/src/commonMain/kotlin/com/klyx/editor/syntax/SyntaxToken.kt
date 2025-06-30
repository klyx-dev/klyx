package com.klyx.editor.syntax

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class SyntaxToken(
    val start: Int,
    val end: Int,
    val color: Color
)
