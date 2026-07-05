package com.klyx.lsp.capabilities

import com.klyx.lsp.WorkspaceEdit
import kotlinx.serialization.Serializable

/**
 * Workspace specific client capabilities.
 */
@Serializable
data class WorkspaceClientCapabilities(
    /**
     * The client supports applying batch edits
     * to the workspace by supporting the request
     * 'workspace/applyEdit'
     */
    var applyEdit: Boolean? = null,

    /**
     * Capabilities specific to [WorkspaceEdit]s
     */
    var workspaceEdit: WorkspaceEditCapabilities? = null,

    /**
     * Capabilities specific to the `workspace/didChangeConfiguration`
     * notification.
     */
    var didChangeConfiguration: DidChangeConfigurationCapabilities? = null,

    /**
     * Capabilities specific to the `workspace/didChangeWatchedFiles`
     * notification.
     */
    var didChangeWatchedFiles: DidChangeWatchedFilesCapabilities? = null,

    /**
     * Capabilities specific to the `workspace/symbol` request.
     */
    var symbol: WorkspaceSymbolCapabilities? = null,

    /**
     * Capabilities specific to the `workspace/executeCommand` request.
     */
    var executeCommand: ExecuteCommandCapabilities? = null,

    /**
     * The client has support for workspace folders.
     *
     * @since 3.6.0
     */
    var workspaceFolders: Boolean? = null,

    /**
     * The client supports `workspace/configuration` requests.
     *
     * @since 3.6.0
     */
    var configuration: Boolean? = null,

    /**
     * Capabilities specific to the semantic token requests scoped to the
     * workspace.
     *
     * @since 3.16.0
     */
    var semanticTokens: SemanticTokensWorkspaceClientCapabilities? = null,

    /**
     * Capabilities specific to the code lens requests scoped to the
     * workspace.
     *
     * @since 3.16.0
     */
    var codeLens: CodeLensWorkspaceClientCapabilities? = null,

    /**
     * The client has support for file requests/notifications.
     *
     * @since 3.16.0
     */
    var fileOperations: FileOperationsClientCapabilities? = null,

    /**
     * Client workspace capabilities specific to inline values.
     *
     * @since 3.17.0
     */
    var inlineValue: InlineValueWorkspaceClientCapabilities? = null,

    /**
     * Client workspace capabilities specific to inlay hints.
     *
     * @since 3.17.0
     */
    var inlayHint: InlayHintWorkspaceClientCapabilities? = null,

    /**
     * Client workspace capabilities specific to diagnostics.
     *
     * @since 3.17.0.
     */
    var diagnostics: DiagnosticWorkspaceClientCapabilities? = null
)
