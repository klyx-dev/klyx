package com.klyx.editor.compose.renderer

import androidx.annotation.MainThread
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import com.klyx.core.PlatformContext

interface PlatformTextRenderer {
    fun getFontMetrics(fontSize: TextUnit, fontFamily: FontFamily): FontMetrics

    /**
     * Returns the width of the text.
     */
    fun measureText(text: String, style: TextStyle = TextStyle.Default): Float

    @MainThread
    fun drawText(
        canvas: NativeCanvas,
        text: String,
        style: TextStyle = TextStyle.Default,
        topLeft: Offset = Offset.Zero,
        paint: Paint = Paint()
    )
}

expect fun PlatformTextRenderer(context: PlatformContext, density: Density): PlatformTextRenderer
