package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * The reason why code actions were requested.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#codeActionTriggerKind)
 *
 * @since 3.17.0
 */
@Serializable
@JvmInline
value class CodeActionTriggerKind private constructor(private val value: Int) {
    companion object {
        /**
         * Code actions were explicitly requested by the user or by an extension.
         */
        val Invoked = CodeActionTriggerKind(1)

        /**
         * Code actions were requested automatically.
         *
         * This typically happens when the current selection in a file changes,
         * but can also be triggered when file content changes.
         */
        val Automatic = CodeActionTriggerKind(2)
    }
}
