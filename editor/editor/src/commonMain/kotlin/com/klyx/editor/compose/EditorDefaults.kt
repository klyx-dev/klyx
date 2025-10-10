package com.klyx.editor.compose

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.klyx.editor.compose.draw.buildPath
import com.klyx.editor.compose.renderer.CurrentLineVerticalOffset

@Stable
object EditorDefaults {
    private val defaultEditorColors: EditorColorScheme
        @Composable
        @ReadOnlyComposable
        get() {
            val c = MaterialTheme.colorScheme
            val sc = LocalTextSelectionColors.current

            return EditorColorScheme(
                background = c.background,
                foreground = c.onBackground,
                lineNumberBackground = c.surfaceColorAtElevation(1.dp),
                lineNumber = c.onSurfaceVariant,
                lineDivider = c.outline,
                currentLineBackground = c.surfaceColorAtElevation(2.dp),
                cursor = sc.handleColor,
                selectionBackground = sc.backgroundColor,
                selectionForeground = sc.handleColor,
                scrollbar = c.onSurfaceVariant.copy(alpha = 0.4f),
                scrollbarThumb = c.onSurfaceVariant
            )
        }

    @Composable
    fun colorScheme() = defaultEditorColors

    @Composable
    fun colorScheme(
        background: Color = Color.Unspecified,
        foreground: Color = Color.Unspecified
    ) = defaultEditorColors.copy(
        background = background,
        foreground = foreground
    )

    internal fun DrawScope.drawTextSelectLeftHandle(caret: Offset, color: Color) {
        translate(caret.x, caret.y) {
            drawPath(
                path = buildPath {
                    moveToRelative(18f, 0f)
                    lineToRelative(0f, 9f)
                    arcToRelative(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -9f, dy1 = 9f)
                    arcToRelative(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -9f, dy1 = -9f)
                    arcToRelative(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 9f, dy1 = -9f)
                    lineToRelative(9f, 0f)
                    close()
                },
                color = color
            )
        }
    }

    internal fun DrawScope.drawTextSelectRightHandle(caret: Offset, color: Color) {
        translate(caret.x, caret.y) {
            drawPath(
                path = buildPath {
                    moveToRelative(0f, 0f)
                    lineToRelative(0f, 9f)
                    arcToRelative(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 9f, dy1 = 9f)
                    arcToRelative(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 9f, dy1 = -9f)
                    arcToRelative(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -9f, dy1 = -9f)
                    lineToRelative(-9f, 0f)
                    close()
                },
                color = color
            )
        }
    }

    internal fun DrawScope.drawTextSelectMiddleHandle(caret: Offset, color: Color) {
        translate(caret.x, caret.y) {
            drawPath(
                path = buildPath {
                    moveToRelative(9f, 0f)
                    lineToRelative(6.364f, 6.364f)
                    arcToRelative(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 0f, dy1 = 12.728f)
                    arcToRelative(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -12.728f, dy1 = 0f)
                    arcToRelative(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 0f, dy1 = -12.728f)
                    lineToRelative(6.364f, -6.364f)
                    close()
                },
                color = color
            )
        }
    }

    internal fun CacheDrawScope.drawCursor(state: CodeEditorState, pinLineNumber: Boolean): DrawResult {
        val noOp = onDrawBehind { }
        val cursor = state.cursor
        if (cursor.line !in 1..state.lineCount) return noOp

        val line = state.getLine(cursor.line)
        val result = TextLineCache.getOrPut(line) {
            state.measureText(line).getOrNull() ?: return noOp
        }
        val cursorRect = result.getCursorRect(cursor.column.coerceAtMost(line.length))

        val leftOffset = state.getContentLeftOffset()
        val y = (cursor.line - 1) * state.lineHeight + state.scrollY + CurrentLineVerticalOffset

        return onDrawWithContent {
            drawContent()

            withTransform({
                if (pinLineNumber) clipRect(left = leftOffset)
                translate(left = leftOffset + state.scrollX, top = y)
            }) {
                drawRoundRect(
                    color = state.colorScheme.cursor.copy(alpha = state.cursorAlpha),
                    topLeft = cursorRect.topLeft,
                    size = Size(2f, cursorRect.height),
                    cornerRadius = CornerRadius(4f)
                )
            }
        }
    }
}
