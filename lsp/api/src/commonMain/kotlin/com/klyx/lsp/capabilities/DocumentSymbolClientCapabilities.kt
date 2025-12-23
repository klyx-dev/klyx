package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentSymbolClientCapabilities)
 */
@Serializable
data class DocumentSymbolClientCapabilities(
    /**
     * Whether document symbol supports dynamic registration.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * Specific capabilities for the `SymbolKind` in the
     * `textDocument/documentSymbol` request.
     */
    var symbolKind: SymbolKindClientCapabilities? = null,

    /**
     * The client supports hierarchical document symbols.
     */
    var hierarchicalDocumentSymbolSupport: Boolean? = null,

    /**
     * The client supports tags on `SymbolInformation`. Tags are supported on
     * `DocumentSymbol` if `hierarchicalDocumentSymbolSupport` is set to true.
     * Clients supporting tags have to handle unknown tags gracefully.
     *
     * @since 3.16.0
     */
    var tagSupport: SymbolTagSupportClientCapabilities? = null,

    /**
     * The client supports an additional label presented in the UI when
     * registering a document symbol provider.
     *
     * @since 3.16.0
     */
    var labelSupport: Boolean? = null
) : DynamicRegistrationCapabilities
