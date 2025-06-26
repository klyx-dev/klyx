package com.klyx.editor

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.klyx.editor.cursor.CURSOR_BLINK_RATE
import com.klyx.editor.cursor.CursorPosition
import com.klyx.editor.input.codeEditorInput
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
@ExperimentalCodeEditorApi
fun CodeEditor(
    modifier: Modifier = Modifier,
    state: CodeEditorState = rememberCodeEditorState(),
    fontFamily: FontFamily = FontFamily.Monospace,
    fontSize: TextUnit = 18.sp,
    editable: Boolean = true,
    pinLineNumber: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val clipboard = LocalClipboard.current
    val textToolbar = LocalTextToolbar.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        state.clipboard = clipboard
        state.textToolbar = textToolbar
        state.coroutineScope = scope
    }

    val lines = remember(state.text) { state.text.lines() }
    val textMeasurer = rememberTextMeasurer()
    val lineLayoutCache = remember { LineLayoutCache() }

    val style by produceState(TextStyle.Default, fontSize, fontFamily, colorScheme) {
        value = TextStyle(
            fontFamily = fontFamily,
            fontSize = fontSize,
            color = colorScheme.onSurface
        )

        awaitDispose { lineLayoutCache.clear() }
    }

    val transition = rememberInfiniteTransition()
    val cursorAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CURSOR_BLINK_RATE, easing = SineEaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CursorAlpha"
    )

    val focusRequester = remember { FocusRequester() }
    var gutterWidth by remember { mutableFloatStateOf(0f) }

    val lineHeight = remember(style, textMeasurer) {
        textMeasurer.measure(
            text = AnnotatedString("Ag"), // characters with ascenders and descenders
            style = style
        ).multiParagraph.getLineHeight(0)
    }

    val visibleRange by remember {
        derivedStateOf {
            if (state.canvasSize.height <= 0f) return@derivedStateOf 0 .. 0

            val raw = -state.scrollY / lineHeight
            println("raw: $raw")

            val firstVisibleLine = maxOf(0, floor(-state.scrollY / lineHeight).toInt())
            val visibleLineCount = (ceil(state.canvasSize.height / lineHeight).toInt() + 2).coerceAtMost(state.lineCount)
            val lastVisibleLine = minOf(lines.size - 1, firstVisibleLine + visibleLineCount)
            println(visibleLineCount)

            (firstVisibleLine .. lastVisibleLine).also(::println)
        }
    }

    val fullTextLayoutResult = remember(state.text, style, textMeasurer) {
        textMeasurer.measure(
            text = state.text,
            style = style,
            softWrap = false
        )
    }

    Row(modifier = modifier) {
        CodeEditorCanvas(
            modifier = Modifier
                .width(with(density) { gutterWidth.toDp() })
                .fillMaxHeight()
                .zIndex(100f)
        ) {
            gutterWidth = 40.dp.toPx()

            onDrawBehind {
                translate(
                    left = if (pinLineNumber) 0f else state.scrollX
                ) {
                    clipRect {
                        drawRect(
                            color = colorScheme.surfaceContainerHigh,
                            size = size
                        )

                        drawLineNumber(
                            visibleRange = visibleRange,
                            lineHeight = lineHeight,
                            state = state,
                            lineLayoutCache = lineLayoutCache,
                            textMeasurer = textMeasurer,
                            fontFamily = fontFamily,
                            fontSize = fontSize,
                            color = colorScheme.onSurface.copy(alpha = 0.7f),
                            gutterWidth = gutterWidth
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(2.dp))

        CodeEditorCanvas(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .codeEditorInput(state, editable, keyboardController)
                .focusable(interactionSource = remember { MutableInteractionSource() })
                .pointerInput(state) { handleTouchInput(focusRequester, state, haptics, editable) }
                .codeEditorScroll(state)
        ) {
            state.canvasSize = size
            state.textLayoutResult = fullTextLayoutResult

            onDrawBehind {
                clipRect(
                    left = if (pinLineNumber) 0f else state.scrollX
                ) {
                    drawRect(colorScheme.background)

                    for (lineIndex in visibleRange) {
                        val line = lines[lineIndex]
                        val yPosition = lineIndex * state.getLineHeight(lineIndex) + state.scrollY

                        // skip empty lines or lines outside visible area
                        if (line.isEmpty() || yPosition + lineHeight < 0 || yPosition > size.height) continue

                        val lineLayout = with(lineLayoutCache) {
                            textMeasurer.getOrMeasure(
                                line = line,
                                style = style
                            )
                        }

                        drawText(
                            textLayoutResult = lineLayout,
                            topLeft = Offset(state.scrollX, yPosition)
                        )
                    }

                    if (editable) {
                        drawCursor(state, colorScheme.primary, cursorAlpha)
                    }

                    if (state.isTextSelected()) {
                        drawSelection(state, colorScheme.primaryContainer)
                    }
                }
            }
        }
    }
}

@ExperimentalCodeEditorApi
private suspend fun PointerInputScope.handleTouchInput(
    focusRequester: FocusRequester,
    state: CodeEditorState,
    haptics: HapticFeedback,
    editable: Boolean
) {
    detectTapGestures(
        onTap = { position ->
            focusRequester.requestFocus()
            if (state.isTextSelected()) state.clearSelection()
            state.hideTextToolbarIfShown()

            state.cursorPosition = CursorPosition(
                offset = state.getOffsetForPosition(position - state.scrollOffset)
            )
        },
        onLongPress = { position ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)

            val offset = state.getOffsetForPosition(position - state.scrollOffset)
            val wordBoundary = state.getWordBoundary(offset)
            state.select(wordBoundary)
            state.cursorPosition = CursorPosition(wordBoundary.end)

            state.showTextToolbar(editable)
        }
    )
}

@ExperimentalCodeEditorApi
private fun DrawScope.drawLineNumber(
    visibleRange: IntRange,
    lineHeight: Float,
    state: CodeEditorState,
    lineLayoutCache: LineLayoutCache,
    textMeasurer: TextMeasurer,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    color: Color,
    gutterWidth: Float
) {
    for (line in visibleRange) {
        val lineNumber = line + 1
        val yPosition = line * lineHeight + state.scrollY

        // skip if line is not visible
        if (yPosition + lineHeight < 0 || yPosition > size.height) continue

        val lineNumberLayout = with(lineLayoutCache) {
            textMeasurer.getOrMeasure(
                line = lineNumber.toString(),
                style = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = fontSize,
                    color = color,
                    textAlign = TextAlign.End
                ),
                constraints = Constraints(maxWidth = (gutterWidth - 4.dp.toPx()).roundToInt())
            )
        }

        drawText(
            textLayoutResult = lineNumberLayout,
            topLeft = Offset(2.dp.toPx(), yPosition)
        )
    }
}

@ExperimentalCodeEditorApi
private fun DrawScope.drawSelection(
    state: CodeEditorState,
    color: Color
) {
    state.getPathForSelectionRange().let { selectionPath ->
        if (!selectionPath.isEmpty) {
            drawPath(
                path = selectionPath.apply { translate(state.scrollOffset) },
                color = color,
                alpha = 0.5f
            )
        }
    }
}

@ExperimentalCodeEditorApi
private fun DrawScope.drawCursor(
    state: CodeEditorState,
    color: Color,
    cursorAlpha: Float
) {
    val cursorRect = state.getCursorRect()
    drawLine(
        color = color,
        alpha = cursorAlpha,
        start = cursorRect.topCenter + state.scrollOffset,
        end = cursorRect.bottomCenter + state.scrollOffset,
        strokeWidth = 2f,
        cap = StrokeCap.Round,
    )
}

@Composable
private fun CodeEditorCanvas(
    modifier: Modifier = Modifier,
    onDraw: CacheDrawScope.() -> DrawResult
) {
    Layout(
        measurePolicy = CodeEditorMeasurePolicy,
        modifier = modifier.drawWithCache(onDraw)
    )
}

private object CodeEditorMeasurePolicy : MeasurePolicy {
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

private val SineEaseInOut: Easing = Easing { fraction ->
    ((1 - cos(fraction * PI)) / 2).toFloat()
}
