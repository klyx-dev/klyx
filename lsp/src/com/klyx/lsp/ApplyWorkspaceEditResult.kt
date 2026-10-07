package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#applyWorkspaceEditResult)
 */
@Serializable
data class ApplyWorkspaceEditResult(
    /**
     * Indicates whether the edit was applied or not.
     */
    val applied: Boolean,

    /**
     * An optional textual description for why the edit was not applied.
     * This may be used by the server for diagnostic logging or to provide
     * a suitable error for a request that triggered the edit.
     */
    var failureReason: String? = null,

    /**
     * Depending on the client's failure handling strategy, `failedChange`
     * might contain the index of the change that failed. This property is
     * only available if the client signals a `failureHandling` strategy
     * in its client capabilities.
     */
    var failedChange: UInt? = null
)
