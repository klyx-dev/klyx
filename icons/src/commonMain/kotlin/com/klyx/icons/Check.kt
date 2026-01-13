package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Check: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Check",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(382f, 720f)
            lineTo(154f, 492f)
            lineToRelative(57f, -57f)
            lineToRelative(171f, 171f)
            lineToRelative(367f, -367f)
            lineToRelative(57f, 57f)
            lineToRelative(-424f, 424f)
            close()
        }
    }.build()
}
