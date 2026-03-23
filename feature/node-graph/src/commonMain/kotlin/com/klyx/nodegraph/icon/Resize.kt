package com.klyx.nodegraph.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Icons.Resize: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Resize",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color(0xFFE3E3E3))) {
            moveTo(784f, 840f)
            lineTo(120f, 177f)
            lineToRelative(57f, -57f)
            lineToRelative(663f, 663f)
            lineToRelative(-56f, 57f)
            close()
            moveTo(383f, 840f)
            lineTo(120f, 577f)
            lineToRelative(57f, -57f)
            lineToRelative(263f, 263f)
            lineToRelative(-57f, 57f)
            close()
        }
    }.build()
}
