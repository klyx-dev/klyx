package com.klyx.lsp.server

import com.klyx.lsp.InitializeParams
import com.klyx.lsp.InitializeResult
import com.klyx.lsp.SetTraceParams
import com.klyx.lsp.ErrorCodes.InvalidRequest
import com.klyx.lsp.WorkDoneProgressCancelParams

/**
 * Interface for implementations of
 * [https://github.com/Microsoft/vscode-languageserver-protocol](https://github.com/Microsoft/vscode-languageserver-protocol)
 *
 * @author Vivek
 */
interface LanguageServer {
    /**
     * Provides access to the `textDocument` services.
     */
    val textDocument: TextDocumentService

    /**
     * Provides access to the `workspace` services.
     */
    val workspace: WorkspaceService

    /**
     * Provides access to the `notebookDocument` services.
     *
     * @since 3.17.0
     */
    val notebookDocument: NotebookDocumentService

    /**
     * The initialize request is sent as the first request from the client to the server.
     * If the server receives a request or notification before the `initialize` request,
     * it should act as follows:
     *
     * - For a request, the response should be an error with `code: -32002`.
     *   The message can be picked by the server.
     * - Notifications should be dropped, except for the exit notification.
     *   This will allow the exit of a server without an initialize request.
     *
     * Until the server has responded to the `initialize` request with an [InitializeResult],
     * the client must not send any additional requests or notifications to the server.
     * In addition the server is not allowed to send any requests or notifications to the
     * client until it has responded with an [InitializeResult], with the exception that
     * during the `initialize` request the server is allowed to send the notifications
     * `window/showMessage`, `window/logMessage` and
     * [`telemetry/event`](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#telemetry_event)
     * as well as the `window/showMessageRequest` request to the client. In case
     * the client sets up a progress token in the initialize params (e.g. property `workDoneToken`) the server
     * is also allowed to use that token (and only that token) using the
     * [`$/progress`](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workDoneProgressBegin)
     * notification sent from the server to the client.
     *
     * The `initialize` request may only be sent once.
     */
    suspend fun initialize(params: InitializeParams): InitializeResult

    /**
     * The initialized notification is sent from the client to the server
     * after the client received the result of the `initialize` request,
     * but before the client is sending any other request or notification
     * to the server. The server can use the `initialized` notification, for
     * example, to dynamically register capabilities. The `initialized`
     * notification may only be sent once.
     */
    suspend fun initialized(params: InitializeParams)

    /**
     * A notification that should be used by the client to modify the trace setting of the server.
     */
    suspend fun setTrace(params: SetTraceParams)

    /**
     * The shutdown request is sent from the client to the server.
     * It asks the server to shut down, but to not exit (otherwise
     * the response might not be delivered correctly to the client).
     * There is a separate exit notification that asks the server to
     * exit. Clients must not send any requests or notifications other
     * than [exit] to a server to which they have sent a shutdown request.
     * Clients should also wait with sending the [exit] notification until
     * they have received a response from the `shutdown` request.
     *
     * If a server receives requests after a shutdown request
     * those requests should error with [InvalidRequest].
     */
    suspend fun shutdown()

    /**
     * A notification to ask the server to exit its process.
     */
    suspend fun exit()

    /**
     * The `window/workDoneProgress/cancel` notification is sent from the client to the server to
     * cancel a progress initiated on the server side using [`window/workDoneProgress/create`][LanguageClient.createProgress].
     * The progress need not be marked as cancellable to be cancelled and a client may cancel
     * a progress for any number of reasons: in case of error, reloading a workspace etc.
     *
     * @since 3.15.0
     */
    suspend fun cancelProgress(params: WorkDoneProgressCancelParams)
}
