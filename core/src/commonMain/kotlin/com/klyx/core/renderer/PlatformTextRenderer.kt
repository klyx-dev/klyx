package com.klyx.core.renderer

import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import com.klyx.core.LocalPlatformContext
import com.klyx.core.PlatformContext

interface PlatformTextRenderer {
    fun getFontSpacing(fontSize: TextUnit, fontFamily: FontFamily): Float

    fun getFontMetrics(fontSize: TextUnit, fontFamily: FontFamily): FontMetrics

    /**
     * Returns the width of the text.
     */
    fun measureText(
        text: CharSequence,
        start: Int = 0,
        end: Int = text.length,
        style: TextStyle = TextStyle.Default
    ): Float

    fun measureText(
        text: CharArray,
        index: Int,
        count: Int,
        style: TextStyle = TextStyle.Default
    ): Float

    @MainThread
    fun drawText(
        canvas: NativeCanvas,
        text: String,
        style: TextStyle = TextStyle.Default,
        topLeft: Offset = Offset.Zero,
        paint: Paint = Paint()
    )

    @MainThread
    fun drawTextRun(
        canvas: NativeCanvas,
        text: CharArray,
        index: Int,
        count: Int,
        contextIndex: Int,
        contextCount: Int,
        topLeft: Offset,
        isRtl: Boolean,
        style: TextStyle = TextStyle.Default,
        paint: Paint = Paint()
    )
}

expect fun PlatformTextRenderer(context: PlatformContext, density: Density): PlatformTextRenderer

@Composable
fun rememberPlatformTextRenderer(): PlatformTextRenderer {
    val context = LocalPlatformContext.current
    val density = LocalDensity.current
    return remember(context, density) { PlatformTextRenderer(context, density) }
}
