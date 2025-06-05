package com.klyx.editor.compose

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klyx.core.event.EventBus
import com.klyx.editor.compose.input.textInput

@Composable
fun KlyxCodeEditor(
    text: String,
    modifier: Modifier = Modifier,
    wrapText: Boolean = false,
    scrollbarThickness: Dp = 10.dp,
    lineHeight: TextUnit = 18.sp,
    horizontalPadding: Dp = 10.dp,
    bottomPaddingLines: Int = 5,
    scrollbarColor: Color = Color(0x88FFFFFF),
    typeface: Typeface? = null,
    gutterWidth: Dp = 48.dp,
    showGutter: Boolean = true,
    pinnedLineNumbers: Boolean = false,
    gutterBackgroundColor: Color = Color(0xFF2B2B2B),
    gutterTextColor: Color = Color(0xFF888888),
    gutterDividerColor: Color = Color(0xFF444444)
) {
    val scrollY = remember { mutableFloatStateOf(0f) }
    val scrollX = remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val lineHeightPx = with(density) { lineHeight.toPx() }
    val lineSpacingPx = with(density) { 4.dp.toPx() }
    val fullLineHeightPx = lineHeightPx + lineSpacingPx
    val horizontalPaddingPx = with(density) { horizontalPadding.toPx() }
    val gutterWidthPx = if (showGutter) with(density) { gutterWidth.toPx() } else 0f
    val scrollbarThicknessPx = with(density) { scrollbarThickness.toPx() }
    val endHorizontalPaddingPx = if (wrapText) 0f else with(density) { 50.dp.toPx() }

    var draggingVerticalScrollbar by remember { mutableStateOf(false) }
    var draggingHorizontalScrollbar by remember { mutableStateOf(false) }
    var verticalDragOffset by remember { mutableFloatStateOf(0f) }
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .textInput(keyboardController) { event ->
                EventBus.getInstance().postSync(event)
                true
            }
            .focusable(interactionSource = remember { MutableInteractionSource() })
            .pointerInput(Unit) {
                detectTapGestures {
                    focusRequester.requestFocus()
                }
            }
            .pointerInput(text, wrapText, showGutter, pinnedLineNumbers) { // Consider unifying allLines calculation
                detectDragGestures(
                    onDragStart = { offset ->
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        val paint = Paint().apply {
                            textSize = lineHeightPx
                            isAntiAlias = true
                            this.typeface = typeface
                        }

                        // this allLines calculation is specific to drag gestures.
                        // for perfect consistency, it should use the same logic/source as the drawing part.
                        val allLines = buildList {
                            text.lines().forEachIndexed { originalLineIdx, originalLine ->
                                if (!wrapText) {
                                    add(originalLine)
                                } else {
                                    if (originalLine.isEmpty()) {
                                        add("")
                                    } else {
                                        var remaining = originalLine
                                        val wrapWidth = canvasWidth - horizontalPaddingPx * 2 - gutterWidthPx
                                        while (remaining.isNotEmpty()) {
                                            val count = paint.breakText(remaining, true, wrapWidth, null)
                                            if (count == 0) {
                                                add(remaining)
                                                break
                                            }
                                            add(remaining.substring(0, count))
                                            remaining = remaining.substring(count)
                                        }
                                    }
                                }
                            }
                        }

                        val totalVisualLines = allLines.size
                        val contentHeight = totalVisualLines * fullLineHeightPx + bottomPaddingLines * fullLineHeightPx
                        val verticalLimit = (contentHeight - canvasHeight).coerceAtLeast(0f)
                        val maxLineWidth = allLines.maxOfOrNull { paint.measureText(it) } ?: 0f
                        val horizontalLimit = if (!wrapText) {
                            (maxLineWidth - canvasWidth + horizontalPaddingPx + endHorizontalPaddingPx + gutterWidthPx).coerceAtLeast(0f)
                        } else 0f

                        val verticalThumbHeight = if (verticalLimit > 0f) {
                            (canvasHeight / contentHeight * canvasHeight).coerceAtLeast(20f)
                        } else canvasHeight.toFloat()

                        val verticalThumbTop = if (verticalLimit > 0f) {
                            (scrollY.floatValue / verticalLimit * (canvasHeight - verticalThumbHeight))
                                .coerceIn(0f, canvasHeight - verticalThumbHeight)
                        } else 0f

                        val horizontalThumbWidth = if (!wrapText && horizontalLimit > 0f) {
                            (canvasWidth / (maxLineWidth + horizontalPaddingPx + endHorizontalPaddingPx + gutterWidthPx) * canvasWidth).coerceAtLeast(20f)
                        } else 0f

                        val horizontalThumbLeft = if (!wrapText && horizontalLimit > 0f) {
                            (scrollX.floatValue / horizontalLimit * (canvasHeight - horizontalThumbWidth))
                                .coerceIn(0f, canvasWidth - horizontalThumbWidth)
                        } else 0f

                        val inVerticalScrollbarX = offset.x >= canvasWidth - scrollbarThicknessPx && offset.x <= canvasWidth
                        val inVerticalScrollbarY = offset.y >= verticalThumbTop && offset.y <= verticalThumbTop + verticalThumbHeight

                        val inHorizontalScrollbarY = offset.y >= canvasHeight - scrollbarThicknessPx && offset.y <= canvasHeight
                        val inHorizontalScrollbarX = offset.x >= horizontalThumbLeft && offset.x <= horizontalThumbLeft + horizontalThumbWidth

                        val inGutterArea = showGutter && offset.x <= gutterWidthPx

                        draggingVerticalScrollbar = !inGutterArea && verticalLimit > 0f && inVerticalScrollbarX && inVerticalScrollbarY
                        draggingHorizontalScrollbar = !inGutterArea && !wrapText && horizontalLimit > 0f && inHorizontalScrollbarY && inHorizontalScrollbarX

                        if (draggingVerticalScrollbar) {
                            verticalDragOffset = offset.y - verticalThumbTop
                        }
                        if (draggingHorizontalScrollbar) {
                            horizontalDragOffset = offset.x - horizontalThumbLeft
                        }
                    },
                    onDragEnd = {
                        draggingVerticalScrollbar = false
                        draggingHorizontalScrollbar = false
                    },
                    onDragCancel = {
                        draggingVerticalScrollbar = false
                        draggingHorizontalScrollbar = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        val paint = Paint().apply { // Duplicated paint, consider hoisting
                            textSize = lineHeightPx
                            isAntiAlias = true
                            this.typeface = typeface
                        }
                         // this allLines calculation is specific to drag gestures.
                        val allLines = buildList {
                            text.lines().forEachIndexed { originalLineIdx, originalLine ->
                                if (!wrapText) {
                                    add(originalLine)
                                } else {
                                    if (originalLine.isEmpty()) {
                                        add("")
                                    } else {
                                        var remaining = originalLine
                                        val wrapWidth = canvasWidth - horizontalPaddingPx * 2 - gutterWidthPx
                                        while (remaining.isNotEmpty()) {
                                            val count = paint.breakText(remaining, true, wrapWidth, null)
                                            if (count == 0) {
                                                add(remaining)
                                                break
                                            }
                                            add(remaining.substring(0, count))
                                            remaining = remaining.substring(count)
                                        }
                                    }
                                }
                            }
                        }

                        val totalVisualLines = allLines.size
                        val contentHeight = totalVisualLines * fullLineHeightPx + bottomPaddingLines * fullLineHeightPx
                        val verticalLimit = (contentHeight - canvasHeight).coerceAtLeast(0f)
                        val maxLineWidth = allLines.maxOfOrNull { paint.measureText(it) } ?: 0f
                        val horizontalLimit = if (!wrapText) {
                            (maxLineWidth - canvasWidth + horizontalPaddingPx + endHorizontalPaddingPx + gutterWidthPx).coerceAtLeast(0f)
                        } else 0f

                        if (draggingVerticalScrollbar && verticalLimit > 0f) {
                            val verticalThumbHeight = (canvasHeight / contentHeight * canvasHeight).coerceAtLeast(20f)
                            val newThumbTop = (change.position.y - verticalDragOffset).coerceIn(0f, canvasHeight - verticalThumbHeight)
                            scrollY.floatValue = (newThumbTop / (canvasHeight - verticalThumbHeight)) * verticalLimit
                        } else if (draggingHorizontalScrollbar && !wrapText && horizontalLimit > 0f) {
                            val horizontalThumbWidth = (canvasWidth / (maxLineWidth + horizontalPaddingPx + endHorizontalPaddingPx + gutterWidthPx) * canvasWidth)
                                .coerceAtLeast(20f)
                            val newThumbLeft = (change.position.x - horizontalDragOffset).coerceIn(0f, canvasWidth - horizontalThumbWidth)
                            scrollX.floatValue = (newThumbLeft / (canvasWidth - horizontalThumbWidth)) * horizontalLimit
                        } else {
                            if (!draggingHorizontalScrollbar && !draggingVerticalScrollbar) { // only scroll with finger if not dragging scrollbar
                                scrollY.floatValue = (scrollY.floatValue - dragAmount.y).coerceIn(0f, verticalLimit)
                                if (!wrapText) {
                                    scrollX.floatValue = (scrollX.floatValue - dragAmount.x).coerceIn(0f, horizontalLimit)
                                }
                            }
                        }
                    }
                )
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val textPaint = Paint().apply {
            textSize = lineHeightPx
            color = Color.White.toArgb()
            isAntiAlias = true
            this.typeface = typeface
        }

        val gutterPaint = Paint().apply {
            textSize = lineHeightPx * 0.85f
            color = gutterTextColor.toArgb()
            isAntiAlias = true
            this.typeface = typeface
            textAlign = Paint.Align.RIGHT
        }

        val allVisualLines = mutableListOf<String>()
        val visualToOriginalLineIndexMap = mutableListOf<Int>()

        text.lines().forEachIndexed { originalLineIdx, originalLine ->
            if (!wrapText) {
                allVisualLines.add(originalLine)
                visualToOriginalLineIndexMap.add(originalLineIdx)
            } else {
                if (originalLine.isEmpty()) {
                    allVisualLines.add("")
                    visualToOriginalLineIndexMap.add(originalLineIdx)
                } else {
                    var remaining = originalLine
                    // ensure wrapWidth is positive, otherwise breakText might behave unexpectedly or loop.
                    val wrapWidth = (canvasWidth - horizontalPaddingPx * 2 - gutterWidthPx).coerceAtLeast(1f)
                    while (remaining.isNotEmpty()) {
                        val count = textPaint.breakText(remaining, true, wrapWidth, null)
                        if (count == 0) { // safety break for very narrow wrapWidth or unusual characters
                            allVisualLines.add(remaining) // add the rest of the line to avoid losing it
                            visualToOriginalLineIndexMap.add(originalLineIdx)
                            break
                        }
                        allVisualLines.add(remaining.substring(0, count))
                        visualToOriginalLineIndexMap.add(originalLineIdx)
                        remaining = remaining.substring(count)
                    }
                }
            }
        }

        val totalVisualLines = allVisualLines.size
        val contentHeight = totalVisualLines * fullLineHeightPx + bottomPaddingLines * fullLineHeightPx
        val verticalLimit = (contentHeight - canvasHeight).coerceAtLeast(0f)
        scrollY.floatValue = scrollY.floatValue.coerceIn(0f, verticalLimit)

        val maxLineWidth = allVisualLines.maxOfOrNull { textPaint.measureText(it) } ?: 0f
        val horizontalLimit = if (!wrapText) {
            (maxLineWidth - canvasWidth + horizontalPaddingPx + endHorizontalPaddingPx + gutterWidthPx).coerceAtLeast(0f)
        } else 0f

        if (!wrapText) {
            scrollX.floatValue = scrollX.floatValue.coerceIn(0f, horizontalLimit)
        } else {
            scrollX.floatValue = 0f
        }

        val firstVisibleIndex = (scrollY.floatValue / fullLineHeightPx).toInt().coerceAtLeast(0)
        val visibleCount = (canvasHeight / fullLineHeightPx).toInt() + 2
        val lastVisibleIndex = (firstVisibleIndex + visibleCount).coerceAtMost(allVisualLines.size)

        // Draw code text first
        for (i in firstVisibleIndex until lastVisibleIndex) {
            val line = allVisualLines[i]
            val y = i * fullLineHeightPx - scrollY.floatValue + lineHeightPx

            val codeStartX = gutterWidthPx + horizontalPaddingPx - scrollX.floatValue
            drawContext.canvas.nativeCanvas.drawText(
                line,
                codeStartX,
                y,
                textPaint
            )
        }

        if (showGutter) {
            val gutterScrollX = if (pinnedLineNumbers) 0f else -scrollX.floatValue

            drawRect(
                color = gutterBackgroundColor,
                topLeft = Offset(gutterScrollX, 0f),
                size = Size(gutterWidthPx, canvasHeight)
            )

            drawRect(
                color = gutterDividerColor,
                topLeft = Offset(gutterWidthPx - 1f + gutterScrollX, 0f),
                size = Size(1f, canvasHeight)
            )

            for (i in firstVisibleIndex until lastVisibleIndex) {
                if (i >= visualToOriginalLineIndexMap.size) continue // Safety check

                val y = i * fullLineHeightPx - scrollY.floatValue + lineHeightPx
                val currentOriginalLineIndex = visualToOriginalLineIndexMap[i]
                
                val shouldShowLineNumber = if (!wrapText) {
                    true
                } else {
                    if (i == 0) {
                        true
                    } else {
                        // show number if the original line index changed from the previous visual line
                        // and ensure previous index is valid
                        if (i -1 < visualToOriginalLineIndexMap.size) {
                           visualToOriginalLineIndexMap[i] != visualToOriginalLineIndexMap[i - 1]
                        } else {
                           true // should not happen if i > 0
                        }
                    }
                }

                if (shouldShowLineNumber) {
                    val lineNumber = currentOriginalLineIndex + 1
                    drawContext.canvas.nativeCanvas.drawText(
                        lineNumber.toString(),
                        gutterWidthPx - horizontalPaddingPx / 2 + gutterScrollX, // Adjust for gutter padding
                        y,
                        gutterPaint
                    )
                }
            }
        }

        if (verticalLimit > 0f) {
            val verticalThumbHeight = (canvasHeight / contentHeight * canvasHeight).coerceAtLeast(20f)
            val verticalThumbTop = (scrollY.floatValue / verticalLimit * (canvasHeight - verticalThumbHeight))
                .coerceIn(0f, canvasHeight - verticalThumbHeight)

            drawRect(
                color = scrollbarColor.copy(alpha = 0.3f),
                topLeft = Offset(canvasWidth - scrollbarThicknessPx, 0f),
                size = Size(scrollbarThicknessPx, canvasHeight)
            )
            drawRect(
                color = scrollbarColor,
                topLeft = Offset(canvasWidth - scrollbarThicknessPx, verticalThumbTop),
                size = Size(scrollbarThicknessPx, verticalThumbHeight)
            )
        }

        if (!wrapText && horizontalLimit > 0f) {
            val horizontalThumbWidth = (canvasWidth / (maxLineWidth + horizontalPaddingPx + endHorizontalPaddingPx + gutterWidthPx) * canvasWidth)
                .coerceAtLeast(20f)
            val horizontalThumbLeft = (scrollX.floatValue / horizontalLimit * (canvasWidth - horizontalThumbWidth))
                .coerceIn(0f, canvasWidth - horizontalThumbWidth)

            drawRect(
                color = scrollbarColor.copy(alpha = 0.3f),
                topLeft = Offset(0f, canvasHeight - scrollbarThicknessPx),
                size = Size(canvasWidth, scrollbarThicknessPx)
            )
            drawRect(
                color = scrollbarColor,
                topLeft = Offset(horizontalThumbLeft, canvasHeight - scrollbarThicknessPx),
                size = Size(horizontalThumbWidth, scrollbarThicknessPx)
            )
        }
    }
}
