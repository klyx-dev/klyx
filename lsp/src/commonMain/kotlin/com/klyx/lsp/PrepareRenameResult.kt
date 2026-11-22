package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * One of the result types of the `textDocument/prepareRename` request.
 * Provides the range of the string to rename and a placeholder text of the string content to be renamed.
 *
 * @since 3.12.0
 */
@Serializable
data class PrepareRenameResult(
    /**
     * The range of the string to rename
     */
    val range: Range,

    /*
	 * A placeholder text of the string content to be renamed.
	 */
    val placeholder: String
)


/**
 * One of the result types of the `textDocument/prepareRename` request.
 * Indicates that the client should use its default behavior to compute the rename range.
 *
 * @since 3.16.0
 */
@Serializable
data class PrepareRenameDefaultBehavior(
    /**
     * Indicates that the client should use its default behavior to compute the rename range.
     */
    val defaultBehavior: Boolean
)
