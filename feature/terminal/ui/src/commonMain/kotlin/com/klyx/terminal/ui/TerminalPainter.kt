package com.klyx.terminal.ui

import androidx.compose.ui.graphics.Canvas

internal expect class TerminalPainter(
    fontSizePx: Float,
    typeface: NativeTypeface
) {
    val fontWidth: Float
    val fontLineSpacing: Int
    val fontAscent: Int
    val fontLineSpacingAndAscent: Int

    fun measureAscii(codePoint: Int): Float
    fun measureText(text: CharArray, start: Int, count: Int): Float

    fun drawColor(canvas: Canvas, argb: Int)
    fun drawRect(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, argb: Int)
    fun drawTextRun(
        canvas: Canvas,
        text: CharArray,
        startCharIndex: Int,
        runWidthChars: Int,
        x: Float,
        y: Float,
        foreArgb: Int,
        bold: Boolean,
        underline: Boolean,
        italic: Boolean,
        strikeThrough: Boolean,
    )

    fun withScale(canvas: Canvas, sx: Float, block: () -> Unit)
}
