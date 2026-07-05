package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#failureHandlingKind)
 */
@JvmInline
@Serializable
value class FailureHandlingKind private constructor(private val value: String) {
    override fun toString() = value

    companion object {
        /**
         * Applying the workspace change is simply aborted if one of the changes
         * provided fails. All operations executed before the failing operation
         * stay executed.
         */
        val Abort = FailureHandlingKind("abort")

        /**
         * All operations are executed transactionally. That means they either all
         * succeed or no changes at all are applied to the workspace.
         */
        val Transactional = FailureHandlingKind("transactional")

        /**
         * If the workspace edit contains only textual file changes they are
         * executed transactionally. If resource changes (create, rename or delete
         * file) are part of the change the failure handling strategy is abort.
         */
        val TextOnlyTransactional = FailureHandlingKind("textOnlyTransactional")

        /**
         * The client tries to undo the operations already executed. But there is no
         * guarantee that this is succeeding.
         */
        val Undo = FailureHandlingKind("undo")
    }
}
