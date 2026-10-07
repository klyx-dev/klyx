package com.klyx.lsp

import com.klyx.lsp.types.OneOf
import kotlinx.serialization.Serializable

/**
 * The notebooks to be synced
 *
 * At least one of either [notebook] or [cells] is required.
 */
@Serializable
data class NotebookSelector(
    /**
     * The notebook to be synced. If a string
     * value is provided, it matches against the
     * notebook type. '*' matches every notebook.
     */
    val notebook: OneOf<String, NotebookDocumentFilter>? = null,

    /**
     * The cells of the matching notebook to be synced.
     */
    val cells: List<NotebookSelectorCell>? = null
)

/**
 * The cells of the matching notebook to be synced.
 */
@Serializable
data class NotebookSelectorCell(val language: String)
