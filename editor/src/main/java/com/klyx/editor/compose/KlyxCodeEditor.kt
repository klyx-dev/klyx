package com.klyx.editor.compose

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    gutterWidth: Dp = 40.dp
) {
    val scrollY = remember { mutableFloatStateOf(0f) }
    val scrollX = remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val lineHeightPx = with(density) { lineHeight.toPx() }
    val lineSpacingPx = with(density) { 4.dp.toPx() }
    val fullLineHeightPx = lineHeightPx + lineSpacingPx
    val horizontalPaddingPx = with(density) { horizontalPadding.toPx() }
    val gutterWidthPx = with(density) { gutterWidth.toPx() }
    val scrollbarThicknessPx = with(density) { scrollbarThickness.toPx() }
    val endHorizontalPaddingPx = if (wrapText) 0f else with(density) { 50.dp.toPx() }

    var draggingVerticalScrollbar by remember { mutableStateOf(false) }
    var draggingHorizontalScrollbar by remember { mutableStateOf(false) }
    var verticalDragOffset by remember { mutableFloatStateOf(0f) }
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(text, wrapText) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        val paint = Paint().apply {
                            textSize = lineHeightPx
                            isAntiAlias = true
                            this.typeface = typeface
                        }

                        val allLines = buildList {
                            for (originalLine in text.lines()) {
                                if (!wrapText) {
                                    add(originalLine)
                                } else {
                                    if (originalLine.isEmpty()) {
                                        add("")
                                    } else {
                                        var remaining = originalLine
                                        while (remaining.isNotEmpty()) {
                                            val wrapWidth = canvasWidth - horizontalPaddingPx * 2
                                            val count = paint.breakText(remaining, true, wrapWidth, null)
                                            if (count == 0) break // Prevent infinite loop
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
                            (maxLineWidth - canvasWidth + horizontalPaddingPx + endHorizontalPaddingPx).coerceAtLeast(0f)
                        } else 0f

                        // Only calculate scrollbar positions if there's content to scroll
                        val verticalThumbHeight = if (verticalLimit > 0f) {
                            (canvasHeight / contentHeight * canvasHeight).coerceAtLeast(20f)
                        } else canvasHeight.toFloat()

                        val verticalThumbTop = if (verticalLimit > 0f) {
                            (scrollY.floatValue / verticalLimit * (canvasHeight - verticalThumbHeight))
                                .coerceIn(0f, canvasHeight - verticalThumbHeight)
                        } else 0f

                        val horizontalThumbWidth = if (!wrapText && horizontalLimit > 0f) {
                            (canvasWidth / (maxLineWidth + horizontalPaddingPx + endHorizontalPaddingPx) * canvasWidth).coerceAtLeast(20f)
                        } else 0f

                        val horizontalThumbLeft = if (!wrapText && horizontalLimit > 0f) {
                            (scrollX.floatValue / horizontalLimit * (canvasWidth - horizontalThumbWidth))
                                .coerceIn(0f, canvasWidth - horizontalThumbWidth)
                        } else 0f

                        val inVerticalScrollbarX = offset.x >= canvasWidth - scrollbarThicknessPx && offset.x <= canvasWidth
                        val inVerticalScrollbarY = offset.y >= verticalThumbTop && offset.y <= verticalThumbTop + verticalThumbHeight

                        val inHorizontalScrollbarY = offset.y >= canvasHeight - scrollbarThicknessPx && offset.y <= canvasHeight
                        val inHorizontalScrollbarX = offset.x >= horizontalThumbLeft && offset.x <= horizontalThumbLeft + horizontalThumbWidth

                        draggingVerticalScrollbar = verticalLimit > 0f && inVerticalScrollbarX && inVerticalScrollbarY
                        draggingHorizontalScrollbar = !wrapText && horizontalLimit > 0f && inHorizontalScrollbarY && inHorizontalScrollbarX

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

                        val paint = Paint().apply {
                            textSize = lineHeightPx
                            isAntiAlias = true
                            this.typeface = typeface
                        }

                        val allLines = buildList {
                            for (originalLine in text.lines()) {
                                if (!wrapText) {
                                    add(originalLine)
                                } else {
                                    if (originalLine.isEmpty()) {
                                        add("")
                                    } else {
                                        var remaining = originalLine
                                        while (remaining.isNotEmpty()) {
                                            val wrapWidth = canvasWidth - horizontalPaddingPx * 2
                                            val count = paint.breakText(remaining, true, wrapWidth, null)
                                            if (count == 0) break // Prevent infinite loop
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
                            (maxLineWidth - canvasWidth + horizontalPaddingPx + endHorizontalPaddingPx).coerceAtLeast(0f)
                        } else 0f

                        if (draggingVerticalScrollbar && verticalLimit > 0f) {
                            val verticalThumbHeight = (canvasHeight / contentHeight * canvasHeight).coerceAtLeast(20f)
                            val newThumbTop = (change.position.y - verticalDragOffset).coerceIn(0f, canvasHeight - verticalThumbHeight)
                            scrollY.floatValue = (newThumbTop / (canvasHeight - verticalThumbHeight)) * verticalLimit
                        } else if (draggingHorizontalScrollbar && !wrapText && horizontalLimit > 0f) {
                            val horizontalThumbWidth = (canvasWidth / (maxLineWidth + horizontalPaddingPx + endHorizontalPaddingPx) * canvasWidth)
                                .coerceAtLeast(20f)
                            val newThumbLeft = (change.position.x - horizontalDragOffset).coerceIn(0f, canvasWidth - horizontalThumbWidth)
                            scrollX.floatValue = (newThumbLeft / (canvasWidth - horizontalThumbWidth)) * horizontalLimit
                        } else {
                            scrollY.floatValue = (scrollY.floatValue - dragAmount.y).coerceIn(0f, verticalLimit)
                            if (!wrapText) {
                                scrollX.floatValue = (scrollX.floatValue - dragAmount.x).coerceIn(0f, horizontalLimit)
                            }
                        }
                    }
                )
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val paint = Paint().apply {
            textSize = lineHeightPx
            color = Color.White.toArgb()
            isAntiAlias = true
            this.typeface = typeface
        }

        val allLines = buildList {
            for (originalLine in text.lines()) {
                if (!wrapText) {
                    add(originalLine)
                } else {
                    if (originalLine.isEmpty()) {
                        add("")
                    } else {
                        var remaining = originalLine
                        while (remaining.isNotEmpty()) {
                            val wrapWidth = canvasWidth - horizontalPaddingPx * 2
                            val count = paint.breakText(remaining, true, wrapWidth, null)
                            if (count == 0) break // Prevent infinite loop
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
        scrollY.floatValue = scrollY.floatValue.coerceIn(0f, verticalLimit)

        val maxLineWidth = allLines.maxOfOrNull { paint.measureText(it) } ?: 0f
        val horizontalLimit = if (!wrapText) {
            (maxLineWidth - canvasWidth + horizontalPaddingPx + endHorizontalPaddingPx).coerceAtLeast(0f)
        } else 0f

        if (!wrapText) {
            scrollX.floatValue = scrollX.floatValue.coerceIn(0f, horizontalLimit)
        } else {
            scrollX.floatValue = 0f
        }

        val firstVisibleIndex = (scrollY.floatValue / fullLineHeightPx).toInt().coerceAtLeast(0)
        val visibleCount = (canvasHeight / fullLineHeightPx).toInt() + 2
        val lastVisibleIndex = (firstVisibleIndex + visibleCount).coerceAtMost(allLines.size)

        for (i in firstVisibleIndex until lastVisibleIndex) {
            val line = allLines[i]
            val y = i * fullLineHeightPx - scrollY.floatValue + lineHeightPx
            drawContext.canvas.nativeCanvas.drawText(
                line,
                horizontalPaddingPx - scrollX.floatValue,
                y,
                paint
            )
        }

        // Only draw vertical scrollbar if there's content to scroll
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

        // Only draw horizontal scrollbar if there's content to scroll
        if (!wrapText && horizontalLimit > 0f) {
            val horizontalThumbWidth = (canvasWidth / (maxLineWidth + horizontalPaddingPx + endHorizontalPaddingPx) * canvasWidth)
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
