package com.klyx.lsp

import com.klyx.lsp.types.DocumentUri
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#textDocumentIdentifier)
 */
@Serializable
sealed interface TextDocumentIdentifier {
    /**
     * The text document's URI.
     */
    val uri: DocumentUri
}

@Serializable
internal data class TextDocumentIdentifierImpl(override val uri: DocumentUri) : TextDocumentIdentifier

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#textDocumentIdentifier)
 *
 * @param uri The text document's URI.
 */
fun TextDocumentIdentifier(uri: DocumentUri): TextDocumentIdentifier = TextDocumentIdentifierImpl(uri)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#versionedTextDocumentIdentifier)
 */
@Serializable
data class VersionedTextDocumentIdentifier(
    override val uri: DocumentUri,

    /**
     * The version number of this document.
     *
     * The version number of a document will increase after each change,
     * including undo/redo. The number doesn't need to be consecutive.
     */
    val version: Int
) : TextDocumentIdentifier

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#optionalVersionedTextDocumentIdentifier)
 */
@Serializable
data class OptionalVersionedTextDocumentIdentifier(
    override val uri: DocumentUri,

    /**
     * The version number of this document. If an optional versioned text document
     * identifier is sent from the server to the client and the file is not
     * open in the editor (the server has not received an open notification
     * before) the server can send `null` to indicate that the version is
     * known and the content on disk is the master (as specified with document
     * content ownership).
     *
     * The version number of a document will increase after each change,
     * including undo/redo. The number doesn't need to be consecutive.
     */
    val version: Int? = null
) : TextDocumentIdentifier
