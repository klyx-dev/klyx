package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#range)
 */
@Serializable
data class Range(
    /**
     * The range's start position.
     */
    val start: Position,

    /**
     * The range's end position.
     */
    val end: Position
)
