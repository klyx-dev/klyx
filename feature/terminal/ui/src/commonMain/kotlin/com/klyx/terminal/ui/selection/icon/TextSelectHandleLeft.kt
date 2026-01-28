package com.klyx.terminal.ui.selection.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val TextSelectHandleLeft: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "TextSelectHandleLeft",
        defaultWidth = 48.dp,
        defaultHeight = 24.dp,
        viewportWidth = 132f,
        viewportHeight = 66f
    ).apply {
        path(fill = SolidColor(Color(0xFF2196F3))) {
            moveTo(52.3f, 1.6f)
            curveToRelative(-5.7f, 2.1f, -12.9f, 8.6f, -16f, 14.8f)
            curveToRelative(-2.2f, 4.1f, -2.8f, 6.9f, -3.1f, 14.3f)
            curveToRelative(-0.6f, 12.6f, 1.3f, 17.8f, 9.3f, 25.8f)
            curveToRelative(8f, 8f, 13.2f, 9.9f, 25.8f, 9.3f)
            curveToRelative(11.1f, -0.5f, 17.3f, -3.2f, 23.5f, -10.3f)
            curveToRelative(6.5f, -7.4f, 7.2f, -10.8f, 7.2f, -34.7f)
            lineToRelative(0f, -20.8f)
            lineToRelative(-21.2f, 0.1f)
            curveToRelative(-16.1f, -0f, -22.3f, 0.4f, -25.5f, 1.5f)
            close()
        }
    }.build()
}
