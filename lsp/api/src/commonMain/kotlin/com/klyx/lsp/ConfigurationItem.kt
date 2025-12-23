package com.klyx.lsp

import com.klyx.lsp.types.URI
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#configurationItem)
 */
@Serializable
data class ConfigurationItem(
    /**
     * The scope to get the configuration section for.
     */
    val scopeUri: URI?,

    /**
     * The configuration section asked for.
     */
    val section: String?
)
