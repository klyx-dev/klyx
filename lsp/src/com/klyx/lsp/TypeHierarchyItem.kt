package com.klyx.lsp

import com.klyx.lsp.types.DocumentUri
import com.klyx.lsp.types.LSPAny
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#typeHierarchyItem)
 */
@Serializable
data class TypeHierarchyItem(
    /**
     * The name of this item.
     */
    val name: String,

    /**
     * The kind of this item.
     */
    val kind: SymbolKind,

    /**
     * Tags for this item.
     */
    val tags: List<SymbolTag>?,

    /**
     * More detail for this item, e.g. the signature of a function.
     */
    val detail: String?,

    /**
     * The resource identifier of this item.
     */
    val uri: DocumentUri,

    /**
     * The range enclosing this symbol not including leading/trailing whitespace
     * but everything else, e.g. comments and code.
     */
    val range: Range,

    /**
     * The range that should be selected and revealed when this symbol is being
     * picked, e.g. the name of a function. Must be contained by the [range].
     */
    val selectionRange: Range,

    /**
     * A data entry field that is preserved between a type hierarchy prepare and
     * supertypes or subtypes requests. It could also be used to identify the
     * type hierarchy in the server, helping improve the performance on
     * resolving supertypes and subtypes.
     */
    val data: LSPAny?
)
