package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Defines how the host (editor) should sync document changes to the language
 * server.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#textDocumentSyncKind)
 */
@Serializable
@JvmInline
value class TextDocumentSyncKind private constructor(private val value: Int) {
    companion object {
        /**
         * Documents should not be synced at all.
         */
        val None = TextDocumentSyncKind(0)

        /**
         * Documents are synced by always sending the full content
         * of the document.
         */
        val Full = TextDocumentSyncKind(1)

        /**
         * Documents are synced by sending the full content on open.
         * After that only incremental updates to the document are
         * sent.
         */
        val Incremental = TextDocumentSyncKind(2)
    }
}
