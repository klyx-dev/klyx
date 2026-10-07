package com.klyx.lsp

import com.klyx.lsp.types.OneOfThree
import kotlinx.serialization.Serializable

/**
 * The result of a hover request.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#hover)
 */
@Serializable
data class Hover(
    /**
     * The hover's content.
     */
    @Suppress("DEPRECATION")
    val contents: OneOfThree<MarkedString, List<MarkedString>, MarkupContent>,

    /**
     * An optional range is a range inside a text document
     * that is used to visualize a hover, e.g. by changing the background color.
     */
    val range: Range?
)
