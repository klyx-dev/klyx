package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#textDocumentSyncClientCapabilities)
 */
@Serializable
data class TextDocumentSyncClientCapabilities(
    /**
     * Whether text document synchronization supports dynamic registration.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * The client supports sending will save notifications.
     */
    var willSave: Boolean? = null,

    /**
     * The client supports sending a will save request and
     * waits for a response providing text edits which will
     * be applied to the document before it is saved.
     */
    var willSaveWaitUntil: Boolean? = null,

    /**
     * The client supports did save notifications.
     */
    var didSave: Boolean? = null,
) : DynamicRegistrationCapabilities
