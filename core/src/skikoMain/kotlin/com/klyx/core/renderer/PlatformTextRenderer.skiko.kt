package com.klyx.core.renderer

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
import org.jetbrains.skia.shaper.ShapingOptions

internal class SkikoTextRenderer(private val density: Density) : PlatformTextRenderer {
    private val resolver = createFontFamilyResolver()

    override fun getFontSpacing(fontSize: TextUnit, fontFamily: FontFamily): Float {
        val typeface by resolver.resolveAsTypeface(fontFamily)
        val font = Font(typeface, with(density) { fontSize.toPx() })
        return font.spacing
    }

    override fun getFontMetrics(fontSize: TextUnit, fontFamily: FontFamily): FontMetrics {
        val typeface by resolver.resolveAsTypeface(fontFamily)
        val font = Font(typeface, with(density) { fontSize.toPx() })
        val fm = font.metrics
        return FontMetrics(fm.top, fm.ascent, fm.descent, fm.bottom, fm.leading)
    }

    override fun measureText(text: CharSequence, start: Int, end: Int, style: TextStyle): Float {
        val typeface by resolver.resolveAsTypeface(
            fontFamily = style.fontFamily,
            fontWeight = style.fontWeight ?: FontWeight.Normal,
            fontStyle = style.fontStyle ?: FontStyle.Normal,
            fontSynthesis = style.fontSynthesis ?: FontSynthesis.All
        )

        val font = Font(typeface, with(density) { style.fontSize.toPx() })
        return font.measureTextWidth(text.substring(start, end))
    }

    override fun measureText(text: CharArray, index: Int, count: Int, style: TextStyle): Float {
        return measureText(text.concatToString(), index, index + count, style)
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

    override fun drawTextRun(
        canvas: NativeCanvas,
        text: CharArray,
        index: Int,
        count: Int,
        contextIndex: Int,
        contextCount: Int,
        topLeft: Offset,
        isRtl: Boolean,
        style: TextStyle,
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

        // todo: draw text run
        canvas.drawTextLine(
            line = TextLine.make(
                text = text.concatToString(index, index + count),
                font = font,
                opts = ShapingOptions(
                    fontMgr = null,
                    features = null,
                    isLeftToRight = isRtl,
                    isApproximateSpaces = true,
                    isApproximatePunctuation = true
                )
            ),
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

actual fun PlatformTextRenderer(context: PlatformContext, density: Density): PlatformTextRenderer {
    return SkikoTextRenderer(density)
}
