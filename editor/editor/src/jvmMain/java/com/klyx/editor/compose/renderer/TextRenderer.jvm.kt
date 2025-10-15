package com.klyx.editor.compose.renderer

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import com.klyx.core.PlatformContext
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine
import org.jetbrains.skia.Typeface

internal class JvmTextRenderer(private val density: Density) : TextRenderer {
    private val resolver = createFontFamilyResolver()

    override fun getFontMetrics(fontSize: TextUnit, fontFamily: FontFamily): FontMetrics {
        val typeface by resolver.resolveAsTypeface(fontFamily)
        val font = Font(typeface, with(density) { fontSize.toPx() })
        val fm = font.metrics
        return FontMetrics(fm.top, fm.ascent, fm.descent, fm.bottom, fm.leading)
    }

    override fun measureText(text: String, style: TextStyle): Float {
        val typeface by resolver.resolveAsTypeface(
            fontFamily = style.fontFamily,
            fontWeight = style.fontWeight ?: FontWeight.Normal,
            fontStyle = style.fontStyle ?: FontStyle.Normal,
            fontSynthesis = style.fontSynthesis ?: FontSynthesis.All
        )

        val font = Font(typeface, with(density) { style.fontSize.toPx() })
        return font.measureTextWidth(text)
    }

    override fun drawText(
        canvas: NativeCanvas,
        text: String,
        style: TextStyle,
        topLeft: Offset,
        paint: Paint
    ) {
        val (x, y) = topLeft
        val typeface by resolver.resolveAsTypeface(
            fontFamily = style.fontFamily,
            fontWeight = style.fontWeight ?: FontWeight.Normal,
            fontStyle = style.fontStyle ?: FontStyle.Normal,
            fontSynthesis = style.fontSynthesis ?: FontSynthesis.All
        )

        val font = Font(typeface, with(density) { style.fontSize.toPx() })

        canvas.drawTextLine(
            line = TextLine.make(text, font),
            x = x,
            y = y,
            paint = paint.asFrameworkPaint().apply {
                isAntiAlias = true
                color = style.color.toArgb()
            }
        )
    }

    private fun FontFamily.Resolver.resolveAsTypeface(
        fontFamily: FontFamily? = null,
        fontWeight: FontWeight = FontWeight.Normal,
        fontStyle: FontStyle = FontStyle.Normal,
        fontSynthesis: FontSynthesis = FontSynthesis.All,
    ): State<Typeface> {
        @Suppress("UNCHECKED_CAST")
        return resolve(fontFamily, fontWeight, fontStyle, fontSynthesis) as State<Typeface>
    }
}

actual fun TextRenderer(context: PlatformContext, density: Density): TextRenderer = JvmTextRenderer(density)
