package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#partialResultParams)
 */
@Serializable
sealed interface PartialResultParams {
    /**
     * An optional token that a server can use to report partial results (e.g.
     * streaming) to the client.
     */
    var partialResultToken: ProgressToken?
}
