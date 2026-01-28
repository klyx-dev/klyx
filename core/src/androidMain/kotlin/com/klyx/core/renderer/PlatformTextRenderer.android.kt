package com.klyx.core.renderer

import android.graphics.Paint
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.core.graphics.withSave
import com.klyx.core.PlatformContext

internal class AndroidTextRenderer(
    context: PlatformContext,
    private val density: Density
) : PlatformTextRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val resolver = createFontFamilyResolver(context)

    override fun getFontSpacing(fontSize: TextUnit, fontFamily: FontFamily): Float {
        val typeface by resolver.resolveAsTypeface(fontFamily)
        paint.typeface = typeface
        paint.textSize = with(density) { fontSize.toPx() }
        return paint.fontSpacing
    }

    override fun getFontMetrics(
        fontSize: TextUnit,
        fontFamily: FontFamily
    ): FontMetrics {
        val typeface by resolver.resolveAsTypeface(fontFamily)
        paint.typeface = typeface
        paint.textSize = with(density) { fontSize.toPx() }

        val fm = paint.fontMetrics
        return FontMetrics(fm.top, fm.ascent, fm.descent, fm.bottom, fm.leading)
    }

    override fun measureText(text: CharSequence, start: Int, end: Int, style: TextStyle): Float {
        val typeface by resolver.resolveAsTypeface(
            fontFamily = style.fontFamily,
            fontWeight = style.fontWeight ?: FontWeight.Normal,
            fontStyle = style.fontStyle ?: FontStyle.Normal,
            fontSynthesis = style.fontSynthesis ?: FontSynthesis.All
        )

        paint.apply {
            this.typeface = typeface
            textSize = with(density) { style.fontSize.toPx() }
            color = style.color.toArgb()

            if (style.letterSpacing.isSpecified) {
                letterSpacing = style.letterSpacing.value
            }
        }

        return paint.measureText(text, start, end)
    }

    override fun measureText(text: CharArray, index: Int, count: Int, style: TextStyle): Float {
        val typeface by resolver.resolveAsTypeface(
            fontFamily = style.fontFamily,
            fontWeight = style.fontWeight ?: FontWeight.Normal,
            fontStyle = style.fontStyle ?: FontStyle.Normal,
            fontSynthesis = style.fontSynthesis ?: FontSynthesis.All
        )

        paint.apply {
            this.typeface = typeface
            textSize = with(density) { style.fontSize.toPx() }
            color = style.color.toArgb()

            if (style.letterSpacing.isSpecified) {
                letterSpacing = style.letterSpacing.value
            }
        }

        return paint.measureText(text, index, count)
    }

    override fun drawText(
        canvas: NativeCanvas,
        text: String,
        style: TextStyle,
        topLeft: Offset,
        paint: androidx.compose.ui.graphics.Paint
    ) {
        val (x, y) = topLeft

        val typeface by resolver.resolveAsTypeface(
            fontFamily = style.fontFamily,
            fontWeight = style.fontWeight ?: FontWeight.Normal,
            fontStyle = style.fontStyle ?: FontStyle.Normal,
            fontSynthesis = style.fontSynthesis ?: FontSynthesis.All
        )

        val paint = paint.asFrameworkPaint().apply {
            this.typeface = typeface
            textSize = with(density) { style.fontSize.toPx() }
            color = style.color.toArgb()

            if (style.letterSpacing.isSpecified) {
                this.letterSpacing = style.letterSpacing.value
            }

            when (style.textAlign) {
                TextAlign.Left -> this.textAlign = Paint.Align.LEFT
                TextAlign.Right -> this.textAlign = Paint.Align.RIGHT
                TextAlign.Center -> this.textAlign = Paint.Align.CENTER
            }

            this.textSkewX = if (style.fontStyle == FontStyle.Italic) -0.35f else 0.0f
            this.isStrikeThruText = style.textDecoration == TextDecoration.LineThrough
            this.isUnderlineText = style.textDecoration == TextDecoration.Underline
            this.isFakeBoldText = style.fontWeight == FontWeight.Bold
        }

        canvas.withSave { drawText(text, x, y, paint) }
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
        paint: androidx.compose.ui.graphics.Paint
    ) {
        val (x, y) = topLeft

        val typeface by resolver.resolveAsTypeface(
            fontFamily = style.fontFamily,
            fontWeight = style.fontWeight ?: FontWeight.Normal,
            fontStyle = style.fontStyle ?: FontStyle.Normal,
            fontSynthesis = style.fontSynthesis ?: FontSynthesis.All
        )

        val paint = paint.asFrameworkPaint().apply {
            this.typeface = typeface
            textSize = with(density) { style.fontSize.toPx() }
            color = style.color.toArgb()

            if (style.letterSpacing.isSpecified) {
                this.letterSpacing = style.letterSpacing.value
            }

            when (style.textAlign) {
                TextAlign.Left -> this.textAlign = Paint.Align.LEFT
                TextAlign.Right -> this.textAlign = Paint.Align.RIGHT
                TextAlign.Center -> this.textAlign = Paint.Align.CENTER
            }

            this.textSkewX = if (style.fontStyle == FontStyle.Italic) -0.35f else 0.0f
            this.isStrikeThruText = style.textDecoration == TextDecoration.LineThrough
            this.isUnderlineText = style.textDecoration == TextDecoration.Underline
            this.isFakeBoldText = style.fontWeight == FontWeight.Bold
        }

        canvas.drawTextRun(text, index, count, contextIndex, contextCount, x, y, isRtl, paint)
    }
}

actual fun PlatformTextRenderer(context: PlatformContext, density: Density): PlatformTextRenderer {
    return AndroidTextRenderer(context, density)
}
