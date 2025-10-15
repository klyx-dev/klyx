package com.klyx.editor.compose

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Typeface

fun renderTextExample(canvas: Canvas) {
    val paint = Paint().apply {
        color = 0xFF000000.toInt()
        isAntiAlias = true
    }

    val font = Font(Typeface.makeEmpty(), 16f)
    val metrics = font.metrics

    println("ascent: ${metrics.ascent}, descent: ${metrics.descent}, height: ${metrics.capHeight}")

    val text = "Hello KMP"
    val width = font.measureTextWidth(text)
    println("text width: $width")

    canvas.drawString(text, 10f, 50f, font, paint)
}
