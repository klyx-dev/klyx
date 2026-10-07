package com.klyx.lsp

import com.klyx.lsp.types.DocumentUri
import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.OneOf
import kotlinx.serialization.Serializable

/**
 * A special workspace symbol that supports locations without a range.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workspaceSymbol)
 *
 * @since 3.17.0
 */
@Serializable
data class WorkspaceSymbol(
    /**
     * The name of this symbol.
     */
    val name: String,

    /**
     * The kind of this symbol.
     */
    val kind: SymbolKind,

    /**
     * Tags for this completion item.
     */
    val tags: List<SymbolTag>?,

    /**
     * The name of the symbol containing this symbol. This information is for
     * user interface purposes (e.g. to render a qualifier in the user interface
     * if necessary). It can't be used to re-infer a hierarchy for the document
     * symbols.
     */
    val containerName: String?,

    /**
     * The location of this symbol. Whether a server is allowed to
     * return a location without a range depends on the client
     * capability `workspace.symbol.resolveSupport`.
     *
     * @see SymbolInformation.location
     */
    val location: OneOf<Location, WorkspaceSymbolLocation>,

    /**
     * A data entry field that is preserved on a workspace symbol between a
     * workspace symbol request and a workspace symbol resolve request.
     */
    val data: LSPAny?
)

/**
 * A special workspace symbol that supports locations without a range
 */
@Serializable
data class WorkspaceSymbolLocation(val uri: DocumentUri)
