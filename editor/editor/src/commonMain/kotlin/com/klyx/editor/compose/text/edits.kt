package com.klyx.editor.compose.text

import kotlinx.serialization.Serializable

/**
 * An identifier for a single edit operation.
 */
@Serializable
data class Identifier(
    /** Identifier major */
    val major: Int = 0,
    /** Identifier minor */
    val minor: Int = 0
)

@Serializable
internal data class ValidatedEditOperation(
    val sortIndex: Int = 0,
    val identifier: Identifier? = null,
    /** The range to replace. This can be empty to emulate a simple insert/delete. */
    val range: Range,
    /** The offset of the range that got replaced. */
    val rangeOffset: Int = 0,
    /** The length of the range that got replaced. */
    val rangeLength: Int = 0,
    /** The text to replace with. This can be null to emulate a simple insert/delete. */
    val text: String? = null,
    /** The total number of lines of text, This can be 0 to emulate a simple insert/delete */
    val eolCount: Int = 0,
    /** The length of the first line of text */
    val firstLineLength: Int = 0,
    /** The length of the last line of text */
    val lastLineLength: Int = 0,
    /**
     * This indicates that this operation has "insert" semantics. i.e. forceMoveMarkers = true => if
     * `range` is collapsed, all markers at the position will be moved.
     */
    val forceMoveMarkers: Boolean = false,
    /**
     * This indicates that this operation is inserting automatic whitespace that can be removed on
     * next model edit operation if `config.trimAutoWhitespace` is true.
     */
    val isAutoWhitespaceEdit: Boolean = false
)

@Serializable
internal data class SingleEditOperation(
    val range: Range,
    val text: String?,
    val identifier: Identifier? = null,
    val forceMoveMarkers: Boolean = false,
    val isAutoWhitespaceEdit: Boolean = false
)

@Serializable
internal data class ReverseEditOperation(
    val sortIndex: Int,
    val identifier: Identifier?,
    val range: Range,
    val text: String?,
    val textChange: TextChange
)

@Serializable
internal data class ContentChange(
    /** The range to replace. This can be empty to emulate a simple insert/delete. */
    val range: Range,
    /** The offset of the range that got replaced. */
    val rangeOffset: Int,
    /** The length of the range that got replaced. */
    val rangeLength: Int,
    /** The text to replace with. This can be null to emulate a simple insert/delete. */
    val text: String?,
    /**
     * This indicates that this operation has "insert" semantics. i.e. forceMoveMarkers = true => if
     * `range` is collapsed, all markers at the position will be moved.
     */
    val forceMoveMarkers: Boolean
)

@Serializable
internal data class ApplyEditsResult(
    val changes: List<ContentChange>,
    val reverseEdits: List<ReverseEditOperation>?,
    val trimAutoWhitespaceLineNumbers: List<Int>?
)
