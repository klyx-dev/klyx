package com.klyx.lsp

import com.klyx.lsp.SaveOptions
import com.klyx.lsp.types.OneOf
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#textDocumentSyncOptions)
 */
@Serializable
data class TextDocumentSyncOptions(
    /**
     * Open and close notifications are sent to the server. If omitted open
     * close notification should not be sent.
     */
    var openClose: Boolean? = null,

    /**
     * Change notifications are sent to the server. See
     * [TextDocumentSyncKind.None], [TextDocumentSyncKind.Full] and
     * [TextDocumentSyncKind.Incremental]. If omitted it defaults to
     * [TextDocumentSyncKind.None].
     */
    var change: TextDocumentSyncKind? = null,

    /**
     * If present will save notifications are sent to the server. If omitted
     * the notification should not be sent.
     */
    var willSave: Boolean? = null,

    /**
     * If present will save wait until requests are sent to the server. If
     * omitted the request should not be sent.
     */
    var willSaveWaitUntil: Boolean? = null,

    /**
     * If present save notifications are sent to the server. If omitted the
     * notification should not be sent.
     */
    var save: OneOf<Boolean, SaveOptions>? = null
)
