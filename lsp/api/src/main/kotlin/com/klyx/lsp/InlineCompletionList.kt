package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Represents a collection of [inline completion items][InlineCompletionItem]
 * to be presented in the editor.
 *
 * @since 3.18.0
 */
@Serializable
data class InlineCompletionList(
    /**
     * The inline completion items.
     */
    val items: List<InlineCompletionItem>
)
