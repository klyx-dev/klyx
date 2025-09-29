package com.klyx.editor.compose.text.buffer

import kotlinx.serialization.Serializable

@Serializable
internal data class VisitedLine(
    var lineNumber: Int,
    var value: String
)
