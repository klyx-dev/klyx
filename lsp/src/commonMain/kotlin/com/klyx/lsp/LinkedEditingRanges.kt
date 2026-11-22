package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#linkedEditingRanges)
 */
@Serializable
data class LinkedEditingRanges(
    /**
     * A list of ranges that can be renamed together. The ranges must have
     * identical length and contain identical text content. The ranges cannot
     * overlap.
     */
    val ranges: List<Range>,

    /**
     * An optional word pattern (regular expression) that describes valid
     * contents for the given ranges. If no pattern is provided, the client
     * configuration's word pattern will be used.
     */
    val wordPattern: String?
)
