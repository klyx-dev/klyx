package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Diagnostic options.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#diagnosticOptions)
 *
 * @since 3.17.0
 */
@Serializable
data class DiagnosticOptions(
    /**
     * An optional identifier under which the diagnostics are
     * managed by the client.
     */
    val identifier: String? = null,

    /**
     * Whether the language has inter file dependencies, meaning that
     * editing code in one file can result in a different diagnostic
     * set in another file. Inter file dependencies are common for
     * most programming languages and typically uncommon for linters.
     */
    val interFileDependencies: Boolean,

    /**
     * The server provides support for workspace diagnostics as well.
     */
    val workspaceDiagnostics: Boolean,
) : WorkDoneProgressOptions()

/**
 * Diagnostic registration options.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#diagnosticRegistrationOptions)
 *
 * @since 3.17.0
 */
@Serializable
data class DiagnosticRegistrationOptions(
    /**
     * An optional identifier under which the diagnostics are
     * managed by the client.
     */
    val identifier: String? = null,

    /**
     * Whether the language has inter file dependencies, meaning that
     * editing code in one file can result in a different diagnostic
     * set in another file. Inter file dependencies are common for
     * most programming languages and typically uncommon for linters.
     */
    val interFileDependencies: Boolean,

    /**
     * The server provides support for workspace diagnostics as well.
     */
    val workspaceDiagnostics: Boolean,

    override var documentSelector: DocumentSelector? = null,
    override var id: String? = null
) : TextDocumentRegistrationOptions, StaticRegistrationOptions, WorkDoneProgressOptions()

