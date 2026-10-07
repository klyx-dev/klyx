package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#semanticTokens)
 */
@Serializable
data class SemanticTokens(
    /**
     * An optional result ID. If provided and clients support delta updating,
     * the client will include the result ID in the next semantic token request.
     * A server can then, instead of computing all semantic tokens again, simply
     * send a delta.
     */
    val resultId: String?,

    /**
     * The actual tokens.
     */
    val data: List<UInt>,
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#semanticTokensPartialResult)
 */
@Serializable
data class SemanticTokensPartialResult(val data: List<UInt>)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#semanticTokensDelta)
 */
@Serializable
data class SemanticTokensDelta(
    val resultId: String?,
    /**
     * The semantic token edits to transform a previous result into a new
     * result.
     */
    val edits: List<SemanticTokensEdit>
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#semanticTokensEdit)
 */
@Serializable
data class SemanticTokensEdit(
    /**
     * The start offset of the edit.
     */
    val start: UInt,

    /**
     * The count of elements to remove.
     */
    val deleteCount: UInt,

    /**
     * The elements to insert.
     */
    var data: List<UInt>? = null,
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#semanticTokensDeltaPartialResult)
 */
@Serializable
data class SemanticTokensDeltaPartialResult(val edits: List<SemanticTokensEdit>)
