package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * Client capabilities specific to regular expressions.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#regExp)
 */
@Serializable
data class RegularExpressionsCapabilities(
    /**
     * The engine's name.
     */
    val engine: String,

    /**
     * The engine's version.
     */
    val version: String?
)

