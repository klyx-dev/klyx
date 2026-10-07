package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Provides information about the context in which an inline completion was
 * requested.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlineCompletionContext)
 *
 * @since 3.18.0
 */
@Serializable
data class InlineCompletionContext(
    /**
     * Describes how the inline completion was triggered.
     */
    val triggerKind: InlineCompletionTriggerKind,

    /**
     * Provides information about the currently selected item in the
     * autocomplete widget if it is visible.
     *
     * If set, provided inline completions must extend the text of the
     * selected item and use the same range, otherwise they are not shown as
     * preview.
     * As an example, if the document text is `console.` and the selected item
     * is `.log` replacing the `.` in the document, the inline completion must
     * also replace `.` and start with `.log`, for example `.log()`.
     *
     * Inline completion providers are requested again whenever the selected
     * item changes.
     */
    val selectedCompletionInfo: SelectedCompletionInfo?
)

/**
 * Describes how an [inline completion provider](InlineCompletionItemProvider) was triggered.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlineCompletionTriggerKind)
 *
 * @since 3.18.0
 */
@Serializable
@JvmInline
value class InlineCompletionTriggerKind private constructor(private val value: Int) {
    companion object {
        /**
         * Completion was triggered explicitly by a user gesture.
         * Return multiple completion items to enable cycling through them.
         */
        val Invoked = InlineCompletionTriggerKind(1)

        /**
         * Completion was triggered automatically while editing.
         * It is sufficient to return a single completion item in this case.
         */
        val Automatic = InlineCompletionTriggerKind(2)
    }
}

/**
 * Describes the currently selected completion item.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#selectedCompletionInfo)
 *
 * @since 3.18.0
 */
@Serializable
data class SelectedCompletionInfo(
    /**
     * The range that will be replaced if this completion item is accepted.
     */
    val range: Range,

    /**
     * The text the range will be replaced with if this completion is
     * accepted.
     */
    val text: String
)
