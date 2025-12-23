package com.klyx.lsp

import com.klyx.lsp.types.DocumentUri
import kotlinx.serialization.Serializable

/**
 * Represents a location inside a resource, such as a line inside a text file.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#location)
 */
@Serializable
data class Location(val uri: DocumentUri, val range: Range)

/**
 * Represents a link between a source and a target location.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#locationLink)
 */
@Serializable
data class LocationLink(
    /**
     * Span of the origin of this link.
     *
     * Used as the underlined span for mouse interaction. Defaults to the word
     * range at the mouse position.
     */
    var originSelectionRange: Range?,

    /**
     * The target resource identifier of this link.
     */
    val targetUri: DocumentUri,

    /**
     * The full target range of this link. If the target is, for example, a
     * symbol, then the target range is the range enclosing this symbol not
     * including leading/trailing whitespace but everything else like comments.
     * This information is typically used to highlight the range in the editor.
     */
    val targetRange: Range,

    /**
     * The range that should be selected and revealed when this link is being
     * followed, e.g., the name of a function. Must be contained by the
     * `targetRange`. See also `DocumentSymbol#range`
     */
    val targetSelectionRange: Range
) {
    constructor(
        targetUri: DocumentUri,
        targetRange: Range,
        targetSelectionRange: Range
    ) : this(
        originSelectionRange = null,
        targetUri,
        targetRange,
        targetSelectionRange
    )
}
