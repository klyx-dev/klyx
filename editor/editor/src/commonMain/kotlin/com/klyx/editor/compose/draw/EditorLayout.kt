package com.klyx.editor.compose.draw

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import com.klyx.editor.compose.CodeEditorState
import com.klyx.editor.compose.input.editorInput
import com.klyx.editor.compose.renderer.OnDraw
import com.klyx.editor.compose.renderer.renderEditor
import com.klyx.editor.compose.scroll.drawHorizontalScrollbar
import com.klyx.editor.compose.scroll.drawVerticalScrollbar
import com.klyx.editor.compose.scroll.editorScroll
import com.klyx.editor.compose.scroll.rememberEditorScrollState
import com.klyx.editor.compose.text.Cursor

@Composable
internal fun EditorLayout(
    modifier: Modifier,
    state: CodeEditorState,
    editable: Boolean,
    showLineNumber: Boolean,
    pinLineNumber: Boolean,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    onDraw: OnDraw = {}
) {
    val hapticFeedback = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }

    Layout(
        modifier = modifier
            .focusRequester(focusRequester)
            .editorInput(state = state, editable = editable)
            .focusable(interactionSource = remember { MutableInteractionSource() })
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f) {
                        state.fontSize *= zoom
                    }
                }
            }
            .pointerInput(state) {
                detectTapGestures(
                    onTap = { offset ->
                        focusRequester.requestFocus()
                        val cursor = state.calculateCursorPositionFromScreenOffset(offset)
                        state.moveCursor(cursor)
                    },
                    onLongPress = { offset ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        val cursor = state.calculateCursorPositionFromScreenOffset(offset)
                        selectText(state, cursor)
                    },
                    onDoubleTap = { offset ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        val cursor = state.calculateCursorPositionFromScreenOffset(offset)
                        selectText(state, cursor)
                    }
                )
            }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawVerticalScrollbar(editorState = state)
            .drawHorizontalScrollbar(editorState = state)
            .renderEditor(
                state = state,
                showLineNumber = showLineNumber,
                pinLineNumber = pinLineNumber,
                fontFamily = fontFamily,
                fontSize = fontSize,
                onDraw = onDraw
            )
            .editorScroll(
                state = rememberEditorScrollState {
                    state.scrollByY(-it)
                    it
                },
                orientation = Orientation.Vertical,
            ).editorScroll(
                state = rememberEditorScrollState {
                    state.scrollByX(-it)
                    it
                },
                orientation = Orientation.Horizontal,
            ),
        measurePolicy = EditorMeasurePolicy
    )
}

private fun selectText(state: CodeEditorState, cursor: Cursor) {
    state.measureText(state.getLine(cursor.line)).onSome { result ->
        val range = result.getWordBoundary(cursor.column)
        val start = state.offsetAt(cursor.line, range.start)
        val end = state.offsetAt(cursor.line, range.end)
        state.moveCursor(state.cursorAt(end))
        state.select(start, end)
    }
}

private object EditorMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        return with(constraints) {
            val width = if (hasFixedWidth) maxWidth else 0
            val height = if (hasFixedHeight) maxHeight else 0
            layout(width, height) {}
        }
    }
}

