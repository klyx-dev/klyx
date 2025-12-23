package com.klyx.lsp.capabilities

import com.klyx.lsp.SymbolTag
import kotlinx.serialization.Serializable

/**
 * @since 3.16.0
 */
@Serializable
data class SymbolTagSupportClientCapabilities(
    /**
     * The tags supported by the client.
     */
    val valueSet: List<SymbolTag>
)
