package com.klyx.editor

import androidx.compose.runtime.Immutable

@Immutable
data class CursorState(
    val right: Int = 0,
    val left: Int = 0,
    val rightLine: Int = 0,
    val rightColumn: Int = 0,
    val leftLine: Int = 0,
    val leftColumn: Int = 0,
    val isSelected: Boolean = false,
)
