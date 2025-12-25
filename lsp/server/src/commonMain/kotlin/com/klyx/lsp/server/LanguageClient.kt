package com.klyx.lsp.server

import com.klyx.lsp.ApplyWorkspaceEditParams
import com.klyx.lsp.ApplyWorkspaceEditResult
import com.klyx.lsp.ConfigurationItem
import com.klyx.lsp.ConfigurationParams
import com.klyx.lsp.Diagnostic
import com.klyx.lsp.LogMessageParams
import com.klyx.lsp.LogTraceParams
import com.klyx.lsp.MessageActionItem
import com.klyx.lsp.ProgressParams
import com.klyx.lsp.PublishDiagnosticsParams
import com.klyx.lsp.RegistrationParams
import com.klyx.lsp.ShowDocumentParams
import com.klyx.lsp.ShowDocumentResult
import com.klyx.lsp.ShowMessageParams
import com.klyx.lsp.ShowMessageRequestParams
import com.klyx.lsp.TextDocumentContentRefreshParams
import com.klyx.lsp.UnregistrationParams
import com.klyx.lsp.WorkDoneProgressCreateParams
import com.klyx.lsp.WorkspaceFolder
import com.klyx.lsp.capabilities.TextDocumentClientCapabilities
import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.LSPArray
import com.klyx.lsp.types.LSPObject
import com.klyx.lsp.types.OneOf

private fun unsupported(): Nothing = throw UnsupportedOperationException()

interface LanguageClient {
    /**
     * The base protocol also offers support to report progress in a generic fashion.
     * This mechanism can be used to report any kind of progress including [work done
     * progress](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workDoneProgress)
     * (usually used to report progress in the user interface using a progress bar)
     * and partial result progress to support streaming of results.
     *
     * @since 3.15.0
     */
    suspend fun notifyProgress(params: ProgressParams) {
        unsupported()
    }

    /**
     * A notification to log the trace of the server’s execution. The
     * amount and content of these notifications depends on the current
     * `trace` configuration. If `trace` is `'off'`, the server should not send
     * any `logTrace` notification. If `trace` is `'messages'`, the server should
     * not add the `'verbose'` field in the [LogTraceParams].
     *
     * `$/logTrace` should be used for systematic trace reporting. For single debugging messages,
     * the server should send [window/logMessage](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#window_logMessage)
     * notifications.
     */
    suspend fun logTrace(params: LogTraceParams) {
        unsupported()
    }

    /**
     * The `client/registerCapability` request is sent from the server to
     * the client to register for a new capability on the client side.
     * Not all clients need to support dynamic capability registration.
     * A client opts in via the `dynamicRegistration` property on the specific
     * client capabilities. A client can even provide dynamic registration for
     * capability A but not for capability B (see [TextDocumentClientCapabilities] as an example).
     *
     * The server must not register the same capability both statically through
     * the initialize result and dynamically for the same document selector.
     * If a server wants to support both static and dynamic registration,
     * it needs to check the client capability in the initialize request and
     * only register the capability statically if the client doesn’t support
     * dynamic registration for that capability.
     */
    suspend fun registerCapability(params: RegistrationParams) {
        unsupported()
    }

    /**
     * The `client/unregisterCapability` request is sent from the
     * server to the client to unregister a previously registered capability.
     */
    suspend fun unregisterCapability(params: UnregistrationParams) {
        unsupported()
    }

    /**
     * The `workspace/codeLens/refresh` request is sent from the server
     * to the client. Servers can use it to ask clients to refresh
     * the code lenses currently shown in editors. As a result the
     * client should ask the server to recompute the code lenses for
     * these editors. This is useful if a server detects a configuration
     * change which requires a re-calculation of all code lenses. Note
     * that the client still has the freedom to delay the re-calculation
     * of the code lenses if, for example, an editor is currently not visible.
     *
     * @since 3.16.0
     */
    suspend fun refreshCodeLenses() {
        unsupported()
    }

    /**
     * The `workspace/foldingRange/refresh` request is sent from the server
     * to the client. Servers can use it to ask clients to refresh the
     * folding ranges currently shown in editors. As a result, the
     * client should ask the server to recompute the folding ranges for
     * these editors. This is useful if a server detects a configuration
     * change which requires a re-calculation of all folding ranges. Note
     * that the client still has the freedom to delay the re-calculation
     * of the folding ranges if, for example, an editor is currently not visible.
     *
     * @since 3.18.0
     */
    suspend fun refreshFoldingRanges() {
        unsupported()
    }

    /**
     * The `workspace/semanticTokens/refresh` request is sent from the server to the client.
     * Servers can use it to ask clients to refresh the editors for which this server
     * provides semantic tokens. As a result, the client should ask the server to recompute
     * the semantic tokens for these editors. This is useful if a server detects a project
     * wide configuration change which requires a re-calculation of all semantic tokens.
     * Note that the client still has the freedom to delay the re-calculation of the semantic
     * tokens if, for example, an editor is currently not visible.
     *
     * @since 3.16.0
     */
    suspend fun refreshSemanticTokens() {
        unsupported()
    }

