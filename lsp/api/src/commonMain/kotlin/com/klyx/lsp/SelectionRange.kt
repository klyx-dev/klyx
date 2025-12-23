package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#selectionRange)
 */
@Serializable
data class SelectionRange(
    /**
     * The [Range] of this selection range.
     */
    val range: Range,

    /**
     * The parent selection range containing this range.
     * Therefore, `parent.range` must contain [range].
     */
    val parent: SelectionRange?
)
