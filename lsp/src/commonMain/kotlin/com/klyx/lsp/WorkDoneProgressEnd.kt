package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workDoneProgressEnd)
 */
@Serializable
data class WorkDoneProgressEnd(
    /**
     * Optional, a final message indicating to for example indicate the outcome
     * of the operation.
     */
    var message: String? = null
) : WorkDoneProgressNotification {
    override val kind = WorkDoneProgressKind.End
}
