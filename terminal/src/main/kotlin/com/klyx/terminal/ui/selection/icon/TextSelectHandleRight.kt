package com.klyx.terminal.ui.selection.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val TextSelectHandleRight: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "TextSelectHandleRight",
        defaultWidth = 48.dp,
        defaultHeight = 24.dp,
        viewportWidth = 132f,
        viewportHeight = 66f
    ).apply {
        path(fill = SolidColor(Color(0xFF2196F3))) {
            moveTo(33f, 20.8f)
            curveToRelative(0f, 23.9f, 0.7f, 27.3f, 7.2f, 34.7f)
            curveToRelative(6.2f, 7.1f, 12.4f, 9.8f, 23.5f, 10.3f)
            curveToRelative(12.6f, 0.6f, 17.8f, -1.3f, 25.8f, -9.3f)
            curveToRelative(8f, -8f, 9.9f, -13.2f, 9.3f, -25.8f)
            curveToRelative(-0.5f, -11.1f, -3.2f, -17.3f, -10.3f, -23.5f)
            curveToRelative(-7.4f, -6.5f, -10.8f, -7.2f, -34.7f, -7.2f)
            lineToRelative(-20.8f, -0f)
            lineToRelative(0f, 20.8f)
            close()
        }
    }.build()
}
