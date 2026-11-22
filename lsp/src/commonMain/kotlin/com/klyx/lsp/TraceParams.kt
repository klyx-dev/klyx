package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#setTrace)
 */
@Serializable
data class SetTraceParams(
    /**
     * The new value that should be assigned to the trace setting.
     */
    val value: TraceValue
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#logTrace)
 */
@Serializable
data class LogTraceParams(
    /**
     * The message to be logged.
     */
    val message: String,

    /**
     * Additional information that can be computed if the `trace` configuration
     * is set to [TraceValue.Verbose].
     */
    val verbose: String?
)
