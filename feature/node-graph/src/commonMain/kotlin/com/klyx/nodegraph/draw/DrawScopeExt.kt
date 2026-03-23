package com.klyx.nodegraph.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private fun niceStep(targetScreenPx: Float, scale: Float): Float {
    val raw = targetScreenPx / scale
    val magnitude = 10.0.pow(floor(log10(raw.toDouble()))).toFloat()
    val normalized = raw / magnitude
    return when {
        normalized < 1.5f -> magnitude
        normalized < 3.5f -> magnitude * 2f
        normalized < 7.5f -> magnitude * 5f
        else -> magnitude * 10f
    }
}

internal fun DrawScope.drawInfiniteGridDot(pan: Offset, scale: Float) {
    val minorStep = niceStep(40f, scale)
    val majorStep = niceStep(200f, scale)

    fun dots(graphStep: Float, radius: Float, color: Color) {
        val firstX = ceil(-pan.x / scale / graphStep) * graphStep
        val firstY = ceil(-pan.y / scale / graphStep) * graphStep

        var gx = firstX
        while (true) {
            val sx = gx * scale + pan.x
            if (sx > size.width + 1f) break
            var gy = firstY
            while (true) {
                val sy = gy * scale + pan.y
                if (sy > size.height + 1f) break
                drawCircle(
                    color = color,
                    radius = radius,
                    center = Offset(sx, sy),
                )
                gy += graphStep
            }
            gx += graphStep
        }
    }

    dots(minorStep, radius = 1.2f, color = Color(0xFF2A2A40))
    dots(majorStep, radius = 2.2f, color = Color(0xFF3A3A58))
}

internal fun DrawScope.drawInfiniteGridLines(pan: Offset, scale: Float) {
    val minorStep = niceStep(40f, scale)
    val majorStep = niceStep(200f, scale)

    fun lines(graphStep: Float, color: Color, strokeWidth: Float) {
        val firstX = ceil((-pan.x / scale / graphStep)) * graphStep
        val firstY = ceil((-pan.y / scale / graphStep)) * graphStep

        var gx = firstX
        while (true) {
            val sx = gx * scale + pan.x
            if (sx > size.width + 1f) break
            drawLine(color, Offset(sx, 0f), Offset(sx, size.height), strokeWidth)
            gx += graphStep
        }

        var gy = firstY
        while (true) {
            val sy = gy * scale + pan.y
            if (sy > size.height + 1f) break
            drawLine(color, Offset(0f, sy), Offset(size.width, sy), strokeWidth)
            gy += graphStep
        }
    }

    lines(minorStep, Color(0xFF1A1A2C), 1f)
    lines(majorStep, Color(0xFF26263C), 1.5f)
}

internal fun DrawScope.drawBezierWire(
    start: Offset,
    end: Offset,
    startColor: Color,
    endColor: Color = startColor,
    alpha: Float = 1f,
    thickness: Float = 2.5f,
    dashIntervals: FloatArray? = null,
    dashPhase: Float = 0f,
) {
    val tangent = abs(end.x - start.x).coerceAtLeast(80f) * 0.5f
    val path = Path().apply {
        moveTo(start.x, start.y)
        cubicTo(start.x + tangent, start.y, end.x - tangent, end.y, end.x, end.y)
    }
    val effect = dashIntervals?.let { PathEffect.dashPathEffect(it, dashPhase) }

    val glowBrush = Brush.linearGradient(
        colors = listOf(
            startColor.copy(alpha = alpha * 0.22f),
            endColor.copy(alpha = alpha * 0.22f),
        ),
        start = start,
        end = end,
    )

    val coreBrush = Brush.linearGradient(
        colors = listOf(
            startColor.copy(alpha = alpha),
            endColor.copy(alpha = alpha),
        ),
        start = start,
        end = end,
    )

    drawPath(path, brush = glowBrush, style = Stroke(width = thickness * 4f))
    drawPath(path, brush = coreBrush, style = Stroke(width = thickness, pathEffect = effect))
}
