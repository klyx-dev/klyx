package com.klyx.editor.compose.text

import kotlinx.serialization.Serializable

// statistical text information
@Serializable
data class TextCounter(
    val lineBreakCount: Int,
    val firstLineLength: Int,
    val lastLineLength: Int,
    val lineBreak: LineBreak
)
