package com.klyx.lsp

import com.klyx.lsp.types.DocumentUri
import kotlinx.serialization.Serializable

/**
 * A previous result ID in a workspace pull request.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#previousResultId)
 *
 * @since 3.17.0
 */
@Serializable
data class PreviousResultId(
    /**
     * The URI for which the client knows a
     * result ID.
     */
    val uri: DocumentUri,

    /**
     * The value of the previous result ID.
     */
    val value: String
)
