package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#messageActionItem)
 */
@Serializable
data class MessageActionItem(
    /**
     * A short title like 'Retry', 'Open Log' etc.
     */
    val title: String
)
