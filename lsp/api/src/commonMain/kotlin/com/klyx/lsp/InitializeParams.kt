package com.klyx.lsp

import com.klyx.lsp.capabilities.ClientCapabilities
import com.klyx.lsp.types.DocumentUri
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#initializeParams)
 */
@Serializable
data class InitializeParams(
    /**
     * The capabilities provided by the client (editor or tool)
     */
    var capabilities: ClientCapabilities = ClientCapabilities(),

    /**
     * The process Id of the parent process that started the server. Is null if
     * the process has not been started by another process. If the parent
     * process is not alive then the server should exit (see exit notification)
     * its process.
     */
    var processId: Int? = null,

    /**
     * Information about the client
     *
     * @since 3.15.0
     */
    var clientInfo: ClientInfo? = null,

    /**
     * The locale the client is currently showing the user interface
     * in. This must not necessarily be the locale of the operating
     * system.
     *
     * Uses IETF language tags as the value's syntax
     * (See [https://en.wikipedia.org/wiki/IETF_language_tag](https://en.wikipedia.org/wiki/IETF_language_tag))
     *
     * @since 3.16.0
     */
    var locale: String? = null,

    /**
     * The rootPath of the workspace. Is null
     * if no folder is open.
     *
     * @deprecated in favour of `rootUri`.
     */
    @Deprecated("use `rootUri` instead", ReplaceWith("rootUri"))
    var rootPath: String? = null,

    /**
     * The rootUri of the workspace. Is null if no
     * folder is open. If both `rootPath` and `rootUri` are set
     * `rootUri` wins.
     */
    @Deprecated("use `workspaceFolders` instead", ReplaceWith("workspaceFolders"))
    var rootUri: DocumentUri? = null,

    /**
     * User provided initialization options.
     */
    var initializationOptions: JsonElement? = null,

    /**
     * The initial trace setting. If omitted trace is disabled ([TraceValue.Off]).
     */
    var trace: TraceValue? = null,

    /**
     * The workspace folders configured in the client when the server starts.
     * This property is only available if the client supports workspace folders.
     * It can be `null` if the client supports workspace folders but none are
     * configured.
     *
     * @since 3.6.0
     */
    var workspaceFolders: List<WorkspaceFolder>? = null,
) : WorkDoneProgressParams()

/**
 * Information about the client
 *
 * @since 3.15.0
 */
@Serializable
data class ClientInfo(
    /**
     * The name of the client as defined by the client.
     */
    val name: String,

    /**
     * The client's version as defined by the client.
     */
    var version: String? = null
)
