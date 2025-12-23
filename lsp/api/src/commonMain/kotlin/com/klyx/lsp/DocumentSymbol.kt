package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Represents programming constructs like variables, classes, interfaces etc.
 * that appear in a document. Document symbols can be hierarchical and they
 * have two ranges: one that encloses their definition and one that points to
 * their most interesting range, e.g. the range of an identifier.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentSymbol)
 */
@Serializable
data class DocumentSymbol(
    /**
     * The name of this symbol. Will be displayed in the user interface and
     * therefore must not be an empty string or a string only consisting of
     * white spaces.
     */
    val name: String,

    /**
     * More detail for this symbol, e.g. the signature of a function.
     */
    val detail: String?,

    /**
     * The kind of this symbol.
     */
    val kind: SymbolKind,

    /**
     * Tags for this document symbol.
     *
     * @since 3.16.0
     */
    val tags: List<SymbolTag>?,

    /**
     * Indicates if this symbol is deprecated.
     */
    @Deprecated("Use tags instead", ReplaceWith("tags"))
    val deprecated: Boolean?,

    /**
     * The range enclosing this symbol not including leading/trailing whitespace
     * but everything else, like comments. This information is typically used to
     * determine if the client's cursor is inside the symbol to reveal the
     * symbol in the UI.
     */
    val range: Range,

    /**
     * The range that should be selected and revealed when this symbol is being
     * picked, e.g. the name of a function. Must be contained by the `range`.
     */
    val selectionRange: Range,

    /**
     * Children of this symbol, e.g. properties of a class.
     */
    val children: List<DocumentSymbol>?
)
