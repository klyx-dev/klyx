package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#semanticTokensLegend)
 */
@Serializable
data class SemanticTokensLegend(
    /**
     * The token types a server uses.
     */
    val tokenTypes: List<String>,

    /**
     * The token modifiers a server uses.
     */
    val tokenModifiers: List<String>
)
