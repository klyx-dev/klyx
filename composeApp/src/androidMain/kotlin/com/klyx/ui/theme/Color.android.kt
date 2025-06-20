package com.klyx.ui.theme

import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

actual fun Color.blend(
    color: Color,
    @FloatRange(from = 0.0, to = 1.0) fraction: Float
): Color = Color(ColorUtils.blendARGB(this.toArgb(), color.toArgb(), fraction))
