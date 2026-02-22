package com.klyx.terminal.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import com.klyx.terminal.emulator.CursorStyle
import com.klyx.terminal.emulator.TextStyle
import com.klyx.terminal.emulator.WcWidth
import com.klyx.util.toCodePoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

private class TerminalRendererNode(
    private var state: TerminalState,
    private var painter: TerminalPainter
) : Modifier.Node(), DrawModifierNode, ObserverModifierNode, CompositionLocalConsumerModifierNode {

    private var job: Job? = null

    override fun onAttach() {
        job = coroutineScope.launch {
            state.redraws.collect { invalidateDraw() }
        }
        observeReads { state.topRow }
    }

    override fun onDetach() {
        job?.cancel()
    }

    override fun ContentDrawScope.draw() {
        val emulator = currentValueOf(LocalEmulator)
        println("DEBUG draw() size=${size.width}x${size.height} rows=${emulator?.rows} cols=${emulator?.columns}")

        if (emulator == null || painter.fontWidth == 0f || painter.fontLineSpacing == 0) {
            drawIntoCanvas { painter.drawRect(it, 0f, 0f, size.width, size.height, 0xFF000000.toInt()) }
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

        drawIntoCanvas { canvas ->
            if (reverseVideo) painter.drawColor(canvas, palette[TextStyle.COLOR_INDEX_FOREGROUND])

            var heightOffset = painter.fontLineSpacingAndAscent.toFloat()
            for (row in topRow until endRow) {
                heightOffset += painter.fontLineSpacing

                val cursorX = if (row == cursorRow && cursorVisible) cursorCol else -1
                var selx1 = -1
                var selx2 = -1
                if (row in selectionY1..selectionY2) {
                    selx1 = if (row == selectionY1) selectionX1 else 0
                    selx2 = if (row == selectionY2) selectionX2 else columns
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
                    val insideCursor = cursorX == column || (codePointWcWidth == 2 && cursorX == column + 1)
                    val insideSelection = column in selx1..selx2
                    val style = lineObject.getStyle(column)

                    val measuredCodePointWidth = if (codePoint < 127) {
                        painter.measureAscii(codePoint)
                    } else {
                        painter.measureText(line, currentCharIndex, charsForCodePoint)
                    }
                    val fontWidthMismatch = abs(measuredCodePointWidth / painter.fontWidth - codePointWcWidth) > 0.01

                    if (
                        style != lastRunStyle ||
                        insideCursor != lastRunInsideCursor ||
                        insideSelection != lastRunInsideSelection ||
                        fontWidthMismatch || lastRunFontWidthMismatch
                    ) {
                        if (column != 0) {
                            drawRun(
                                canvas = canvas,
                                text = line,
                                palette = palette,
                                y = heightOffset,
                                startColumn = lastRunStartColumn,
                                runWidthColumns = column - lastRunStartColumn,
                                startCharIndex = lastRunStartIndex,
                                runWidthChars = currentCharIndex - lastRunStartIndex,
                                measuredWidth = measuredWidthForRun,
                                cursorStyle = cursorShape,
                                textStyle = lastRunStyle,
                                reverseVideo = reverseVideo || (lastRunInsideCursor && cursorShape == CursorStyle.Block) || lastRunInsideSelection,
                                insideCursor = lastRunInsideCursor
                            )
                        }
                        measuredWidthForRun = 0f; lastRunStyle = style
                        lastRunInsideCursor = insideCursor; lastRunInsideSelection = insideSelection
                        lastRunStartColumn = column; lastRunStartIndex = currentCharIndex
                        lastRunFontWidthMismatch = fontWidthMismatch
                    }

                    measuredWidthForRun += measuredCodePointWidth
                    column += codePointWcWidth
                    currentCharIndex += charsForCodePoint

                    while (currentCharIndex < charsUsedInLine && WcWidth.width(line, currentCharIndex) <= 0) {
                        currentCharIndex += if (line[currentCharIndex].isHighSurrogate()) 2 else 1
                    }
                }

                // final run
                drawRun(
                    canvas = canvas,
                    text = line,
                    palette = palette,
                    y = heightOffset,
                    startColumn = lastRunStartColumn,
                    runWidthColumns = columns - lastRunStartColumn,
                    startCharIndex = lastRunStartIndex,
                    runWidthChars = currentCharIndex - lastRunStartIndex,
                    measuredWidth = measuredWidthForRun,
                    cursorStyle = cursorShape,
                    textStyle = lastRunStyle,
                    reverseVideo = reverseVideo || (lastRunInsideCursor && cursorShape == CursorStyle.Block) || lastRunInsideSelection,
                    insideCursor = lastRunInsideCursor
                )
            }
        }
    }

    private fun drawRun(
        canvas: Canvas,
        text: CharArray,
        palette: IntArray,
        y: Float,
        startColumn: Int,
        runWidthColumns: Int,
        startCharIndex: Int,
        runWidthChars: Int,
        measuredWidth: Float,
        cursorStyle: CursorStyle,
        textStyle: Long,
        reverseVideo: Boolean,
        insideCursor: Boolean
    ) {
        var foreColor = TextStyle.decodeForeColor(textStyle)
        val effect = TextStyle.decodeEffect(textStyle)
        var backColor = TextStyle.decodeBackColor(textStyle)

        val bold = (effect and (TextStyle.CHARACTER_ATTRIBUTE_BOLD or TextStyle.CHARACTER_ATTRIBUTE_BLINK)) != 0
        val underline = (effect and TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE) != 0
        val italic = (effect and TextStyle.CHARACTER_ATTRIBUTE_ITALIC) != 0
        val strikeThrough = (effect and TextStyle.CHARACTER_ATTRIBUTE_STRIKETHROUGH) != 0
        val dim = (effect and TextStyle.CHARACTER_ATTRIBUTE_DIM) != 0
        val invisible = (effect and TextStyle.CHARACTER_ATTRIBUTE_INVISIBLE) != 0

        if ((foreColor and 0xff000000.toInt()) != 0xff000000.toInt()) {
            if (bold && foreColor in 0..7) foreColor += 8
            foreColor = palette[foreColor]
        }
        if ((backColor and 0xff000000.toInt()) != 0xff000000.toInt()) backColor = palette[backColor]

        val reverseHere = reverseVideo xor ((effect and TextStyle.CHARACTER_ATTRIBUTE_INVERSE) != 0)
        if (reverseHere) {
            val t = foreColor; foreColor = backColor; backColor = t
        }

        var left = startColumn * painter.fontWidth
        var right = left + runWidthColumns * painter.fontWidth

        val mes = measuredWidth / painter.fontWidth
        val needScale = abs(mes - runWidthColumns) > 0.01

        val drawContent = {
            if (backColor != palette[TextStyle.COLOR_INDEX_BACKGROUND]) {
                painter.drawRect(
                    canvas = canvas,
                    left = left,
                    top = y - painter.fontLineSpacingAndAscent + painter.fontAscent,
                    right = right,
                    bottom = y,
                    argb = backColor
                )
            }

            if (insideCursor) {
                val fullH = (painter.fontLineSpacingAndAscent - painter.fontAscent).toFloat()
                var cr = right
                var ch = fullH
                var ct = y - fullH
                when (cursorStyle) {
                    CursorStyle.Underline -> {
                        ch = fullH / 4f; ct = y - ch
                    }

                    CursorStyle.Bar -> cr -= ((right - left) * 3f) / 4f
                    else -> Unit
                }
                painter.drawRect(canvas, left, ct, cr, y, palette[TextStyle.COLOR_INDEX_CURSOR])
            }

            if (!invisible) {
                var fg = foreColor
                if (dim) {
                    fg = (0xFF000000.toInt()) or
                            (((fg shr 16) and 0xFF) * 2 / 3 shl 16) or
                            (((fg shr 8) and 0xFF) * 2 / 3 shl 8) or
                            ((fg and 0xFF) * 2 / 3)
                }
                painter.drawTextRun(
                    canvas = canvas,
                    text = text,
                    startCharIndex = startCharIndex,
                    runWidthChars = runWidthChars,
                    x = left,
                    y = y,
                    foreArgb = fg,
                    bold = bold,
                    underline = underline,
                    italic = italic,
                    strikeThrough = strikeThrough
                )
            }
        }

        if (needScale) {
            val scaledLeft = left * (mes / runWidthColumns)
            val scaledRight = right * (mes / runWidthColumns)
            left = scaledLeft; right = scaledRight
            painter.withScale(canvas, runWidthColumns / mes, drawContent)
        } else {
            drawContent()
        }
    }

    override fun onObservedReadsChanged() {
        invalidateDraw()
    }

    fun update(state: TerminalState, painter: TerminalPainter) {
        this.state = state; this.painter = painter
    }
}

private data class TerminalRendererNodeElement(
    val state: TerminalState,
    val painter: TerminalPainter
) : ModifierNodeElement<TerminalRendererNode>() {

    override fun create() = TerminalRendererNode(state, painter)
    override fun update(node: TerminalRendererNode) = node.update(state, painter)
}

internal fun Modifier.renderTerminal(
    state: TerminalState,
    painter: TerminalPainter
) = this then TerminalRendererNodeElement(state, painter)
