package com.klyx.editor.language

import kotlinx.serialization.Serializable

/**
 * The configuration for block comments for this language.
 *
 * @property start A start tag of block comment.
 * @property end A end tag of block comment.
 * @property prefix A character to add as a prefix when a new line is added to a block comment.
 * @property tabSize A indent to add for prefix and end line upon new line.
 */
@Serializable
data class BlockCommentConfig(
    val start: String,
    val end: String,
    val prefix: String,
    val tabSize: UInt
)
