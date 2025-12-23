package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * A notebook document filter denotes a notebook document by
 * different properties.
 *
 * At least one of either [notebookType], [scheme], or [pattern] is required.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#notebookDocumentFilter)
 *
 * @since 3.17.0
 */
@Serializable
data class NotebookDocumentFilter(
    /**
     * The type of the enclosing notebook.
     */
    val notebookType: String? = null,

    /**
     * A Uri scheme, like `file` or `untitled`.
     */
    val scheme: String? = null,

    /**
     * A glob pattern.
     */
    val pattern: GlobPattern? = null
)
