package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Contains additional information about the context in which a completion
 * request is triggered.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#completionContext)
 */
@Serializable
data class CompletionContext(
    /**
     * How the completion was triggered.
     */
    val triggerKind: CompletionTriggerKind,

    /**
     * The trigger character (a single character) that
     * has triggered code complete. Is undefined if
     * [triggerKind] !== [CompletionTriggerKind.TriggerCharacter]
     */
    val triggerCharacter: String? = null
)

/**
 * How a completion was triggered.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#completionTriggerKind)
 */
@Serializable
@JvmInline
value class CompletionTriggerKind private constructor(private val value: Int) {
    companion object {
        /**
         * Completion was triggered by typing an identifier (automatic code
         * complete), manual invocation (e.g. Ctrl+Space) or via API.
         */
        val Invoked = CompletionTriggerKind(1)

        /**
         * Completion was triggered by a trigger character specified by
         * the `triggerCharacters` properties of the
         * `CompletionRegistrationOptions`.
         */
        val TriggerCharacter = CompletionTriggerKind(2)

        /**
         * Completion was re-triggered as the current completion list is incomplete.
         */
        val TriggerForIncompleteCompletions = CompletionTriggerKind(3)
    }
}
