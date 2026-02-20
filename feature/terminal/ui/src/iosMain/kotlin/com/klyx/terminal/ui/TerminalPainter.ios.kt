package com.klyx.terminal.ui

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.TextBlobBuilder
import kotlin.math.ceil

internal actual class TerminalPainter actual constructor(
    fontSizePx: Float,
    typeface: NativeTypeface
) {
    private val regularTypeface = typeface
    private val boldTypeface = regularTypeface

    private val regularFont = Font(regularTypeface, fontSizePx)
    private val boldFont = Font(boldTypeface, fontSizePx)

    private val paint = Paint()

    private val skiaMetrics = regularFont.metrics

    actual val fontWidth = regularFont.measureTextWidth("X")
    actual val fontLineSpacing = ceil(regularFont.spacing).toInt()

    // Skia ascent is negative (above baseline); we store its absolute value
    actual val fontAscent = ceil(-skiaMetrics.ascent).toInt()
    actual val fontLineSpacingAndAscent = fontLineSpacing + fontAscent

    private val asciiMeasures = FloatArray(127).also { arr ->
        for (i in arr.indices) {
            arr[i] = regularFont.measureTextWidth(i.toChar().toString())
        }
    }

    actual fun measureAscii(codePoint: Int) = asciiMeasures[codePoint]
    actual fun measureText(text: CharArray, start: Int, count: Int) =
        regularFont.measureTextWidth(text.concatToString(start, start + count))

    actual fun drawColor(canvas: Canvas, argb: Int) {
        paint.color = argb
        canvas.nativeCanvas.drawPaint(paint)
    }

    actual fun drawRect(
        canvas: Canvas,
        left: Float, top: Float, right: Float, bottom: Float,
        argb: Int,
    ) {
        paint.color = argb
        canvas.nativeCanvas.drawRect(Rect.makeLTRB(left, top, right, bottom), paint)
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
        strikeThrough: Boolean,
    ) {
        paint.color = foreArgb

        val font = if (bold) boldFont else regularFont
        val str = text.concatToString(startCharIndex, startCharIndex + runWidthChars)

        val baselineY = y - fontLineSpacingAndAscent + fontAscent

        val blob = TextBlobBuilder().run {
            appendRun(font, str, x, baselineY)
            build()
        }

        val nc = canvas.nativeCanvas
        if (blob != null) {
            nc.drawTextBlob(blob, 0f, 0f, paint)
            blob.close()
        }

        // Skia doesn't have Paint-level underline/strikethrough flags. draw manually
        if (underline || strikeThrough) {
            val textWidth = font.measureTextWidth(str)
            paint.color = foreArgb
            if (underline) {
                val uy = baselineY + 1.5f
                nc.drawRect(Rect.makeLTRB(x, uy, x + textWidth, uy + 1f), paint)
            }
            if (strikeThrough) {
                val sy = baselineY - (fontAscent / 2f)
                nc.drawRect(Rect.makeLTRB(x, sy, x + textWidth, sy + 1f), paint)
            }
        }
    }

    actual fun withScale(canvas: Canvas, sx: Float, block: () -> Unit) {
        val nc = canvas.nativeCanvas
        nc.save()
        nc.scale(sx, 1f)
        block()
        nc.restore()
    }
}
