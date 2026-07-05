package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#callHierarchyIncomingCall)
 */
@Serializable
data class CallHierarchyIncomingCall(
    /**
     * The item that makes the call.
     */
    val from: CallHierarchyItem,

    /**
     * The ranges at which the calls appear. This is relative to the caller
     * denoted by [from].
     */
    val fromRanges: List<Range>
)
