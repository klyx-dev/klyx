package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * A document highlight is a range inside a text document which deserves
 * special attention. Usually a document highlight is visualized by changing
 * the background color of its range.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentHighlight)
 */
@Serializable
data class DocumentHighlight(
    /**
     * The range this highlight applies to.
     */
    val range: Range,

    /**
     * The highlight kind, default is [DocumentHighlightKind.Text].
     */
    val kind: DocumentHighlightKind?
)

/**
 * A document highlight kind.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentHighlightKind)
 */
@Serializable
@JvmInline
value class DocumentHighlightKind private constructor(private val value: Int) {
    companion object {
        /**
         * A textual occurrence.
         */
        val Text = DocumentHighlightKind(1)

        /**
         * Read-access of a symbol, like reading a variable.
         */
        val Read = DocumentHighlightKind(2)

        /**
         * Write-access of a symbol, like writing to a variable.
         */
        val Write = DocumentHighlightKind(3)
    }
}