    /**
     * The `workspace/inlayHint/refresh` request is sent from the server to the client.
     * Servers can use it to ask clients to refresh the inlay hints currently shown in
     * editors. As a result, the client should ask the server to recompute the inlay hints
     * for these editors. This is useful if a server detects a configuration change which
     * requires a re-calculation of all inlay hints. Note that the client still has the
     * freedom to delay the re-calculation of the inlay hints if, for example, an
     * editor is currently not visible.
     *
     * @since 3.17.0
     */
    suspend fun refreshInlayHints() {
        unsupported()
    }

    /**
     * The `workspace/inlineValue/refresh` request is sent from the server to the client.
     * Servers can use it to ask clients to refresh the inline values currently shown in
     * editors. As a result, the client should ask the server to recompute the inline
     * values for these editors. This is useful if a server detects a configuration change
     * which requires a re-calculation of all inline values. Note that the client still has
     * the freedom to delay the re-calculation of the inline values if, for example, an editor
     * is currently not visible.
     *
     * @since 3.17.0
     */
    suspend fun refreshInlineValues() {
        unsupported()
    }

    /**
     * Diagnostics notifications are sent from the server to the client to signal results of validation runs.
     *
     * Diagnostics are “owned” by the server, so it is the server’s responsibility to clear them if necessary.
     *
     * When a file changes, it is the server’s responsibility to re-compute diagnostics and push them to the
     * client. If the computed set is empty, the server has to push the empty list to clear former diagnostics.
     * Newly pushed diagnostics always replace previously pushed diagnostics. There is no merging that happens
     * on the client side.
     *
     * @see Diagnostic
     */
    suspend fun publishDiagnostics(params: PublishDiagnosticsParams)

    /**
     * The `workspace/diagnostic/refresh` request is sent from the server to the client. Servers
     * can use it to ask clients to refresh all needed document and workspace diagnostics. This
     * is useful if a server detects a project wide configuration change which requires a re-calculation
     * of all diagnostics.
     */
    suspend fun refreshDiagnostics() {
        unsupported()
    }

    /**
     * The `workspace/configuration` request is sent from the server to the client to fetch configuration
     * settings from the client. The request can fetch several configuration settings in one roundtrip.
     * The order of the returned configuration settings correspond to the order of the passed
     * [ConfigurationItem]s (e.g. the first item in the response is the result for the first configuration
     * item in the params).
     *
     * @since 3.6.0
     */
    suspend fun configuration(params: ConfigurationParams): List<LSPAny>

    /**
     * The `workspace/workspaceFolders` request is sent from the server to the client to fetch the current
     * open list of workspace folders.
     *
     * @return `null` in the response if only a single file is open in the tool,
     *         an [`empty list`][emptyList] if a workspace is open but no folders are configured.
     *
     * @since 3.6.0
     */
    suspend fun workspaceFolders(): List<WorkspaceFolder>? = unsupported()

    /**
     * The `workspace/applyEdit` request is sent from the server to the client to modify resource on the client side.
     */
    suspend fun applyEdit(params: ApplyWorkspaceEditParams): ApplyWorkspaceEditResult = unsupported()

    /**
     * The `workspace/textDocumentContent/refresh` request is sent from the server to the client to refresh
     * the content of a specific text document.
     *
     * @since 3.18.0
     */
    suspend fun refreshTextDocumentContent(params: TextDocumentContentRefreshParams) {
        unsupported()
    }

    /**
     * The show message notification is sent from a server to a client to ask the client to display
     * a particular message in the user interface.
     */
    suspend fun showMessage(params: ShowMessageParams)

    /**
     * The show message request is sent from a server to a client to ask the client to display a
     * particular message in the user interface. In addition to the show message notification,
     * the request allows to pass actions and to wait for an answer from the client.
     */
    suspend fun showMessageRequest(params: ShowMessageRequestParams): MessageActionItem?

    /**
     * The show document request is sent from a server to a client to ask the client to display
     * a particular resource referenced by a URI in the user interface.
     */
    suspend fun showDocument(params: ShowDocumentParams): ShowDocumentResult = unsupported()

    /**
     * The log message notification is sent from the server to the client to ask the client to
     * log a particular message.
     */
    suspend fun logMessage(params: LogMessageParams)

    /**
     * The `window/workDoneProgress/create` request is sent from the server to the client to ask the
     * client to create a work done progress.
     *
     * @since 3.15.0
     */
    suspend fun createProgress(params: WorkDoneProgressCreateParams) {
        unsupported()
    }

    /**
     * The telemetry notification is sent from the server to the client to ask the client to log a
     * telemetry event. The protocol doesn’t specify the payload since no interpretation of the data
     * happens in the protocol. Most clients don’t even handle the event directly but forward them to
     * the extensions owing the corresponding server issuing the event.
     */
    suspend fun telemetryEvent(params: OneOf<LSPObject, LSPArray>)
}
