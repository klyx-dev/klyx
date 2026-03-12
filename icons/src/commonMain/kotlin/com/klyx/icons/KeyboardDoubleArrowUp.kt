package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.KeyboardDoubleArrowUp: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "KeyboardDoubleArrowUp",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveToRelative(296f, 736f)
            lineToRelative(-56f, -56f)
            lineToRelative(240f, -240f)
            lineToRelative(240f, 240f)
            lineToRelative(-56f, 56f)
            lineToRelative(-184f, -183f)
            lineToRelative(-184f, 183f)
            close()
            moveTo(296f, 496f)
            lineTo(240f, 440f)
            lineTo(480f, 200f)
            lineTo(720f, 440f)
            lineTo(664f, 496f)
            lineTo(480f, 313f)
            lineTo(296f, 496f)
            close()
        }
    }.build()
}
