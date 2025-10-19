package com.klyx.editor.compose.selection

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow

internal open class StaticTextSelectionParams(
    val layoutCoordinates: LayoutCoordinates?,
    val textLayoutResult: TextLayoutResult?,
) {
    companion object {
        val Empty = StaticTextSelectionParams(null, null)
    }

    open fun getPathForRange(start: Int, end: Int): Path? {
        return textLayoutResult?.getPathForRange(start, end)
    }

    open val shouldClip: Boolean
        get() =
            textLayoutResult?.let {
                it.layoutInput.overflow != TextOverflow.Visible && it.hasVisualOverflow
            } ?: false

    // if this copy shows up in traces, this class may become mutable
    fun copy(
        layoutCoordinates: LayoutCoordinates? = this.layoutCoordinates,
        textLayoutResult: TextLayoutResult? = this.textLayoutResult,
    ): StaticTextSelectionParams {
        return StaticTextSelectionParams(layoutCoordinates, textLayoutResult)
    }
}
