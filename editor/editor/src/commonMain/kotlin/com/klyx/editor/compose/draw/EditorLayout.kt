package com.klyx.editor.compose.draw

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberScrollable2DState
import androidx.compose.foundation.gestures.scrollable2D
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
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
import com.klyx.editor.compose.EditorDefaults.drawCursor
import com.klyx.editor.compose.input.editorInput
import com.klyx.editor.compose.renderer.OnDraw
import com.klyx.editor.compose.renderer.renderEditor
import com.klyx.editor.compose.scroll.drawHorizontalScrollbar
import com.klyx.editor.compose.scroll.drawVerticalScrollbar
import com.klyx.editor.compose.text.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    val overscrollEffect = rememberOverscrollEffect()
    val scrollModifier = Modifier.scrollable2D(
        state = rememberScrollable2DState { delta ->
            val oldValue = state.scrollState.offset
            state.scrollBy(delta)

            with(state.scrollState.offset - oldValue) {
                if (getDistanceSquared() == 0f) this else delta
            }
        },
        overscrollEffect = overscrollEffect
    )

    val cursorAlpha = remember { Animatable(1f) }
    var cursorJob: Job? = null

    var isTyping by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { state.cursor }.collect {
            isTyping = true
            delay(400)
            isTyping = false
        }
    }

    LaunchedEffect(state.editable, state.cursor) {
        cursorJob?.cancel()

        if (state.editable) {
            cursorJob = launch(Dispatchers.Default) {
                while (true) {
                    if (!isTyping) {
                        cursorAlpha.animateTo(0f, tween(500)) { state.cursorAlpha = value }
                        cursorAlpha.animateTo(1f, tween(500)) { state.cursorAlpha = value }
                    } else {
                        state.cursorAlpha = 1f
                        delay(500)
                    }
                }
            }
        }
    }

    Box(modifier = modifier.overscroll(overscrollEffect)) {
        Layout(
            modifier = Modifier
                .matchParentSize()
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
                .drawWithCache { drawCursor(state, pinLineNumber) }
                .renderEditor(
                    state = state,
                    showLineNumber = showLineNumber,
                    pinLineNumber = pinLineNumber,
                    fontFamily = fontFamily,
                    fontSize = fontSize,
                    onDraw = onDraw
                )
                .then(scrollModifier),
            measurePolicy = EditorMeasurePolicy
        )
    }
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

