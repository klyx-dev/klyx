package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Backspace: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Backspace",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveToRelative(456f, 640f)
            lineToRelative(104f, -104f)
            lineToRelative(104f, 104f)
            lineToRelative(56f, -56f)
            lineToRelative(-104f, -104f)
            lineToRelative(104f, -104f)
            lineToRelative(-56f, -56f)
            lineToRelative(-104f, 104f)
            lineToRelative(-104f, -104f)
            lineToRelative(-56f, 56f)
            lineToRelative(104f, 104f)
            lineToRelative(-104f, 104f)
            lineToRelative(56f, 56f)
            close()
            moveTo(360f, 800f)
            quadToRelative(-19f, 0f, -36f, -8.5f)
            reflectiveQuadTo(296f, 768f)
            lineTo(80f, 480f)
            lineToRelative(216f, -288f)
            quadToRelative(11f, -15f, 28f, -23.5f)
            reflectiveQuadToRelative(36f, -8.5f)
            horizontalLineToRelative(440f)
            quadToRelative(33f, 0f, 56.5f, 23.5f)
            reflectiveQuadTo(880f, 240f)
            verticalLineToRelative(480f)
            quadToRelative(0f, 33f, -23.5f, 56.5f)
            reflectiveQuadTo(800f, 800f)
            lineTo(360f, 800f)
            close()
            moveTo(180f, 480f)
            lineToRelative(180f, 240f)
            horizontalLineToRelative(440f)
            verticalLineToRelative(-480f)
            lineTo(360f, 240f)
            lineTo(180f, 480f)
            close()
            moveTo(580f, 480f)
            close()
        }
    }.build()
}
