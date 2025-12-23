package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#tokenFormat)
 */
@Serializable
@JvmInline
value class TokenFormat private constructor(private val value: String) {
    override fun toString() = value

    companion object {
        val Relative = TokenFormat("relative")
    }
}
