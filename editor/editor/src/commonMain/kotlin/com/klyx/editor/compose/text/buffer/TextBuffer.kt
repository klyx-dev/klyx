package com.klyx.editor.compose.text.buffer

import kotlinx.serialization.Serializable

@Serializable
internal data class TextBuffer(
    @Serializable(with = StringBuilderSerializer::class)
    var buffer: StringBuilder,
    var lineStarts: MutableList<Int>
)

internal fun TextBuffer() = TextBuffer(StringBuilder(), mutableListOf(0))
