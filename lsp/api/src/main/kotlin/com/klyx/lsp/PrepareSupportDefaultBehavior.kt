package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#prepareSupportDefaultBehavior)
 */
@Serializable
@JvmInline
value class PrepareSupportDefaultBehavior private constructor(private val value: Int) {
    companion object {
        /**
         * The client's default behavior is to select the identifier
         * according to the language's syntax rule.
         */
        val Identifier = PrepareSupportDefaultBehavior(1)
    }
}
