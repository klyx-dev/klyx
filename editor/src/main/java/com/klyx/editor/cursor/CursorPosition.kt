package com.klyx.editor.cursor

import android.graphics.Paint
import androidx.annotation.IntRange
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.klyx.editor.CodeEditorState
import kotlin.math.absoluteValue

@Immutable
data class CursorPosition(
    val line: Int,
    val column: Int
) {
    companion object {
        val Zero = CursorPosition(0, 0)
        val Start = CursorPosition(1, 0)
        val Invalid = CursorPosition(-1, -1)
    }

    enum class Direction {
        Left, Right, Up, Down
    }

    val isValid: Boolean
        get() = line >= 0 && column >= 0

    fun move(
        direction: Direction,
        totalLines: Int,
        currentLineLength: Int,
        @IntRange(from = 1)
        length: Int = 1
    ): CursorPosition {
        return when (direction) {
            Direction.Left -> {
                if (column > 0) {
                    copy(column = column - length)
                } else if (line > 1) {
                    CursorPosition(line - length, currentLineLength)
                } else {
                    this
                }
            }

            Direction.Right -> {
                if (column < currentLineLength) {
                    copy(column = column + length)
                } else if (line < totalLines) {
                    CursorPosition(line + length, 0)
                } else {
                    this
                }
            }

            Direction.Up -> copy(line = (line - length).coerceAtLeast(1))
            Direction.Down -> copy(line = (line + length).coerceAtMost(totalLines))
        }
    }
}

/** converts screen coords to [CursorPosition] */
fun Offset.toCursorPosition(
    editorState: CodeEditorState,
    textPaint: Paint
): CursorPosition {
    val x = this.x - editorState.gutterWidth - editorState.gutterPadding - editorState.scrollOffset.x
    val y = this.y - editorState.scrollOffset.y

    val line = ((y / editorState.lineHeightWithSpacing).toInt() + 1).coerceIn(1, editorState.totalLines)
    val lineText = editorState.getLine(line - 1) ?: ""

    var closestColumn = 0
    var minDistance = Float.MAX_VALUE

    for (i in 0 .. lineText.length) {
        val textBeforeCursor = lineText.substring(0, i)
        val charX = textPaint.measureText(textBeforeCursor)
        val distance = (charX - x).absoluteValue

        if (distance < minDistance) {
            minDistance = distance
            closestColumn = i
        }
    }

    return CursorPosition(line, closestColumn)
}
