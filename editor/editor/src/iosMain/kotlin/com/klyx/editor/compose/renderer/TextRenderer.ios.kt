package com.klyx.editor.compose.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import com.klyx.core.PlatformContext

internal class IOSTextRenderer : TextRenderer {
    override fun getFontMetrics(
        fontSize: TextUnit,
        fontFamily: FontFamily
    ): FontMetrics {
        TODO("Not yet implemented")
    }

    override fun measureText(text: String, style: TextStyle): Float {
        TODO("Not yet implemented")
    }

    override fun drawText(
        canvas: NativeCanvas,
        text: String,
        style: TextStyle,
        topLeft: Offset,
        paint: Paint
    ) {
        TODO("Not yet implemented")
    }
}

actual fun TextRenderer(context: PlatformContext, density: Density): TextRenderer = IOSTextRenderer()
