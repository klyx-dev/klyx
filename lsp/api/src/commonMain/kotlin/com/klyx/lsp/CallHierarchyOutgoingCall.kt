package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#callHierarchyOutgoingCall)
 */
@Serializable
data class CallHierarchyOutgoingCall(
    /**
     * The item that is called.
     */
    val to: CallHierarchyItem,

    /**
     * The range at which this item is called. This is the range relative to
     * the caller, e.g., the item passed to `callHierarchy/outgoingCalls` request.
     */
    val fromRanges: List<Range>
)
