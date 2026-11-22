package com.klyx.lsp.capabilities

import com.klyx.lsp.DiagnosticTag
import kotlinx.serialization.Serializable

/**
 * Client capabilities specific to diagnostic pull requests.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#diagnosticClientCapabilities)
 *
 * @since 3.17.0
 */
@Serializable
data class DiagnosticClientCapabilities(
    /**
     * Whether implementation supports dynamic registration. If this is set to
     * `true`, the client supports the new
     * `(TextDocumentRegistrationOptions & StaticRegistrationOptions)`
     * return value for the corresponding server capability as well.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * Whether the clients supports related documents for document diagnostic
     * pulls.
     */
    var relatedDocumentSupport: Boolean? = null,

    /**
     * Whether the clients accepts diagnostics with related information.
     */
    var relatedInformation: Boolean? = null,

    /**
     * Client supports the tag property to provide meta data about a diagnostic.
     * Clients supporting tags have to handle unknown tags gracefully.
     */
    var tagSupport: ClientDiagnosticsTagOptions? = null,

    /**
     * Client supports a codeDescription property
     */
    var codeDescriptionSupport: Boolean? = null,

    /**
     * Whether the client supports `MarkupContent` in diagnostic messages.
     *
     * @since 3.18.0
     * @proposed
     */
    var markupMessageSupport: Boolean? = null,

    /**
     * Whether code action supports the `data` property which is
     * preserved between a `textDocument/publishDiagnostics` and
     * `textDocument/codeAction` request.
     */
    var dataSupport: Boolean? = null,
) : DynamicRegistrationCapabilities

@Serializable
data class ClientDiagnosticsTagOptions(
    /**
     * The tags supported by the client.
     */
    val valueSet: List<DiagnosticTag>
)
