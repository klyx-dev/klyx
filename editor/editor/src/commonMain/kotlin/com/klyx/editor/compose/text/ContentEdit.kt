package com.klyx.editor.compose.text

import androidx.compose.ui.text.TextRange

internal fun buildSingleEditOperation(
    text: String?,
    range: Range
) = listOf(SingleEditOperation(range, text))

internal sealed interface ContentEditAction {
    data object Insert : ContentEditAction
    data class Delete(val count: Int, val isForward: Boolean = false) : ContentEditAction
}

data class ContentEditOperation(
    val range: TextRange,
    val text: Text?
)

interface ContentChangeList {
    /** The number of changes that have been performed. */
    val changeCount: Int

    /**
     * Returns the range in the [Content] that was changed.
     *
     * @throws IndexOutOfBoundsException If [changeIndex] is not in [0, [changeCount]).
     */
    fun getRange(changeIndex: Int): TextRange
}
