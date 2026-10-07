package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#referenceContext)
 */
@Serializable
data class ReferenceContext(
    /**
     * Include the declaration of the current symbol.
     */
    val includeDeclaration: Boolean
)
