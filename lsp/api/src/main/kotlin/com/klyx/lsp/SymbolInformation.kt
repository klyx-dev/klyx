package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Represents information about programming constructs like variables, classes,
 * interfaces etc.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#symbolInformation)
 */
@Deprecated("Use DocumentSymbol or WorkspaceSymbol instead")
@Serializable
data class SymbolInformation(
    /**
     * The name of this symbol.
     */
    val name: String,

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
     * The location of this symbol. The location's range is used by a tool
     * to reveal the location in the editor. If the symbol is selected in the
     * tool the range's start information is used to position the cursor. So
     * the range usually spans more then the actual symbol's name and does
     * normally include things like visibility modifiers.
     *
     * The range doesn't have to denote a node range in the sense of an abstract
     * syntax tree. It can therefore not be used to re-construct a hierarchy of
     * the symbols.
     */
    val location: Location,

    /**
     * The name of the symbol containing this symbol. This information is for
     * user interface purposes (e.g. to render a qualifier in the user interface
     * if necessary). It can't be used to re-infer a hierarchy for the document
     * symbols.
     */
    val containerName: String?
)
