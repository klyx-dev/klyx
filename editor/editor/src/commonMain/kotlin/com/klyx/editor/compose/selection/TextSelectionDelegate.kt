package com.klyx.editor.compose.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import arrow.core.getOrElse
import com.klyx.editor.compose.CodeEditorState
import com.klyx.editor.compose.renderer.CurrentLineVerticalOffset
import kotlin.math.max

/**
 * This method returns the graphical position where the selection handle should be based on the
 * offset and other information.
 *
 * @param textLayoutResult a result of the text layout.
 * @param offset character offset to be calculated
 * @param isStart true if called for selection start handle
 * @param areHandlesCrossed true if the selection handles are crossed
 * @return the graphical position where the selection handle should be.
 */
internal fun getSelectionHandleCoordinates(
    editorState: CodeEditorState,
    offset: Int,
    isStart: Boolean,
    areHandlesCrossed: Boolean,
): Offset {
    val (line, column) = editorState.cursorAt(offset)

    // This happens if maxLines is set but the offset is on a line >= maxLines.
    if (line >= editorState.lineCount) return Offset.Unspecified

    val textLayoutResult = editorState
        .measureText(editorState.getLine(line))
        .getOrElse { return Offset.Unspecified }

    val x =
        textLayoutResult
            .getHorizontalPosition(column, isStart, areHandlesCrossed)
            .coerceIn(0f, textLayoutResult.size.width.toFloat()) +
                editorState.getContentLeftOffset() + editorState.scrollX

    val y = with(editorState) { (line - 1) * lineHeight + lineHeight + scrollY + CurrentLineVerticalOffset }
    return Offset(x, y)
}

internal fun TextLayoutResult.getHorizontalPosition(
    offset: Int,
    isStart: Boolean,
    areHandlesCrossed: Boolean,
): Float {
    val offsetToCheck =
        if (isStart && !areHandlesCrossed || !isStart && areHandlesCrossed) offset
        else max(offset - 1, 0)
    val bidiRunDirection = getBidiRunDirection(offsetToCheck)
    val paragraphDirection = getParagraphDirection(offset)

    return getHorizontalPosition(
        offset = offset,
        usePrimaryDirection = bidiRunDirection == paragraphDirection,
    )
}
