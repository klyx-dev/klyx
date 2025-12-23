package com.klyx.lsp.capabilities

import com.klyx.lsp.DiagnosticTag
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#publishDiagnosticsClientCapabilities)
 */
@Serializable
data class PublishDiagnosticsClientCapabilities(
    /**
     * Whether the clients accepts diagnostics with related information.
     */
    var relatedInformation: Boolean? = null,

    /**
     * Client supports the tag property to provide meta data about a diagnostic.
     * Clients supporting tags have to handle unknown tags gracefully.
     *
     * @since 3.15.0
     */
    var tagSupport: DiagnosticTagSupport? = null,

    /**
     * Whether the client interprets the version property of the
     * `textDocument/publishDiagnostics` notification's parameter.
     *
     * @since 3.15.0
     */
    var versionSupport: Boolean? = null,

    /**
     * Client supports a codeDescription property.
     *
     * @since 3.16.0
     */
    var codeDescriptionSupport: Boolean? = null,

    /**
     * Whether code action supports the `data` property which is
     * preserved between a `textDocument/publishDiagnostics` and
     * `textDocument/codeAction` request.
     *
     * @since 3.16.0
     */
    var dataSupport: Boolean? = null
)

/**
 * Client supports the tag property to provide meta data about a diagnostic.
 * Clients supporting tags have to handle unknown tags gracefully.
 *
 * @since 3.15.0
 */
@Serializable
data class DiagnosticTagSupport(
    /**
     * The tags supported by the client.
     */
    val valueSet: List<DiagnosticTag>
)
