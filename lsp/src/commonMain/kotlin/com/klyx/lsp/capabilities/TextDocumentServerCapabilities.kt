package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * Text document specific server capabilities.
 *
 * @since 3.18.0
 */
@Serializable
data class TextDocumentServerCapabilities(
    /**
     * Capabilities specific to the diagnostic pull model.
     *
     * @since 3.18.0
     */
    var diagnostic: TextDocumentDiagnosticServerCapabilities? = null
)

/**
 * Capabilities specific to the diagnostic pull model.
 *
 * @since 3.18.0
 */
@Serializable
data class TextDocumentDiagnosticServerCapabilities(
    /**
     * Whether the server supports `MarkupContent` in diagnostic messages.
     *
     * @since 3.18.0
     * @proposed
     */
    var markupMessageSupport: Boolean? = null
)
