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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.klyx.editor.compose.draw.buildPath
import com.klyx.editor.compose.renderer.CurrentLineVerticalOffset
import com.klyx.editor.compose.renderer.TextLineCache
import kotlin.math.floor
import kotlin.math.round

@Stable
object EditorDefaults {
    val CursorThickness = 2.dp

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
        if (!state.selection.collapsed) return noOp
        val cursor = state.cursor
        if (cursor.line !in 1..state.lineCount) return noOp

        val line = state.getLine(cursor.line)
        val result = TextLineCache.getOrPut(line) {
            state.measureText(line).getOrNull() ?: return noOp
        }

        val cursorRect = result
            .getCursorRect(cursor.column.coerceAtMost(line.length)).also {
                state.cursorRect = calculateCursorRect(it, result)
            }

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
                    size = Size(CursorThickness.toPx(), cursorRect.height),
                    cornerRadius = CornerRadius(4f)
                )
            }
        }
    }

    context(density: Density)
    private fun calculateCursorRect(cursorRect: Rect, layoutResult: TextLayoutResult) = with(density) {
        val cursorWidth = floor(CursorThickness.toPx()).coerceAtLeast(1f)
        // left and right values in cursorRect should be the same but in any case use the
        // logically correct anchor.
        val cursorCenterX = if (layoutResult.layoutInput.layoutDirection == LayoutDirection.Ltr) {
            (cursorRect.left + cursorWidth / 2)
        } else {
            (cursorRect.right - cursorWidth / 2)
        }

        // don't let cursor go beyond the bounds of text layout node or cursor will be clipped.
        // but also make sure that empty Text Layout still draws a cursor.
        val coercedCursorCenterX =
            cursorCenterX
                // do not use coerceIn because it is not guaranteed that minimum value is smaller
                // than the maximum value.
                .coerceAtMost(layoutResult.size.width - cursorWidth / 2)
                .coerceAtLeast(cursorWidth / 2)
                .let {
                    // When cursor width is odd, draw it in the middle of a pixel,
                    // to avoid blurring due to antialiasing.
                    if (cursorWidth.toInt() % 2 == 1) {
                        floor(it) + 0.5f // round to nearest n+0.5
                    } else round(it)
                }

        Rect(
            left = coercedCursorCenterX - cursorWidth / 2,
            right = coercedCursorCenterX + cursorWidth / 2,
            top = cursorRect.top,
            bottom = cursorRect.bottom,
        )
    }
}
