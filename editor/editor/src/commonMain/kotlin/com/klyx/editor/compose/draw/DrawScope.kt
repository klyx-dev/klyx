@file:Suppress("NOTHING_TO_INLINE")

package com.klyx.editor.compose.draw

import androidx.annotation.FloatRange
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.isSpecified

inline fun DrawScope.drawRect(
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

inline fun DrawScope.drawColor(color: Color) {
    if (color.isSpecified) drawRect(color)
}
