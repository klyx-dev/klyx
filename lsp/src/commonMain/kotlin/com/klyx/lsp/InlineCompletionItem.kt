package com.klyx.lsp

import com.klyx.lsp.types.OneOf
import kotlinx.serialization.Serializable

/**
 * An inline completion item represents a text snippet that is proposed inline
 * to complete text that is being typed.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlineCompletionItem)
 *
 * @since 3.18.0
 */
@Serializable
data class InlineCompletionItem(
    /**
     * The text to replace the range with. Must be set.
     * Is used both for the preview and the accept operation.
     */
    val insertText: OneOf<String, StringValue>,

    /**
     * A text that is used to decide if this inline completion should be
     * shown. When `falsy`, the [insertText] is used.
     *
     * An inline completion is shown if the text to replace is a prefix of the
     * filter text.
     */
    val filterText: String?,

    /**
     * The range to replace.
     * Must begin and end on the same line.
     *
     * Prefer replacements over insertions to provide a better experience when
     * the user deletes typed text.
     */
    val range: Range?,

    /**
     * An optional [Command] that is executed *after* inserting this
     * completion.
     */
    val command: Command?
)
