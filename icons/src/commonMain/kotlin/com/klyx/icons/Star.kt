package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Star: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Star",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveToRelative(354f, 673f)
            lineToRelative(126f, -76f)
            lineToRelative(126f, 77f)
            lineToRelative(-33f, -144f)
            lineToRelative(111f, -96f)
            lineToRelative(-146f, -13f)
            lineToRelative(-58f, -136f)
            lineToRelative(-58f, 135f)
            lineToRelative(-146f, 13f)
            lineToRelative(111f, 97f)
            lineToRelative(-33f, 143f)
            close()
            moveTo(233f, 840f)
            lineToRelative(65f, -281f)
            lineTo(80f, 370f)
            lineToRelative(288f, -25f)
            lineToRelative(112f, -265f)
            lineToRelative(112f, 265f)
            lineToRelative(288f, 25f)
            lineToRelative(-218f, 189f)
            lineToRelative(65f, 281f)
            lineToRelative(-247f, -149f)
            lineToRelative(-247f, 149f)
            close()
            moveTo(480f, 490f)
            close()
        }
    }.build()
}
