package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * The params sent in an open notebook document notification.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#didOpenNotebookDocumentParams)
 *
 * @since 3.17.0
 */
@Serializable
data class DidOpenNotebookDocumentParams(
    /**
     * The notebook document that got opened.
     */
    val notebookDocument: NotebookDocument,

    /**
     * The text documents that represent the content
     * of a notebook cell.
     */
    val cellTextDocuments: List<TextDocumentItem>
)

/**
 * The params sent in a change notebook document notification.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#didChangeNotebookDocumentParams)
 *
 * @since 3.17.0
 */
@Serializable
data class DidChangeNotebookDocumentParams(
    /**
     * The notebook document that did change. The version number points
     * to the version after all provided changes have been applied.
     */
    val notebookDocument: VersionedNotebookDocumentIdentifier,

    /**
     * The actual changes to the notebook document.
     *
     * The change describes a single state change to the notebook document,
     * so it moves a notebook document, its cells and its cell text document
     * contents from state S to S'.
     *
     * To mirror the content of a notebook using change events use the
     * following approach:
     * - start with the same initial content
     * - apply the 'notebookDocument/didChange' notifications in the order
     *   you receive them.
     */
    val change: NotebookDocumentChangeEvent
)

/**
 * The params sent in a save notebook document notification.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#didSaveNotebookDocumentParams)
 *
 * @since 3.17.0
 */
@Serializable
data class DidSaveNotebookDocumentParams(
    /**
     * The notebook document that got saved.
     */
    val notebookDocument: NotebookDocumentIdentifier
)

/**
 * The params sent in a close notebook document notification.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#didCloseNotebookDocumentParams)
 *
 * @since 3.17.0
 */
@Serializable
data class DidCloseNotebookDocumentParams(
    /**
     * The notebook document that got closed.
     */
    val notebookDocument: NotebookDocumentIdentifier,

    /**
     * The text documents that represent the content
     * of a notebook cell that got closed.
     */
    val cellTextDocuments: List<TextDocumentIdentifier>
)

/**
 * A change event for a notebook document.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#notebookDocumentChangeEvent)
 *
 * @since 3.17.0
 */
@Serializable
data class NotebookDocumentChangeEvent(
    /**
     * The changed meta data if any.
     */
    var metadata: JsonObject? = null,

    /**
     * Changes to cells.
     */
    var cells: NotebookDocumentChangeEventCells? = null
)

/**
 * Changes to cells.
 */
@Serializable
data class NotebookDocumentChangeEventCells(
    /**
     * Changes to the cell structure to add or
     * remove cells.
     */
    var structure: NotebookDocumentChangeEventCellStructure? = null,

    /**
     * Changes to notebook cells properties like its
     * kind, execution summary or metadata.
     */
    var data: List<NotebookCell>? = null,

    /**
     * Changes to the text content of notebook cells.
     */
    var textContent: List<NotebookDocumentChangeEventCellTextContent>? = null
)

/**
 * Changes to the text content of notebook cells.
 */
@Serializable
data class NotebookDocumentChangeEventCellTextContent(
    val document: VersionedTextDocumentIdentifier,
    val changes: List<TextDocumentContentChangeEvent>
)

/**
 * Changes to the cell structure to add or
 * remove cells.
 */
@Serializable
data class NotebookDocumentChangeEventCellStructure(
    /**
     * The change to the cell array.
     */
    val array: NotebookCellArrayChange,

    /**
     * Additional opened cell text documents.
     */
    var didOpen: List<TextDocumentItem>? = null,

    /**
     * Additional closed cell text documents.
     */
    var didClose: List<TextDocumentIdentifier>? = null
)

/**
 * A change describing how to move a [NotebookCell]
 * array from state S to S'.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#notebookCellArrayChange)
 *
 * @since 3.17.0
 */
@Serializable
data class NotebookCellArrayChange(
    /**
     * The start offset of the cell that changed.
     */
    val start: UInt,

    /**
     * The number of deleted cells.
     */
    val deleteCount: UInt,

    /**
     * The new cells, if any.
     */
    var cells: List<NotebookCell>? = null
)
