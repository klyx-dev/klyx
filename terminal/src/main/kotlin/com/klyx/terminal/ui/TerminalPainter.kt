package com.klyx.terminal.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.ceil

internal class TerminalPainter(
    fontSizePx: Float,
    typeface: Typeface
) {
    private val regularTypeface = typeface
    private val boldTypeface = Typeface.create(typeface, Typeface.BOLD)

    private val paint = Paint().apply {
        isAntiAlias = true
        this.typeface = regularTypeface
        textSize = fontSizePx
    }

    val fontWidth = paint.measureText("X")
    val fontLineSpacing = ceil(paint.fontSpacing).toInt()
    val fontAscent = ceil(paint.ascent()).toInt()
    val fontLineSpacingAndAscent = fontLineSpacing + fontAscent

    private val asciiMeasures = FloatArray(127).also { arr ->
        val sb = StringBuilder(" ")
        for (i in arr.indices) {
            sb[0] = i.toChar()
            arr[i] = paint.measureText(sb, 0, 1)
        }
    }

    fun measureAscii(codePoint: Int) = asciiMeasures[codePoint]
    fun measureText(text: CharArray, start: Int, count: Int) = paint.measureText(text, start, count)

    fun drawColor(canvas: Canvas, argb: Int) {
        canvas.nativeCanvas.drawColor(argb)
    }

    fun drawRect(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        argb: Int
    ) {
        paint.color = argb
        canvas.nativeCanvas.drawRect(left, top, right, bottom, paint)
    }

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
        strikeThrough: Boolean
    ) {
        paint.typeface = if (bold) boldTypeface else regularTypeface
        paint.isFakeBoldText = false
        paint.isUnderlineText = underline
        paint.textSkewX = if (italic) -0.35f else 0f
        paint.isStrikeThruText = strikeThrough
        paint.color = foreArgb

        canvas.nativeCanvas.drawTextRun(
            text,
            startCharIndex, runWidthChars,
            startCharIndex, runWidthChars,
            x,
            y - fontLineSpacingAndAscent,
            false,
            paint
        )
    }

    fun withScale(canvas: Canvas, sx: Float, block: () -> Unit) {
        val native = canvas.nativeCanvas
        native.save()
        native.scale(sx, 1f)
        block()
        native.restore()
    }
}
