package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#traceValue)
 */
@JvmInline
@Serializable
value class TraceValue private constructor(private val value: String) {
    override fun toString() = value

    companion object {
        val Off = TraceValue("off")
        val Messages = TraceValue("messages")
        val Verbose = TraceValue("verbose")
    }
}
