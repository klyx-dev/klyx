package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Completion options.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#completionOptions)
 */
@Serializable
data class CompletionOptions(
    /**
     * Most tools trigger completion request automatically without explicitly
     * requesting it using a keyboard shortcut (e.g., Ctrl+Space). Typically they
     * do so when the user starts to type an identifier. For example, if the user
     * types `c` in a JavaScript file, code complete will automatically pop up and
     * present `console` besides others as a completion item. Characters that
     * make up identifiers don't need to be listed here.
     *
     * If code complete should automatically be triggered on characters not being
     * valid inside an identifier (for example, `.` in JavaScript), list them in
     * `triggerCharacters`.
     */
    val triggerCharacters: List<String>?,

    /**
     * The list of all possible characters that commit a completion. This field
     * can be used if clients don't support individual commit characters per
     * completion item. See client capability
     * `completion.completionItem.commitCharactersSupport`.
     *
     * If a server provides both `allCommitCharacters` and commit characters on
     * an individual completion item, the ones on the completion item win.
     *
     * @since 3.2.0
     */
    val allCommitCharacters: List<String>?,

    /**
     * The server provides support to resolve additional
     * information for a completion item.
     */
    val resolveProvider: Boolean?,

    /**
     * The server supports the following `CompletionItem` specific
     * capabilities.
     *
     * @since 3.17.0
     */
    val completionItem: CompletionItemOptions?,
) : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#completionRegistrationOptions)
 */
@Serializable
data class CompletionRegistrationOptions(
    /**
     * Most tools trigger completion request automatically without explicitly
     * requesting it using a keyboard shortcut (e.g., Ctrl+Space). Typically they
     * do so when the user starts to type an identifier. For example, if the user
     * types `c` in a JavaScript file, code complete will automatically pop up and
     * present `console` besides others as a completion item. Characters that
     * make up identifiers don't need to be listed here.
     *
     * If code complete should automatically be triggered on characters not being
     * valid inside an identifier (for example, `.` in JavaScript), list them in
     * `triggerCharacters`.
     */
    val triggerCharacters: List<String>?,

    /**
     * The list of all possible characters that commit a completion. This field
     * can be used if clients don't support individual commit characters per
     * completion item. See client capability
     * `completion.completionItem.commitCharactersSupport`.
     *
     * If a server provides both `allCommitCharacters` and commit characters on
     * an individual completion item, the ones on the completion item win.
     *
     * @since 3.2.0
     */
    val allCommitCharacters: List<String>?,

    /**
     * The server provides support to resolve additional
     * information for a completion item.
     */
    val resolveProvider: Boolean?,

    /**
     * The server supports the following `CompletionItem` specific
     * capabilities.
     *
     * @since 3.17.0
     */
    val completionItem: CompletionItemOptions?,
    override var documentSelector: DocumentSelector? = null
) : WorkDoneProgressOptions(), TextDocumentRegistrationOptions

/**
 * The server supports the following `CompletionItem` specific
 * capabilities.
 *
 * @since 3.17.0
 */
@Serializable
data class CompletionItemOptions(
    /**
     * The server has support for completion item label
     * details (see also `CompletionItemLabelDetails`) when receiving
     * a completion item in a resolve call.
     *
     * @since 3.17.0
     */
    val labelDetailsSupport: Boolean?
)
