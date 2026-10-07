package com.klyx.lsp

import com.klyx.lsp.types.DocumentUri
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.URI
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.jvm.JvmInline

/**
 * A notebook document.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#notebookDocument)
 *
 * @since 3.17.0
 */
@Serializable
data class NotebookDocument(
    /**
     * The notebook document's URI.
     */
    val uri: URI,

    /**
     * The type of the notebook.
     */
    val type: String,

    /**
     * The version number of this document (it will increase after each
     * change, including undo/redo).
     */
    val version: Int,

    /**
     * The cells of a notebook.
     */
    val cells: List<NotebookCell>,

    /**
     * Additional metadata stored with the notebook
     * document.
     */
    val metadata: JsonObject? = null,
)

/**
 * A notebook cell.
 *
 * A cell's document URI must be unique across ALL notebook
 * cells and can therefore be used to uniquely identify a
 * notebook cell or the cell's text document.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#notebookCell)
 *
 * @since 3.17.0
 */
@Serializable
data class NotebookCell(
    /**
     * The cell's kind.
     */
    val kind: NotebookCellKind,

    /**
     * The URI of the cell's text document
     * content.
     */
    val document: DocumentUri,

    /**
     * Additional metadata stored with the cell.
     */
    var metadata: JsonObject? = null,

    /**
     * Additional execution summary information
     * if supported by the client.
     */
    var executionSummary: ExecutionSummary? = null
)

/**
 * A notebook cell kind.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#notebookCellKind)
 *
 * @since 3.17.0
 */
@JvmInline
@Serializable
value class NotebookCellKind private constructor(private val value: Int) {
    companion object {
        /**
         * A markup-cell is a formatted source that is used for display.
         */
        val Markup = NotebookCellKind(1)

        /**
         * A code-cell is source code.
         */
        val Code = NotebookCellKind(2)
    }
}

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#executionSummary)
 */
@Serializable
data class ExecutionSummary(
    /**
     * A strictly monotonically increasing value
     * indicating the execution order of a cell
     * inside a notebook.
     */
    val executionOrder: UInt,

    /**
     * Whether the execution was successful or
     * not if known by the client.
     */
    val success: Boolean?
)

/**
 * A notebook cell text document filter denotes a cell text
 * document by different properties.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#notebookCellTextDocumentFilter)
 *
 * @since 3.17.0
 */
@Serializable
data class NotebookCellTextDocumentFilter(
    /**
     * A filter that matches against the notebook
     * containing the notebook cell. If a string
     * value is provided, it matches against the
     * notebook type. '*' matches every notebook.
     */
    val notebook: OneOf<String, NotebookDocumentFilter>,

    /**
     * A language ID like `python`.
     *
     * Will be matched against the language ID of the
     * notebook cell document. '*' matches every language.
     */
    val language: String?
)

/**
 * A versioned notebook document identifier.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#versionedNotebookDocumentIdentifier)
 *
 * @since 3.17.0
 */
@Serializable
data class VersionedNotebookDocumentIdentifier(
    /**
     * The version number of this notebook document.
     */
    val version: Int,

    /**
     * The notebook document's URI.
     */
    val uri: URI
)

/**
 * A literal to identify a notebook document in the client.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#notebookDocumentIdentifier)
 *
 * @since 3.17.0
 */
@Serializable
data class NotebookDocumentIdentifier(
    /**
     * The notebook document's URI.
     */
    val uri: URI
)
