package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#didOpenTextDocumentParams)
 */
@Serializable
data class DidOpenTextDocumentParams(
    /**
     * The document that was opened.
     */
    val textDocument: TextDocumentItem
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#didChangeTextDocumentParams)
 */
@Serializable
data class DidChangeTextDocumentParams(
    /**
     * The document that did change. The version number points
     * to the version after all provided content changes have
     * been applied.
     */
    val textDocument: VersionedTextDocumentIdentifier,

    /**
     * The actual content changes. The content changes describe single state
     * changes to the document. So if there are two content changes c1 (at
     * array index 0) and c2 (at array index 1) for a document in state S then
     * c1 moves the document from S to S' and c2 from S' to S''. So c1 is
     * computed on the state S and c2 is computed on the state S'.
     *
     * To mirror the content of a document using change events use the following
     * approach:
     * - start with the same initial content
     * - apply the 'textDocument/didChange' notifications in the order you
     *   receive them.
     * - apply the [TextDocumentContentChangeEvent]s in a single notification
     *   in the order you receive them.
     */
    val contentChanges: List<TextDocumentContentChangeEvent>
)

/**
 * An event describing a change to a text document. If only a text is provided
 * it is considered to be the full content of the document.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#textDocumentContentChangeEvent)
 */
@Serializable
data class TextDocumentContentChangeEvent(
    /**
     * The range of the document that changed.
     */
    val range: Range?,

    /**
     * The new text for the provided range.
     */
    val text: String,

    /**
     * The optional length of the range that got replaced.
     */
    @Deprecated("use range instead", ReplaceWith("range"))
    var rangeLength: UInt? = null
)

/**
 * The parameters send in a will save text document notification.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#willSaveTextDocumentParams)
 */
@Serializable
data class WillSaveTextDocumentParams(
    /**
     * The document that will be saved.
     */
    val textDocument: TextDocumentIdentifier,

    /**
     * The [TextDocumentSaveReason].
     */
    val reason: TextDocumentSaveReason
)

/**
 * Represents reasons why a text document is saved.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#textDocumentSaveReason)
 */
@JvmInline
@Serializable
value class TextDocumentSaveReason private constructor(private val value: Int) {
    companion object {
        /**
         * Manually triggered, e.g. by the user pressing save, by starting
         * debugging, or by an API call.
         */
        val Manual = TextDocumentSaveReason(1)

        /**
         * Automatic after a delay.
         */
        val AfterDelay = TextDocumentSaveReason(2)

        /**
         * When the editor lost focus.
         */
        val FocusOut = TextDocumentSaveReason(3)
    }
}

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#didSaveTextDocumentParams)
 */
@Serializable
data class DidSaveTextDocumentParams(
    /**
     * The document that was saved.
     */
    val textDocument: TextDocumentIdentifier,

    /**
     * Optional the content when saved. Depends on the includeText value
     * when the save notification was requested.
     */
    var text: String? = null
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#didCloseTextDocumentParams)
 */
@Serializable
data class DidCloseTextDocumentParams(
    /**
     * The document that was closed.
     */
    val textDocument: TextDocumentIdentifier
)
