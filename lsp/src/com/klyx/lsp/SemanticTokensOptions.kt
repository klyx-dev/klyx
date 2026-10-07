package com.klyx.lsp

import com.klyx.lsp.types.OneOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#semanticTokensOptions)
 */
@Serializable
data class SemanticTokensOptions(
    /**
     * The legend used by the server.
     */
    val legend: SemanticTokensLegend,

    /**
     * Server supports providing semantic tokens for a specific range
     * of a document.
     */
    var range: OneOf<Boolean, JsonElement>? = null,

    /**
     * Server supports providing semantic tokens for a full document.
     */
    var full: OneOf<Boolean, SemanticTokensOptionsFull>? = null
) : WorkDoneProgressOptions()

@Serializable
data class SemanticTokensOptionsFull(
    /**
     * The server supports deltas for full documents.
     */
    var delta: Boolean? = null
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#semanticTokensRegistrationOptions)
 */
@Serializable
data class SemanticTokensRegistrationOptions(
    /**
     * The legend used by the server.
     */
    val legend: SemanticTokensLegend,

    /**
     * Server supports providing semantic tokens for a specific range
     * of a document.
     */
    var range: OneOf<Boolean, JsonElement>? = null,

    /**
     * Server supports providing semantic tokens for a full document.
     */
    var full: OneOf<Boolean, SemanticTokensOptionsFull>? = null,
    override var documentSelector: DocumentSelector? = null,
    override var id: String? = null
) : TextDocumentRegistrationOptions, StaticRegistrationOptions, WorkDoneProgressOptions()
