package com.klyx.lsp.capabilities

import com.klyx.lsp.SymbolKind
import kotlinx.serialization.Serializable

/**
 * Specific capabilities for the [SymbolKind].
 */
@Serializable
data class SymbolKindCapabilities(
    /**
     * The symbol kind values the client supports. When this
     * property exists, the client also guarantees that it will
     * handle values outside its set gracefully and falls back
     * to a default value when unknown.
     *
     * If this property is not present, the client only supports
     * the symbol kinds from `File` to `Array` as defined in
     * the initial version of the protocol.
     */
    var valueSet: List<SymbolKind>? = null
)
