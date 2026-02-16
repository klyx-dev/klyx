package com.klyx.terminal.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import com.klyx.terminal.emulator.CursorStyle
import com.klyx.terminal.emulator.WcWidth
import com.klyx.util.toCodePoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.klyx.terminal.emulator.TextStyle as TerminalTextStyle

private class TerminalRendererNode(
    private var state: TerminalState,
    private var textMeasurer: TextMeasurer,
    private var fontSize: TextUnit,
    private var fontFamily: FontFamily,
    private var fontWidth: Int,
    private var fontLineSpacing: Int,
    private var fontAscent: Float,
    private var fontDescent: Float,
    private val baseTextStyle: TextStyle
) : Modifier.Node(), DrawModifierNode, ObserverModifierNode, CompositionLocalConsumerModifierNode {

    val fontLineSpacingAndAscent get() = fontLineSpacing + fontAscent

    private val asciiMeasures = FloatArray(127)

    init {
        // Pre-measure ASCII characters for performance
        val sb = StringBuilder(" ")
        for (i in asciiMeasures.indices) {
            sb[0] = i.toChar()
            asciiMeasures[i] = textMeasurer.measure(sb.toString(), baseTextStyle).size.width.toFloat()
        }
    }

    private var job: Job? = null

    override fun onAttach() {
        job = coroutineScope.launch {
            state.redraws.collect {
                invalidateDraw()
            }
        }

        observeReads {
            state.topRow
            //invalidateDraw()
        }
    }

    override fun onDetach() {
        job?.cancel()
    }

    override fun ContentDrawScope.draw() {
        val emulator = currentValueOf(LocalEmulator)

        if (emulator == null || fontWidth == 0 || fontLineSpacing == 0) {
            drawRect(color = Color.Black)
            return
        }

        val topRow = state.topRow.intValue
        val selectionY1 = state.selectionY1.intValue
        val selectionY2 = state.selectionY2.intValue
        val selectionX1 = state.selectionX1.intValue
        val selectionX2 = state.selectionX2.intValue

        val reverseVideo = emulator.isReverseVideo
        val endRow = topRow + emulator.rows
        val columns = emulator.columns
        val cursorCol = emulator.cursorColumn
        val cursorRow = emulator.cursorRow
        val cursorVisible = emulator.shouldCursorBeVisible()
        val screen = emulator.screen
        val palette = emulator.colors.currentColors
        val cursorShape = emulator.cursorStyle

        // Clear background with reverse video color if needed
        if (reverseVideo) {
            drawRect(
                color = Color(palette[TerminalTextStyle.COLOR_INDEX_FOREGROUND]),
                size = size
            )
        }

        var baselineY = 0f
        for (row in topRow until endRow) {
            baselineY += fontLineSpacing

            val cursorX = if (row == cursorRow && cursorVisible) cursorCol else -1
            var selx1 = -1
            var selx2 = -1

            if (row >= selectionY1 && row <= selectionY2) {
                if (row == selectionY1) selx1 = selectionX1
                selx2 = if (row == selectionY2) selectionX2 else emulator.columns
            }

            val lineObject = screen.allocateFullLineIfNecessary(screen.externalToInternalRow(row))
            val line = lineObject.text
            val charsUsedInLine = lineObject.spaceUsed

            var lastRunStyle = 0L
            var lastRunInsideCursor = false
            var lastRunInsideSelection = false
            var lastRunStartColumn = -1
            var lastRunStartIndex = 0
            var lastRunFontWidthMismatch = false
            var currentCharIndex = 0
            var measuredWidthForRun = 0f

            var column = 0
            while (column < columns) {
                val charAtIndex = line[currentCharIndex]
                val charIsHighSurrogate = charAtIndex.isHighSurrogate()
                val charsForCodePoint = if (charIsHighSurrogate) 2 else 1
                val codePoint = if (charIsHighSurrogate) {
                    Char.toCodePoint(charAtIndex, line[currentCharIndex + 1])
                } else {
                    charAtIndex.code
                }
                val codePointWcWidth = WcWidth.width(codePoint)
                val insideCursor = (cursorX == column || (codePointWcWidth == 2 && cursorX == column + 1))
                val insideSelection = column >= selx1 && column <= selx2
                val style = lineObject.getStyle(column)

                // Measure text width for this code point
                val measuredCodePointWidth = if (codePoint < asciiMeasures.size) {
                    asciiMeasures[codePoint]
                } else {
                    textMeasurer.measure(
                        line.concatToString(currentCharIndex, currentCharIndex + charsForCodePoint),
                        baseTextStyle
                    ).size.width.toFloat()
                }
                val fontWidthMismatch = abs(measuredCodePointWidth / fontWidth - codePointWcWidth) > 0.01

                if (style != lastRunStyle || insideCursor != lastRunInsideCursor ||
                    insideSelection != lastRunInsideSelection || fontWidthMismatch ||
                    lastRunFontWidthMismatch
                ) {
                    if (column != 0) {
                        val columnWidthSinceLastRun = column - lastRunStartColumn
                        val charsSinceLastRun = currentCharIndex - lastRunStartIndex
                        val cursorColor = if (lastRunInsideCursor) {
                            palette[TerminalTextStyle.COLOR_INDEX_CURSOR]
                        } else {
                            0
                        }
                        val invertCursorTextColor = lastRunInsideCursor && cursorShape == CursorStyle.Block

                        drawTextRun(
                            line, palette, baselineY, lastRunStartColumn,
                            columnWidthSinceLastRun, lastRunStartIndex, charsSinceLastRun,
                            measuredWidthForRun, cursorColor, cursorShape, lastRunStyle,
                            reverseVideo || invertCursorTextColor || lastRunInsideSelection
                        )
                    }

                    measuredWidthForRun = 0f
                    lastRunStyle = style
                    lastRunInsideCursor = insideCursor
                    lastRunInsideSelection = insideSelection
                    lastRunStartColumn = column
                    lastRunStartIndex = currentCharIndex
                    lastRunFontWidthMismatch = fontWidthMismatch
                }

                measuredWidthForRun += measuredCodePointWidth
                column += codePointWcWidth
                currentCharIndex += charsForCodePoint

                // eat combining characters
                while (currentCharIndex < charsUsedInLine &&
                    WcWidth.width(line, currentCharIndex) <= 0
                ) {
                    currentCharIndex += if (line[currentCharIndex].isHighSurrogate()) 2 else 1
                }
            }

            // draw final run
            val columnWidthSinceLastRun = columns - lastRunStartColumn
            val charsSinceLastRun = currentCharIndex - lastRunStartIndex
            val cursorColor = if (lastRunInsideCursor) {
                palette[TerminalTextStyle.COLOR_INDEX_CURSOR]
            } else {
                0
            }
            val invertCursorTextColor = lastRunInsideCursor && cursorShape == CursorStyle.Block

            drawTextRun(
                line, palette, baselineY, lastRunStartColumn, columnWidthSinceLastRun,
                lastRunStartIndex, charsSinceLastRun, measuredWidthForRun, cursorColor,
                cursorShape, lastRunStyle, reverseVideo || invertCursorTextColor || lastRunInsideSelection
            )
        }
    }

    fun update(
        state: TerminalState,
        textMeasurer: TextMeasurer,
        fontSize: TextUnit,
        fontFamily: FontFamily,
        fontWidth: Int,
        fontLineSpacing: Int,
        fontAscent: Float,
        fontDescent: Float
    ) {
        this.state = state
        this.textMeasurer = textMeasurer
        this.fontSize = fontSize
        this.fontFamily = fontFamily
        this.fontWidth = fontWidth
        this.fontLineSpacing = fontLineSpacing
        this.fontAscent = fontAscent
        this.fontDescent = fontDescent
    }

    private fun DrawScope.drawTextRun(
        text: CharArray,
        palette: IntArray,
        y: Float,
        startColumn: Int,
        runWidthColumns: Int,
        startCharIndex: Int,
        runWidthChars: Int,
        measuredWidth: Float,
        cursor: Int,
        cursorStyle: CursorStyle,
        textStyle: Long,
        reverseVideo: Boolean
    ) {
        var foreColor = TerminalTextStyle.decodeForeColor(textStyle)
        val effect = TerminalTextStyle.decodeEffect(textStyle)
        var backColor = TerminalTextStyle.decodeBackColor(textStyle)

        val bold = (effect and (TerminalTextStyle.CHARACTER_ATTRIBUTE_BOLD or
                TerminalTextStyle.CHARACTER_ATTRIBUTE_BLINK)) != 0
        val underline = (effect and TerminalTextStyle.CHARACTER_ATTRIBUTE_UNDERLINE) != 0
        val italic = (effect and TerminalTextStyle.CHARACTER_ATTRIBUTE_ITALIC) != 0
        val strikeThrough = (effect and TerminalTextStyle.CHARACTER_ATTRIBUTE_STRIKETHROUGH) != 0
        val dim = (effect and TerminalTextStyle.CHARACTER_ATTRIBUTE_DIM) != 0

        // handle color palette lookups
        if ((foreColor and 0xff000000.toInt()) != 0xff000000.toInt()) {
            if (bold && foreColor in 0..7) foreColor += 8
            foreColor = palette[foreColor]
        }

        if ((backColor and 0xff000000.toInt()) != 0xff000000.toInt()) {
            backColor = palette[backColor]
        }

        // reverse video
        val reverseVideoHere = reverseVideo xor ((effect and TerminalTextStyle.CHARACTER_ATTRIBUTE_INVERSE) != 0)
        if (reverseVideoHere) {
            val tmp = foreColor
            foreColor = backColor
            backColor = tmp
        }

        var left = (startColumn * fontWidth).toFloat()
        var right = left + runWidthColumns * fontWidth

        val mes = measuredWidth / fontWidth
        val needsScaling = abs(mes - runWidthColumns) > 0.01

        if (needsScaling) {
            scale(scaleX = runWidthColumns / mes, scaleY = 1f) {
                left *= mes / runWidthColumns
                right *= mes / runWidthColumns
                drawRunContent(
                    text, palette, y, left, right, startCharIndex, runWidthChars,
                    foreColor, backColor, cursor, cursorStyle, effect, bold,
                    underline, italic, strikeThrough, dim
                )
            }
        } else {
            drawRunContent(
                text, palette, y, left, right, startCharIndex, runWidthChars,
                foreColor, backColor, cursor, cursorStyle, effect, bold,
                underline, italic, strikeThrough, dim
            )
        }
    }

    private fun DrawScope.drawRunContent(
        text: CharArray,
        palette: IntArray,
        y: Float,
        left: Float,
        right: Float,
        startCharIndex: Int,
        runWidthChars: Int,
        foreColor: Int,
        backColor: Int,
        cursor: Int,
        cursorStyle: CursorStyle,
        effect: Int,
        bold: Boolean,
        underline: Boolean,
        italic: Boolean,
        strikeThrough: Boolean,
        dim: Boolean
    ) {
        // draw non-default background
        if (backColor != palette[TerminalTextStyle.COLOR_INDEX_BACKGROUND]) {
            drawRect(
                color = Color(backColor),
                topLeft = Offset(left, y - fontLineSpacingAndAscent + fontAscent),
                size = Size(right - left, fontLineSpacingAndAscent - fontAscent)
            )
        }

        // draw cursor
        if (cursor != 0) {
            val fullCursorHeight = fontLineSpacingAndAscent - fontAscent
            var cursorHeight = fullCursorHeight
            var cursorRight = right
            var cursorTop = y - fullCursorHeight

            when (cursorStyle) {
                CursorStyle.Underline -> {
                    cursorHeight /= 4f
                    cursorTop = y - cursorHeight
                }
                CursorStyle.Bar -> {
                    cursorRight -= ((right - left) * 3) / 4f
                }
            }

            drawRect(
                color = Color(cursor),
                topLeft = Offset(left, cursorTop),
                size = Size(cursorRight - left, cursorHeight)
            )
        }

        // draw text if not invisible
        if ((effect and TerminalTextStyle.CHARACTER_ATTRIBUTE_INVISIBLE) == 0) {
            var finalForeColor = foreColor

            if (dim) {
                val red = ((foreColor shr 16) and 0xFF) * 2 / 3
                val green = ((foreColor shr 8) and 0xFF) * 2 / 3
                val blue = (foreColor and 0xFF) * 2 / 3
                finalForeColor = 0xFF000000.toInt() or (red shl 16) or (green shl 8) or blue
            }

            val textStyle = baseTextStyle.copy(
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                textDecoration = when {
                    underline && strikeThrough -> TextDecoration.combine(
                        listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                    )

                    underline -> TextDecoration.Underline
                    strikeThrough -> TextDecoration.LineThrough
                    else -> TextDecoration.None
                },
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                color = Color(finalForeColor)
            )

            val textToDraw = text.concatToString(startCharIndex, startCharIndex + runWidthChars)
            val textLayoutResult = textMeasurer.measure(textToDraw, textStyle)

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(left, y - fontLineSpacingAndAscent + fontAscent)
            )
        } else {
            println("[Terminal] INVISIBLE TEXT")
        }
    }

    override fun onObservedReadsChanged() {
        invalidateDraw()
    }
}

