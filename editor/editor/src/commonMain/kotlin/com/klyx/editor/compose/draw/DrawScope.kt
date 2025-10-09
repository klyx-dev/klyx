package com.klyx.editor.compose.draw

import androidx.annotation.FloatRange
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScopeMarker
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.toPath

@Suppress("NOTHING_TO_INLINE")
@DrawScopeMarker
internal inline fun DrawScope.drawRect(
    color: Color,
    rect: Rect = Rect(Offset.Zero, size),
    @FloatRange(from = 0.0, to = 1.0)
    alpha: Float = 1.0f,
    style: DrawStyle = Fill,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode
) {
    drawRect(
        color = color,
        topLeft = rect.topLeft,
        size = rect.size,
        alpha = alpha,
        style = style,
        colorFilter = colorFilter,
        blendMode = blendMode
    )
}

@Suppress("NOTHING_TO_INLINE")
@DrawScopeMarker
internal inline fun DrawScope.drawColor(color: Color) {
    if (color.isSpecified) drawRect(color)
}

internal inline fun buildPath(block: PathBuilder.() -> Unit) = PathData(block).toPath()
