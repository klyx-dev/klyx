package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workDoneProgressParams)
 */
@Serializable
sealed class WorkDoneProgressParams(
    /**
     * An optional token that a server can use to report work done progress.
     */
    open var workDoneToken: ProgressToken? = null
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workDoneProgressOptions)
 */
@Serializable
sealed class WorkDoneProgressOptions(open var workDoneProgress: Boolean? = null)