private data class TerminalRendererNodeElement(
    val state: TerminalState,
    val textMeasurer: TextMeasurer,
    val fontFamily: FontFamily,
    val fontSize: TextUnit,
    val fontWidth: Int,
    val fontLineSpacing: Int,
    val fontAscent: Float,
    val fontDescent: Float,
    val baseTextStyle: TextStyle
) : ModifierNodeElement<TerminalRendererNode>() {

    override fun create() = TerminalRendererNode(
        state = state,
        textMeasurer = textMeasurer,
        fontSize = fontSize,
        fontFamily = fontFamily,
        fontWidth = fontWidth,
        fontLineSpacing = fontLineSpacing,
        fontAscent = fontAscent,
        fontDescent = fontDescent,
        baseTextStyle = baseTextStyle
    )

    override fun update(node: TerminalRendererNode) {
        node.update(
            state = state,
            textMeasurer = textMeasurer,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWidth = fontWidth,
            fontLineSpacing = fontLineSpacing,
            fontAscent = fontAscent,
            fontDescent = fontDescent
        )
    }
}

internal fun Modifier.renderTerminal(
    state: TerminalState,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    textMeasurer: TextMeasurer,
    fontMetrics: FontMetrics,
    baseTextStyle: TextStyle = TextStyle(fontFamily = fontFamily, fontSize = fontSize)
) = this then TerminalRendererNodeElement(
    state = state,
    textMeasurer = textMeasurer,
    fontFamily = fontFamily,
    fontSize = fontSize,
    fontWidth = fontMetrics.width,
    fontLineSpacing = fontMetrics.height,
    fontAscent = fontMetrics.ascent,
    fontDescent = fontMetrics.descent,
    baseTextStyle = baseTextStyle
)
