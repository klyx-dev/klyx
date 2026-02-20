package com.klyx.terminal.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.ceil

internal actual class TerminalPainter actual constructor(
    fontSizePx: Float,
    typeface: NativeTypeface
) {
    private val regularTypeface = typeface
    private val boldTypeface = Typeface.create(typeface, Typeface.BOLD)

    private val paint = Paint().apply {
        isAntiAlias = true
        this.typeface = regularTypeface
        textSize = fontSizePx
    }

    actual val fontWidth = paint.measureText("X")
    actual val fontLineSpacing = ceil(paint.fontSpacing).toInt()
    actual val fontAscent = ceil(paint.ascent()).toInt()
    actual val fontLineSpacingAndAscent = fontLineSpacing + fontAscent

    private val asciiMeasures = FloatArray(127).also { arr ->
        val sb = StringBuilder(" ")
        for (i in arr.indices) {
            sb[0] = i.toChar()
            arr[i] = paint.measureText(sb, 0, 1)
        }
    }

    actual fun measureAscii(codePoint: Int) = asciiMeasures[codePoint]
    actual fun measureText(text: CharArray, start: Int, count: Int) = paint.measureText(text, start, count)

    actual fun drawColor(canvas: Canvas, argb: Int) {
        canvas.nativeCanvas.drawColor(argb)
    }

    actual fun drawRect(
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

    actual fun drawTextRun(
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

    actual fun withScale(canvas: Canvas, sx: Float, block: () -> Unit) {
        val native = canvas.nativeCanvas
        native.save()
        native.scale(sx, 1f)
        block()
        native.restore()
    }
}
