package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Close: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Close",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveToRelative(256f, 760f)
            lineToRelative(-56f, -56f)
            lineToRelative(224f, -224f)
            lineToRelative(-224f, -224f)
            lineToRelative(56f, -56f)
            lineToRelative(224f, 224f)
            lineToRelative(224f, -224f)
            lineToRelative(56f, 56f)
            lineToRelative(-224f, 224f)
            lineToRelative(224f, 224f)
            lineToRelative(-56f, 56f)
            lineToRelative(-224f, -224f)
            lineToRelative(-224f, 224f)
            close()
        }
    }.build()
}
