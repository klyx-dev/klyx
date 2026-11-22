package com.klyx.lsp

import com.klyx.lsp.types.DocumentUri
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#callHierarchyItem)
 */
@Serializable
data class CallHierarchyItem(
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
     * A data entry field that is preserved between a call hierarchy prepare and
     * incoming calls or outgoing calls requests.
     */
    val data: JsonElement?
)
